/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websocket.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websocket.plugin.PluginConstants.DISPATCHER_ANNOTATION;
import static io.ballerina.stdlib.websocket.plugin.PluginConstants.DISPATCHER_STREAM_ID_ANNOTATION;
import static io.ballerina.stdlib.websocket.plugin.PluginConstants.ORG_NAME;
import static io.ballerina.stdlib.websocket.plugin.Utils.reportDiagnostics;

/**
 * Validates a Ballerina WebSocket Upgrade Service.
 */
public class WebSocketUpgradeServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        List<Diagnostic> diagnostics = ctx.semanticModel().diagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                return;
            }
        }
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        boolean disptacherKeyAnnotation = getDispatcherConfigAnnotation(serviceDeclarationNode,
                ctx.semanticModel(), DISPATCHER_ANNOTATION);
        boolean disptacherStreamIdAnnotation = getDispatcherConfigAnnotation(serviceDeclarationNode,
                ctx.semanticModel(), DISPATCHER_STREAM_ID_ANNOTATION);
        if (disptacherStreamIdAnnotation && !disptacherKeyAnnotation) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.DISPATCHER_STREAM_ID_WITHOUT_KEY,
                    serviceDeclarationNode.location());
        }

        String modulePrefix = Utils.getPrefix(ctx);
        Optional<Symbol> serviceDeclarationSymbol = ctx.semanticModel().symbol(serviceDeclarationNode);
        if (serviceDeclarationSymbol.isPresent()) {
            List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceDeclarationSymbol.get())
                    .listenerTypes();
            for (TypeSymbol listenerType : listenerTypes) {
                if (isListenerBelongsToWebSocketModule(listenerType)) {
                    ListenerInitExpressionNodeVisitor visitor = new ListenerInitExpressionNodeVisitor(ctx);
                    serviceDeclarationNode.syntaxTree().rootNode().accept(visitor);
                    validateListenerArguments(ctx, visitor);
                    validateService(ctx, modulePrefix, disptacherKeyAnnotation);
                    return;
                }
            }
        }
    }

    private boolean isAnnotationPresent(AnnotationNode annotation, SemanticModel semanticModel,
                                              String annotationName) {
        Optional<Symbol> symbolOpt = semanticModel.symbol(annotation);
        if (symbolOpt.isEmpty()) {
            return false;
        }

        Symbol symbol = symbolOpt.get();
        if (!(symbol instanceof AnnotationSymbol)) {
            return false;
        }

        return annotation.annotValue().toString().contains(annotationName);
    }

    private boolean getDispatcherConfigAnnotation(ServiceDeclarationNode serviceNode,
                                                  SemanticModel semanticModel, String annotationName) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) {
            return false;
        }
        MetadataNode metaData = metadata.get();
        NodeList<AnnotationNode> annotations = metaData.annotations();
        return annotations.stream()
                .anyMatch(ann -> isAnnotationPresent(ann, semanticModel, annotationName));
    }

    private boolean isListenerBelongsToWebSocketModule(TypeSymbol listenerType) {
        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isWebSocketModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isWebSocketModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }

        return false;
    }

    private void validateListenerArguments(SyntaxNodeAnalysisContext context,
            ListenerInitExpressionNodeVisitor visitor) {
        visitor.getExplicitNewExpressionNodes()
                .forEach(explicitNewExpressionNode -> {
                    SeparatedNodeList<FunctionArgumentNode> functionArgs = explicitNewExpressionNode
                            .parenthesizedArgList().arguments();
                    verifyListenerArgType(context, explicitNewExpressionNode.location(), functionArgs);
                });

        visitor.getImplicitNewExpressionNodes()
                .forEach(implicitNewExpressionNode -> {
                    Optional<ParenthesizedArgList> argListOpt = implicitNewExpressionNode.parenthesizedArgList();
                    if (argListOpt.isPresent()) {
                        SeparatedNodeList<FunctionArgumentNode> functionArgs = argListOpt.get().arguments();
                        verifyListenerArgType(context, implicitNewExpressionNode.location(), functionArgs);
                    }
                });
    }

    private void verifyListenerArgType(SyntaxNodeAnalysisContext context, NodeLocation location,
            SeparatedNodeList<FunctionArgumentNode> functionArgs) {
        if (functionArgs.size() >= 2) {
            PositionalArgumentNode firstArg = (PositionalArgumentNode) functionArgs.get(0);
            SyntaxKind firstArgSyntaxKind = firstArg.expression().kind();
            SyntaxKind secondArgSyntaxKind = null;
            if (functionArgs.get(1) instanceof PositionalArgumentNode) {
                secondArgSyntaxKind = ((PositionalArgumentNode) functionArgs.get(1)).expression().kind();
            } else if (functionArgs.get(1) instanceof NamedArgumentNode) {
                secondArgSyntaxKind = ((NamedArgumentNode) functionArgs.get(1)).expression().kind();
            }
            if (secondArgSyntaxKind != null && !(firstArgSyntaxKind == SyntaxKind.NUMERIC_LITERAL && (
                    secondArgSyntaxKind == SyntaxKind.SIMPLE_NAME_REFERENCE
                            || secondArgSyntaxKind == SyntaxKind.MAPPING_CONSTRUCTOR))) {
                reportDiagnostics(context, PluginConstants.CompilationErrors.INVALID_LISTENER_INIT_PARAMS,
                        location, WebSocketConstants.PACKAGE_WEBSOCKET);
            }
        }
    }

    private boolean isWebSocketModule(ModuleSymbol moduleSymbol) {
        return Utils.equals(moduleSymbol.getName().get(), WebSocketConstants.PACKAGE_WEBSOCKET)
                && Utils.equals(moduleSymbol.id().orgName(), ORG_NAME);
    }

    private void validateService(SyntaxNodeAnalysisContext ctx, String modulePrefix, boolean disptacherAnnotation) {
        WebSocketUpgradeServiceValidator wsServiceValidator;
        wsServiceValidator = new WebSocketUpgradeServiceValidator(ctx,
                modulePrefix + SyntaxKind.COLON_TOKEN.stringValue());
        wsServiceValidator.validate(disptacherAnnotation);
    }
}

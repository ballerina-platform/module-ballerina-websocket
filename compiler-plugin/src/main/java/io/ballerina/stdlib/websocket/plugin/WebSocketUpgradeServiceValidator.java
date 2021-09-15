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

import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websocket.WebSocketConstants;

import java.util.List;

import static io.ballerina.stdlib.websocket.plugin.PluginConstants.REMOTE_KEY_WORD;

/**
 * Class to validate WebSocket services.
 */
public class WebSocketUpgradeServiceValidator {
    private SyntaxNodeAnalysisContext ctx;
    private FunctionDefinitionNode resourceNode;
    private String modulePrefix;

    WebSocketUpgradeServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext, String modulePrefix) {
        this.ctx = syntaxNodeAnalysisContext;
        this.modulePrefix = modulePrefix;
    }

    void validate() {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        if (serviceDeclarationNode.members().size() > 1) {
            int numResources = (int) serviceDeclarationNode.members().stream()
                    .filter(child -> child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).count();
            if (numResources > 1) {
                Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RESOURCE_ERROR,
                        serviceDeclarationNode.location());
            }
        }
        serviceDeclarationNode.members().stream().filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).forEach(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String functionName = functionDefinitionNode.functionName().toString().split(" ")[0];
            if (functionName.compareTo(WebSocketConstants.GET) == 0
                    && functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                resourceNode = functionDefinitionNode;
            } else if (functionDefinitionNode.qualifierList().get(0).text().equals(REMOTE_KEY_WORD)
                    || functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                reportInvalidFunction(functionDefinitionNode);
            }
        });
        if (resourceNode != null) {
            validateResourceReturnTypes(resourceNode);
            ReturnStatementNodeVisitor returnStatementNodeVisitor = new ReturnStatementNodeVisitor();
            resourceNode.accept(returnStatementNodeVisitor);
            for (ReturnStatementNode returnStatementNode : returnStatementNodeVisitor.getReturnStatementNodes()) {
                ExpressionNode expressionNode = returnStatementNode.expression().get();
                if (expressionNode instanceof NewExpressionNode) {
                    final TypeReferenceTypeSymbol definition = (TypeReferenceTypeSymbol) ctx.semanticModel()
                            .type(expressionNode).get();
                    ClassSymbol classSymbol = (ClassSymbol) definition.typeDescriptor();
                    final MethodSymbol[] methodSymbols = classSymbol.methods().values().toArray(new MethodSymbol[0]);
                    List<TypeSymbol> typeInclusions = classSymbol.typeInclusions();
                    if (typeInclusions.isEmpty()) {
                        for (MethodSymbol symbol : methodSymbols) {
                            if (symbol.qualifiers().contains(Qualifier.REMOTE)) {
                                String functionName = symbol.getName().get();
                                switch (functionName) {
                                case PluginConstants.ON_OPEN:
                                    Utils.validateOnOpenFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                case PluginConstants.ON_CLOSE:
                                    Utils.validateOnCloseFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                case PluginConstants.ON_IDLE_TIMEOUT:
                                    Utils.validateOnIdleTimeoutFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                case PluginConstants.ON_ERROR:
                                    Utils.validateOnErrorFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                case PluginConstants.ON_TEXT_MESSAGE:
                                    Utils.validateOnTextMessageFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                case PluginConstants.ON_BINARY_MESSAGE:
                                    Utils.validateOnBinaryMessageFunction(symbol.typeDescriptor(), ctx, resourceNode);
                                    break;
                                default:
                                    reportInvalidFunction(resourceNode);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void validateResourceReturnTypes(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            FunctionTypeSymbol functionTypeSymbol = ((MethodSymbol) ctx.semanticModel().symbol(resourceNode).get())
                    .typeDescriptor();
            TypeSymbol returnTypeSymbol = functionTypeSymbol.returnTypeDescriptor().get();
            if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
                for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                    if (!((symbol.typeKind() == TypeDescKind.TYPE_REFERENCE && symbol.getName().get()
                            .equals(PluginConstants.SERVICE) && symbol.getModule().map(ModuleSymbol::id).get().orgName()
                            .equals(PluginConstants.ORG_NAME) && WebSocketConstants.PACKAGE_WEBSOCKET
                            .equals(symbol.getModule().map(ModuleSymbol::id).get().modulePrefix())) || (
                            symbol.typeKind() == TypeDescKind.TYPE_REFERENCE &&
                                    ((TypeReferenceTypeSymbol) symbol).typeDescriptor().typeKind()
                                            == TypeDescKind.ERROR))) {
                        Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_IN_RESOURCE,
                                resourceNode.location(), symbol.typeKind().getName(), resourceNode.functionName(),
                                modulePrefix + PluginConstants.SERVICE + PluginConstants.PIPE + modulePrefix
                                        + PluginConstants.UPGRADE_ERROR);
                    }
                }
            } else if (returnTypeSymbol.typeKind() == TypeDescKind.NIL) {
                Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_IN_RESOURCE,
                        resourceNode.location(), TypeDescKind.NIL.getName(), resourceNode.functionName(),
                        modulePrefix + PluginConstants.SERVICE + PluginConstants.PIPE + modulePrefix
                                + PluginConstants.UPGRADE_ERROR);
            }
        }
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString());
    }
}

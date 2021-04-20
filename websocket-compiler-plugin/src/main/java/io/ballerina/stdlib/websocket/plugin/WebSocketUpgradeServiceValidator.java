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
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websocket.plugin.PluginConstants.PIPE;
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
                DiagnosticInfo diagnosticInfo = Utils
                        .getDiagnosticInfo(PluginConstants.CompilationErrors.INVALID_RESOURCE_ERROR);
                ctx.reportDiagnostic(
                        DiagnosticFactory.createDiagnostic(diagnosticInfo, serviceDeclarationNode.location()));
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
            validateResourceParams(resourceNode);
            validateResourceReturnTypes(resourceNode);
            ReturnStatementNodeVisitor returnStatementNodeVisitor = new ReturnStatementNodeVisitor();
            resourceNode.accept(returnStatementNodeVisitor);
            for (ReturnStatementNode returnStatementNode : returnStatementNodeVisitor.getReturnStatementNodes()) {
                ExpressionNode expressionNode = returnStatementNode.expression().get();
                if (expressionNode instanceof NewExpressionNode) {
                    final TypeReferenceTypeSymbol definition = (TypeReferenceTypeSymbol) ctx.semanticModel()
                            .symbol(expressionNode).get();
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

    private void validateResourceParams(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() > 1) {
                DiagnosticInfo diagnosticInfo = Utils
                        .getDiagnosticInfo(PluginConstants.CompilationErrors.MORE_THAN_ONE_RESOURCE_PARAM_ERROR);
                ctx.reportDiagnostic(DiagnosticFactory
                        .createDiagnostic(diagnosticInfo, resourceNode.location()));
            } else if (!parameterNodes.isEmpty()) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(PluginConstants.HTTP_REQUEST)) {
                    DiagnosticInfo diagnosticInfo = Utils
                            .getDiagnosticInfo(PluginConstants.CompilationErrors.INVALID_RESOURCE_PARAMETER_ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString(),
                                    PluginConstants.HTTP_REQUEST));
                }
            }
        }
    }

    private void validateResourceReturnTypes(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode
                    .functionSignature().returnTypeDesc();
            if (resourceNode.functionSignature().returnTypeDesc().isEmpty()) {
                DiagnosticInfo diagnosticInfo = Utils
                        .getDiagnosticInfo(PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_IN_RESOURCE);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location()));
            }
            Node returnTypeDescriptor = returnTypesNode.get().type();
            String returnTypeDescWithoutTrailingSpace = returnTypeDescriptor.toString().split(" ")[0];
            if (!(returnTypeDescWithoutTrailingSpace.equals(
                    modulePrefix + PluginConstants.SERVICE + PIPE + modulePrefix + PluginConstants.UPGRADE_ERROR))
                    && !(returnTypeDescWithoutTrailingSpace.equals(
                    modulePrefix + PluginConstants.UPGRADE_ERROR + PIPE + modulePrefix + PluginConstants.SERVICE))) {
                DiagnosticInfo diagnosticInfo = Utils
                        .getDiagnosticInfo(PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_IN_RESOURCE);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, returnTypeDescriptor.location(),
                        returnTypeDescriptor.toString(), resourceNode.functionName(),
                        modulePrefix + PluginConstants.SERVICE + PluginConstants.PIPE + modulePrefix
                                + PluginConstants.UPGRADE_ERROR));
            }

        }
    }

    void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = Utils
                .getDiagnosticInfo(PluginConstants.CompilationErrors.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, functionDefinitionNode.location(),
                functionDefinitionNode.functionName().toString()));
    }
}

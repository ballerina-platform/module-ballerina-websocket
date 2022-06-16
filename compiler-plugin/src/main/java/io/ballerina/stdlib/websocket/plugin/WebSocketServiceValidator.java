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

import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class for validating websocket service.
 */
public class WebSocketServiceValidator {
    public static final String GENERIC_FUNCTION = "generic function";
    private SyntaxNodeAnalysisContext ctx;

    WebSocketServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        this.ctx = syntaxNodeAnalysisContext;
    }

    public void validate() {
        ClassDefinitionNode classDefNode = (ClassDefinitionNode) ctx.node();
        Map<String, Boolean> functionSet = classDefNode.members().stream().filter(child ->
                child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).map(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            NodeList<Token> qualifierList = functionDefinitionNode.qualifierList();
            if (qualifierList.isEmpty()) {
                return GENERIC_FUNCTION;
            }
            String qualifier = qualifierList.get(0).text();
            if (qualifier.equals(Qualifier.REMOTE.getValue())) {
                filterRemoteFunctions(functionDefinitionNode);
                return functionDefinitionNode.functionName().toString();
            } else if (qualifier.equals(Qualifier.RESOURCE.getValue())) {
                reportInvalidFunction(functionDefinitionNode);
            }
            return GENERIC_FUNCTION;
        }).collect(Collectors.toMap(node -> node, node -> true, (node1, node2) -> node1));

        if (functionSet.containsKey(PluginConstants.ON_MESSAGE) &&
                functionSet.containsKey(PluginConstants.ON_TEXT_MESSAGE)) {
            Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTIONS,
                    classDefNode.location(), PluginConstants.ON_TEXT_MESSAGE);
        }
        if (functionSet.containsKey(PluginConstants.ON_MESSAGE) &&
                        functionSet.containsKey(PluginConstants.ON_BINARY_MESSAGE)) {
            Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTIONS,
                    classDefNode.location(), PluginConstants.ON_BINARY_MESSAGE);
        }
        if (!functionSet.containsKey(PluginConstants.ON_TEXT_MESSAGE) &&
                !functionSet.containsKey(PluginConstants.ON_MESSAGE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_TEXT_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_BINARY_MESSAGE) &&
                !functionSet.containsKey(PluginConstants.ON_MESSAGE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_BINARY_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_CLOSE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_CLOSE_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_OPEN)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_OPEN_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_PING_MESSAGE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_PING_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_PONG_MESSAGE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_PONG_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_IDLE_TIMEOUT)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_IDLE_TIMEOUT_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_ERROR)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_ERROR_GENERATION_HINT);
        }
        if (!functionSet.containsKey(PluginConstants.ON_MESSAGE) &&
                !functionSet.containsKey(PluginConstants.ON_TEXT_MESSAGE) &&
                !functionSet.containsKey(PluginConstants.ON_BINARY_MESSAGE)) {
            reportDiagnostic(classDefNode, PluginConstants.CompilationErrors.ON_MESSAGE_GENERATION_HINT);
        }
    }

    private void filterRemoteFunctions(FunctionDefinitionNode functionDefinitionNode) {
        FunctionTypeSymbol functionTypeSymbol = ((MethodSymbol) ctx.semanticModel().symbol(functionDefinitionNode)
                .get()).typeDescriptor();
        switch (functionDefinitionNode.functionName().toString()) {
            case PluginConstants.ON_OPEN:
                Utils.validateOnOpenFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_CLOSE:
                Utils.validateOnCloseFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_ERROR:
                Utils.validateOnErrorFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_IDLE_TIMEOUT:
                Utils.validateOnIdleTimeoutFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_TEXT_MESSAGE:
                Utils.validateOnTextMessageFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_MESSAGE:
                Utils.validateOnDataFunctions(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case PluginConstants.ON_PING_MESSAGE:
            case PluginConstants.ON_PONG_MESSAGE:
            case PluginConstants.ON_BINARY_MESSAGE:
                Utils.validateOnBinaryMessageFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            default:
                reportInvalidFunction(functionDefinitionNode);
        }
    }

    private void reportDiagnostic(ClassDefinitionNode classDefNode,
                                  PluginConstants.CompilationErrors hint) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                hint.getErrorCode(),
                hint.getError(),
                DiagnosticSeverity.INTERNAL);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, classDefNode.location()));
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        Utils.reportDiagnostics(ctx, PluginConstants.CompilationErrors.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString());
    }
}

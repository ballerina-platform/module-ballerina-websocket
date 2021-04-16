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
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;

/**
 * A class for validating websocket service.
 */
public class WebSocketServiceValidator {
    private SyntaxNodeAnalysisContext ctx;

    WebSocketServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        this.ctx = syntaxNodeAnalysisContext;
    }

    public void validate() {
        ClassDefinitionNode classDefNode = (ClassDefinitionNode) ctx.node();
        classDefNode.members().stream().filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).forEach(methodNode -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) methodNode;
            if (functionDefinitionNode.qualifierList().get(0).text().equals(PluginConstants.REMOTE_KEY_WORD)) {
                filterRemoteFunctions(functionDefinitionNode);
            } else if (functionDefinitionNode.qualifierList().get(0).text().equals(PluginConstants.RESOURCE_KEY_WORD)) {
                reportInvalidFunction(functionDefinitionNode);
            }
        });
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
            case PluginConstants.ON_BINARY_MESSAGE:
                Utils.validateOnBinaryMessageFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            default:
                reportInvalidFunction(functionDefinitionNode);
        }
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = Utils
                .getDiagnosticInfo(PluginConstants.CompilationErrors.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }
}

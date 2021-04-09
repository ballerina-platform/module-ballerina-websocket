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
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import static io.ballerina.stdlib.websocket.plugin.WebSocketUpgradeServiceValidator.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE;

/**
 * A class for validating websocket service.
 */
public class WebSocketServiceValidator {
    private SyntaxNodeAnalysisContext ctx;
    public static final String CALLER = "Caller";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String STRING = "string";
    public static final String INT = "int";
    public static final String ERROR = "Error";
    public static final String GENERIC_ERROR = "error";
    public static final String OPTIONAL = "?";
    private static final String CODE = "WS_110";
    static final String ON_ERROR = "onError";
    static final String ON_OPEN = "onOpen";
    static final String ON_CLOSE = "onClose";
    static final String ON_IDLE_TIMEOUT = "onIdleTimeout";
    static final String ON_TEXT_MESSAGE = "onTextMessage";
    static final String ON_BINARY_MESSAGE = "onBinaryMessage";
    private static final String REMOTE_KEY_WORD = "remote";
    private static final String RESOURCE_KEY_WORD = "resource";
    public static final String INVALID_INPUT_PARAM_FOR_ON_CLOSE =
            "Invalid parameters `{0}` provided for onClose remote function";
    public static final String INVALID_INPUT_PARAM_FOR_ON_CLOSE_CODE = "WS_202";
    public static final String INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onClose remote function. `string` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS_CODE = "WS_203";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_OPEN =
            "Invalid parameters provided for onOpen remote function. Only `{0}`:Caller is allowed as the parameter";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_OPEN_CODE = "WS_204";
    public static final String INVALID_RETURN_TYPES =
            "Invalid return types provided for `{0}` remote function, return type should be either `error?` or `{1}` ";
    public static final String INVALID_RETURN_TYPES_CODE = "WS_205";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_CLOSE =
            "Invalid parameters provided for onClose remote function";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_CLOSE_CODE = "WS_206";
    public static final String INVALID_INPUT_FOR_ON_ERROR_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onError remote function. `error` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ON_ERROR_WITH_ONE_PARAMS_CODE = "WS_207";
    public static final String INVALID_INPUT_FOR_ON_ERROR = "Invalid parameters `{0}` provided for onError "
            + "remote function";
    public static final String INVALID_INPUT_FOR_ON_ERROR_CODE = "WS_208";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_IDLE_TIMEOUT = "Invalid parameters provided for "
            + "OnIdleTimeout remote function. Only `{0}`:Caller is allowed as the parameter";
    public static final String INVALID_INPUT_PARAMS_FOR_ON_IDLE_TIMEOUT_CODE = "WS_209";
    public static final String INVALID_INPUT_PARAM_FOR_ON_IDLE_TIMEOUT =
            "Invalid parameters `{0}` provided for onIdleTimeout remote function";
    public static final String INVALID_INPUT_PARAM_FOR_ON_IDLE_TIMEOUT_CODE = "WS_210";
    public static final String INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onTextMessage remote function. `string` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS_CODE = "WS_211";
    public static final String INVALID_INPUT_FOR_ON_TEXT =
            "Invalid parameters `{0}` provided for onTextMessage remote function";
    public static final String INVALID_INPUT_FOR_ON_TEXT_CODE = "WS_212";
    public static final String INVALID_RETURN_TYPES_ON_DATA =
            "Invalid return type `{0}` provided for `{1}` remote function";
    public static final String INVALID_RETURN_TYPES_ON_DATA_CODE = "WS_213";
    public static final String INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onBinaryMessage remote function. "
                    + "`byte[]` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS_CODE = "WS_214";
    public static final String INVALID_INPUT_FOR_ON_BINARY =
            "Invalid parameters `{0}` provided for onBinaryMessage remote function";
    public static final String INVALID_INPUT_FOR_ON_BINARY_CODE = "WS_215";

    WebSocketServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        this.ctx = syntaxNodeAnalysisContext;
    }

    public void validate() {
        ClassDefinitionNode classDefNode = (ClassDefinitionNode) ctx.node();
        classDefNode.members().stream().filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).forEach(methodNode -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) methodNode;
            if (functionDefinitionNode.qualifierList().get(0).text().equals(REMOTE_KEY_WORD)) {
                filterRemoteFunctions(functionDefinitionNode);
            } else if (functionDefinitionNode.qualifierList().get(0).text().equals(RESOURCE_KEY_WORD)) {
                reportInvalidFunction(functionDefinitionNode);
            }
        });
    }

    private void filterRemoteFunctions(FunctionDefinitionNode functionDefinitionNode) {
        FunctionTypeSymbol functionTypeSymbol = ((MethodSymbol) ctx.semanticModel().symbol(functionDefinitionNode)
                .get()).typeDescriptor();
        switch (functionDefinitionNode.functionName().toString()) {
            case ON_OPEN:
                Utils.validateOnOpenFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case ON_CLOSE:
                Utils.validateOnCloseFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case ON_ERROR:
                Utils.validateOnErrorFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case ON_IDLE_TIMEOUT:
                Utils.validateOnIdleTimeoutFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case ON_TEXT_MESSAGE:
                Utils.validateOnTextMessageFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            case ON_BINARY_MESSAGE:
                Utils.validateOnBinaryMessageFunction(functionTypeSymbol, ctx, functionDefinitionNode);
                break;
            default:
                reportInvalidFunction(functionDefinitionNode);
        }
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE,
                DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }
}

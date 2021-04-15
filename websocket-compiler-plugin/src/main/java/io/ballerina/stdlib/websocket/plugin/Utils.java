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
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.List;

import static io.ballerina.stdlib.websocket.plugin.WebSocketUpgradeServiceValidator.COLON;

/**
 * Class for util functions related to compiler plugin.
 */
public class Utils {

    public static final String PREFIX = "ballerina/websocket:";
    public static final String CALLER = "Caller";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String STRING = "string";
    public static final String INT = "int";
    public static final String ERROR = "Error";
    public static final String GENERIC_ERROR = "error";
    public static final String OPTIONAL = "?";
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
    static final String CODE = "WS_110";
    static final String ON_ERROR = "onError";
    static final String ON_OPEN = "onOpen";
    static final String ON_CLOSE = "onClose";
    static final String ON_IDLE_TIMEOUT = "onIdleTimeout";
    static final String ON_TEXT_MESSAGE = "onTextMessage";
    static final String ON_BINARY_MESSAGE = "onBinaryMessage";
    static final String REMOTE_KEY_WORD = "remote";
    static final String RESOURCE_KEY_WORD = "resource";

    public static String getPrefix(SyntaxNodeAnalysisContext ctx) {
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (importDeclaration.moduleName().get(0).toString().split(" ")[0]
                    .compareTo(WebSocketConstants.PACKAGE_WEBSOCKET) == 0) {
                if (importDeclaration.prefix().isPresent()) {
                    return importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }
        return WebSocketConstants.PACKAGE_WEBSOCKET;
    }

    public static void validateOnOpenFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        for (ParameterSymbol inputParam : inputParams) {
            if (!inputParams.isEmpty()) {
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.startsWith("ballerina/websocket:") || !paramSignature.endsWith(CALLER)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_INPUT_PARAMS_FOR_ON_OPEN_CODE,
                            INVALID_INPUT_PARAMS_FOR_ON_OPEN, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                            WebSocketConstants.PACKAGE_WEBSOCKET));
                }
            }
        }
        TypeSymbol returnTypeSymbol = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnTypeSymbol, ON_OPEN, resourceNode, ctx);
    }

    public static void validateOnBinaryMessageFunction(FunctionTypeSymbol functionTypeSymbol,
            SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1 && !inputParams.get(0).typeDescriptor().signature().equals(BYTE_ARRAY)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS_CODE,
                    INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                    inputParams.get(0).typeDescriptor().signature()));
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(BYTE_ARRAY) && !paramSignature.equals(moduleId + COLON + CALLER)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_INPUT_FOR_ON_BINARY_CODE,
                            INVALID_INPUT_FOR_ON_BINARY, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), paramSignature));
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnStatement, ON_BINARY_MESSAGE, resourceNode, ctx);
    }

    public static void validateOnDataReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) && !(symbol.typeKind() == TypeDescKind.NIL) && !(
                        symbol.typeKind() == TypeDescKind.STRING) && !(symbol.typeKind() == TypeDescKind.ARRAY)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_RETURN_TYPES_ON_DATA_CODE,
                            INVALID_RETURN_TYPES_ON_DATA, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), symbol.signature(),
                                    functionName));
                }
            }
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL) && !(returnTypeSymbol.typeKind()
                == TypeDescKind.ARRAY) && !(returnTypeSymbol.typeKind() == TypeDescKind.STRING)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_RETURN_TYPES_ON_DATA_CODE,
                    INVALID_RETURN_TYPES_ON_DATA, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), returnTypeSymbol.signature(),
                            functionName));
        }
    }

    static void validateOnErrorFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1) {
            ParameterSymbol inputParam = inputParams.get(0);
            String moduleId = getModuleId(inputParam);
            String inputParamTypeDescSignature = inputParam.typeDescriptor().signature();
            if (!inputParamTypeDescSignature.contains(GENERIC_ERROR) && !inputParamTypeDescSignature
                    .equals(moduleId + COLON + ERROR)) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                        INVALID_INPUT_FOR_ON_ERROR_WITH_ONE_PARAMS_CODE,
                        INVALID_INPUT_FOR_ON_ERROR_WITH_ONE_PARAMS, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                        inputParamTypeDescSignature));
            }
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.contains(GENERIC_ERROR) && !paramSignature.equals(moduleId + COLON + CALLER)
                        && !paramSignature.equals(moduleId + COLON + ERROR)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_INPUT_FOR_ON_ERROR_CODE,
                            INVALID_INPUT_FOR_ON_ERROR, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), paramSignature));
                }
            }
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), ON_ERROR,
                resourceNode, ctx);
    }

    private static void validateErrorReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) && !(symbol.typeKind() == TypeDescKind.NIL)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RETURN_TYPES_CODE, INVALID_RETURN_TYPES,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), functionName,
                                    WebSocketConstants.PACKAGE_WEBSOCKET + COLON + ERROR + OPTIONAL));
                }
            }
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RETURN_TYPES_CODE, INVALID_RETURN_TYPES,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), functionName,
                            WebSocketConstants.PACKAGE_WEBSOCKET + COLON + ERROR + OPTIONAL));
        }
    }

    static void validateOnCloseFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() > 3 || inputParams.size() < 1) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_PARAMS_FOR_ON_CLOSE_CODE,
                    INVALID_INPUT_PARAMS_FOR_ON_CLOSE, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), WebSocketConstants.PACKAGE_WEBSOCKET));
        } else if (inputParams.size() == 1 && inputParams.get(0).typeDescriptor().signature()
                .equals(STRING)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS_CODE,
                    INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                    inputParams.get(0).typeDescriptor().signature()));
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(STRING) && !paramSignature.equals(moduleId + COLON + CALLER)
                        && !paramSignature.equals(INT)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_INPUT_PARAM_FOR_ON_CLOSE_CODE,
                            INVALID_INPUT_PARAM_FOR_ON_CLOSE, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), paramSignature));
                }
            }
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), ON_CLOSE,
                resourceNode, ctx);
    }

    static void validateOnIdleTimeoutFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() > 1) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_PARAMS_FOR_ON_IDLE_TIMEOUT_CODE,
                    INVALID_INPUT_PARAMS_FOR_ON_IDLE_TIMEOUT, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), WebSocketConstants.PACKAGE_WEBSOCKET));
        } else if (inputParams.size() == 1 && (!inputParams.get(0).typeDescriptor().signature().endsWith(":Caller")
                || !inputParams.get(0).typeDescriptor().signature().startsWith(PREFIX))) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_PARAM_FOR_ON_IDLE_TIMEOUT_CODE,
                    INVALID_INPUT_PARAM_FOR_ON_IDLE_TIMEOUT, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                    inputParams.get(0).typeDescriptor().signature()));
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(),
                ON_IDLE_TIMEOUT, resourceNode, ctx);
    }

    static void validateOnTextMessageFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1 && !inputParams.get(0).typeDescriptor().signature().equals(STRING)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                    INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS_CODE,
                    INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(),
                    inputParams.get(0).typeDescriptor().signature()));
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(STRING) && !paramSignature.equals(moduleId + COLON + CALLER)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(
                            INVALID_INPUT_FOR_ON_TEXT_CODE,
                            INVALID_INPUT_FOR_ON_TEXT, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), paramSignature));
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnStatement, ON_TEXT_MESSAGE, resourceNode, ctx);
    }

    private static String getModuleId(ParameterSymbol inputParam) {
        String moduleId = WebSocketConstants.PACKAGE_WEBSOCKET;
        if (inputParam.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE
                || inputParam.typeDescriptor().typeKind() == TypeDescKind.ERROR) {
            moduleId = inputParam.typeDescriptor().getModule().get().id().toString();
        }
        return moduleId;
    }

    public static boolean equals(String actual, String expected) {
        return actual.compareTo(expected) == 0;
    }
}

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
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

import java.util.List;

import static io.ballerina.stdlib.websocket.plugin.PluginConstants.COLON;

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
                if (!paramSignature.startsWith(PREFIX) || !paramSignature.endsWith(CALLER)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAMS_FOR_ON_OPEN,
                            resourceNode.location(), resourceNode.location(), WebSocketConstants.PACKAGE_WEBSOCKET);
                }
            }
        }
        TypeSymbol returnTypeSymbol = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnTypeSymbol, PluginConstants.ON_OPEN, resourceNode, ctx);
    }

    public static void validateOnBinaryMessageFunction(FunctionTypeSymbol functionTypeSymbol,
            SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1 && !inputParams.get(0).typeDescriptor().signature().equals(BYTE_ARRAY)) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS,
                    resourceNode.location(), resourceNode.location(), inputParams.get(0).typeDescriptor().signature());
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(BYTE_ARRAY) && !paramSignature.equals(moduleId + COLON + CALLER)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_BINARY,
                            resourceNode.location(), resourceNode.location(), paramSignature);
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnStatement, PluginConstants.ON_BINARY_MESSAGE, resourceNode, ctx);
    }

    public static void validateOnDataReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) && !(symbol.typeKind() == TypeDescKind.TYPE_REFERENCE
                        && symbol.signature().contains(ERROR)) && !(symbol.typeKind() == TypeDescKind.NIL) && !(
                        symbol.typeKind() == TypeDescKind.STRING) && !(symbol.typeKind() == TypeDescKind.ARRAY)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA,
                            resourceNode.location(), symbol.signature(), functionName);
                }
            }
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL) && !(returnTypeSymbol.typeKind()
                == TypeDescKind.ARRAY) && !(returnTypeSymbol.typeKind() == TypeDescKind.STRING)) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA,
                    resourceNode.location(), returnTypeSymbol.signature(),
                    functionName);
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
                reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_ERROR_WITH_ONE_PARAMS,
                        resourceNode.location(), inputParamTypeDescSignature);
            }
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.contains(GENERIC_ERROR) && !paramSignature.equals(moduleId + COLON + CALLER)
                        && !paramSignature.equals(moduleId + COLON + ERROR)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_ERROR,
                            resourceNode.location(), paramSignature);
                }
            }
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), PluginConstants.ON_ERROR,
                resourceNode, ctx);
    }

    private static void validateErrorReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) && !(symbol.typeKind() == TypeDescKind.NIL)
                        && !(symbol.typeKind() == TypeDescKind.TYPE_REFERENCE && symbol.signature()
                        .endsWith(SyntaxKind.COLON_TOKEN.stringValue() + ERROR))) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES,
                            resourceNode.location(), functionName,
                            WebSocketConstants.PACKAGE_WEBSOCKET + COLON + ERROR + OPTIONAL);
                }
            }
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL)) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_RETURN_TYPES, resourceNode.location(),
                    functionName, WebSocketConstants.PACKAGE_WEBSOCKET + COLON + ERROR + OPTIONAL);
        }
    }

    static void validateOnCloseFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() > 3 || inputParams.size() < 1) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAMS_FOR_ON_CLOSE,
                    resourceNode.location());
        } else if (inputParams.size() == 1 && inputParams.get(0).typeDescriptor().signature()
                .equals(STRING)) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS,
                    resourceNode.location(), inputParams.get(0).typeDescriptor().signature());
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(STRING) && !paramSignature.equals(moduleId + COLON + CALLER)
                        && !paramSignature.equals(INT)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAM_FOR_ON_CLOSE,
                            resourceNode.location(), paramSignature);
                }
            }
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), PluginConstants.ON_CLOSE,
                resourceNode, ctx);
    }

    public static void reportDiagnostics(SyntaxNodeAnalysisContext context, PluginConstants.CompilationErrors error,
            NodeLocation location, Object... args) {
        String errorMessage = error.getError();
        String diagnosticCode = error.getErrorCode();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(diagnosticCode, errorMessage, DiagnosticSeverity.ERROR);
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
        context.reportDiagnostic(diagnostic);
    }

    static void validateOnIdleTimeoutFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() > 1) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAMS_FOR_ON_IDLE_TIMEOUT,
                    resourceNode.location());
        } else if (inputParams.size() == 1 && (!inputParams.get(0).typeDescriptor().signature().endsWith(":Caller")
                || !inputParams.get(0).typeDescriptor().signature().startsWith(PREFIX))) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAM_FOR_ON_IDLE_TIMEOUT,
                    resourceNode.location(), inputParams.get(0).typeDescriptor().signature());
        }
        validateErrorReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(),
                PluginConstants.ON_IDLE_TIMEOUT, resourceNode, ctx);
    }

    static void validateOnTextMessageFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1 && !inputParams.get(0).typeDescriptor().signature().equals(STRING)) {
            reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS,
                    resourceNode.location(), inputParams.get(0).typeDescriptor().signature());
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.equals(STRING) && !paramSignature.equals(moduleId + COLON + CALLER)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_TEXT,
                            resourceNode.location(), paramSignature);
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnStatement, PluginConstants.ON_TEXT_MESSAGE, resourceNode, ctx);
    }

    private static String getModuleId(ParameterSymbol inputParam) {
        String moduleId = WebSocketConstants.PACKAGE_WEBSOCKET;
        if (inputParam.typeDescriptor().typeKind() == TypeDescKind.TYPE_REFERENCE
                || inputParam.typeDescriptor().typeKind() == TypeDescKind.ERROR) {
            moduleId = inputParam.typeDescriptor().getModule().get().id().toString();
        }
        return moduleId;
    }

    public static boolean isWithinRange(LineRange lineRange, LinePosition pos) {
        int sLine = lineRange.startLine().line();
        int sCol = lineRange.startLine().offset();
        int eLine = lineRange.endLine().line();
        int eCol = lineRange.endLine().offset();

        return ((sLine == eLine && pos.line() == sLine) &&
                (pos.offset() >= sCol && pos.offset() <= eCol)
        ) || ((sLine != eLine) && (pos.line() > sLine && pos.line() < eLine ||
                pos.line() == eLine && pos.offset() <= eCol ||
                pos.line() == sLine && pos.offset() >= sCol
        ));
    }

    public static boolean equals(String actual, String expected) {
        return actual.compareTo(expected) == 0;
    }
}

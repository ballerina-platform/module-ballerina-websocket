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
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeLocation;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websocket.WebSocketConstants.CLOSE_FRAME_TYPE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.CUSTOM_CLOSE_FRAME_TYPE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PACKAGE_WEBSOCKET;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PREDEFINED_CLOSE_FRAME_TYPE;
import static io.ballerina.stdlib.websocket.plugin.PluginConstants.CLOSE_FRAME;
import static io.ballerina.stdlib.websocket.plugin.PluginConstants.COLON;
import static io.ballerina.stdlib.websocket.plugin.PluginConstants.PIPE;

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
    public static final String NODE_LOCATION = "node.location";

    public static String getPrefix(SyntaxNodeAnalysisContext ctx) {
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (importDeclaration.moduleName().get(0).toString().split(" ")[0]
                    .compareTo(PACKAGE_WEBSOCKET) == 0) {
                if (importDeclaration.prefix().isPresent()) {
                    return importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }
        return PACKAGE_WEBSOCKET;
    }

    public static void validateOnOpenFunction(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
            FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        for (ParameterSymbol inputParam : inputParams) {
            if (!inputParams.isEmpty()) {
                String paramSignature = inputParam.typeDescriptor().signature();
                if (!paramSignature.startsWith(PREFIX) || !paramSignature.endsWith(CALLER)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_PARAMS_FOR_ON_OPEN,
                            resourceNode.location(), resourceNode.location(), PACKAGE_WEBSOCKET);
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
                if (!(paramSignature.equals(BYTE_ARRAY) || paramSignature.equals(moduleId + COLON + CALLER) ||
                        (inputParam.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION &&
                                paramSignature.contains(BYTE_ARRAY)))) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_BINARY,
                            resourceNode.location(), paramSignature);
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnDataReturnTypes(returnStatement, PluginConstants.ON_BINARY_MESSAGE, resourceNode, ctx);
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

    public static void validateOnDataReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) && !(symbol.typeKind() == TypeDescKind.TYPE_REFERENCE
                        && symbol.signature().contains(ERROR)) && !(symbol.typeKind() == TypeDescKind.NIL) && !(
                        symbol.typeKind() == TypeDescKind.STRING) && !(symbol.typeKind() == TypeDescKind.ARRAY)
                && !(symbol.typeKind() == TypeDescKind.STREAM)) {
                    repoteDiagnostics(functionName, resourceNode, ctx,
                            PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA, symbol.signature());
                }
            }
            validateContradictingReturnTypes(returnTypeSymbol, functionName, resourceNode, ctx);
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL) && !(returnTypeSymbol.typeKind()
                == TypeDescKind.ARRAY) && !(returnTypeSymbol.typeKind() == TypeDescKind.STRING)) {
            repoteDiagnostics(functionName, resourceNode, ctx,
                    PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA, returnTypeSymbol.signature());
        }
    }

    public static void validateContradictingReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
                                                 FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        boolean hasStreamType = false;
        boolean hasOtherType = false;
        for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
            if (symbol.typeKind() == TypeDescKind.STREAM) {
                hasStreamType = true;
            } else if (symbol.typeKind() != TypeDescKind.ERROR) {
                hasOtherType = true;
            }
        }
        if (hasStreamType && hasOtherType) {
            repoteDiagnostics(functionName, resourceNode, ctx,
                    PluginConstants.CompilationErrors.CONTRADICTING_RETURN_TYPES, returnTypeSymbol.signature());
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
        validateReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), PluginConstants.ON_ERROR,
                resourceNode, ctx);
    }

    private static void validateReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
            FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        String allowedTypes = String.format("%s%s%s%s", PACKAGE_WEBSOCKET, COLON, ERROR, OPTIONAL) + PIPE +
                String.format("%s%s%s%s", PACKAGE_WEBSOCKET, COLON, CLOSE_FRAME, OPTIONAL);
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (!(symbol.typeKind() == TypeDescKind.ERROR) &&
                        !(isCloseFrameRecordType(symbol)) &&
                        !(symbol.typeKind() == TypeDescKind.NIL) &&
                        !(symbol.typeKind() == TypeDescKind.TYPE_REFERENCE && symbol.signature()
                                .endsWith(SyntaxKind.COLON_TOKEN.stringValue() + ERROR))) {
                    repoteDiagnostics(allowedTypes, resourceNode, ctx,
                            PluginConstants.CompilationErrors.INVALID_RETURN_TYPES, functionName);
                }
            }
        } else if (!(returnTypeSymbol.typeKind() == TypeDescKind.NIL) && !isCloseFrameRecordType(returnTypeSymbol)) {
            repoteDiagnostics(allowedTypes, resourceNode, ctx,
                    PluginConstants.CompilationErrors.INVALID_RETURN_TYPES, functionName);
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
        validateReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), PluginConstants.ON_CLOSE,
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
        validateReturnTypes(functionTypeSymbol.returnTypeDescriptor().get(), PluginConstants.ON_IDLE_TIMEOUT,
                resourceNode, ctx);
    }

    static void validateOnDataFunctions(FunctionTypeSymbol functionTypeSymbol, SyntaxNodeAnalysisContext ctx,
                                        FunctionDefinitionNode resourceNode) {
        List<ParameterSymbol> inputParams = functionTypeSymbol.params().get();
        if (inputParams.size() == 1) {
            ParameterSymbol inputParam = inputParams.get(0);
            TypeDescKind kind = inputParam.typeDescriptor().typeKind();
            String paramSignature = inputParam.typeDescriptor().signature();
            if (!(isValidInput(getModuleId(inputParam), paramSignature, kind)) &&
                    inputParams.get(0).signature().contains(COLON + CALLER)) {
                reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_MESSAGE,
                        resourceNode.location(), resourceNode.functionName(),
                        inputParams.get(0).typeDescriptor().signature());
            }
        } else {
            for (ParameterSymbol inputParam : inputParams) {
                String moduleId = getModuleId(inputParam);
                String paramSignature = inputParam.typeDescriptor().signature();
                TypeDescKind kind = inputParam.typeDescriptor().typeKind();
                if (isValidInput(moduleId, paramSignature, kind)) {
                    reportDiagnostics(ctx, PluginConstants.CompilationErrors.INVALID_INPUT_FOR_ON_MESSAGE,
                            resourceNode.location(), resourceNode.functionName(), paramSignature);
                }
            }
        }
        TypeSymbol returnStatement = functionTypeSymbol.returnTypeDescriptor().get();
        validateOnTextReturnTypes(returnStatement, PluginConstants.ON_TEXT_MESSAGE, resourceNode, ctx);
    }

    private static boolean isValidInput(String moduleId, String paramSignature, TypeDescKind kind) {
        return !kind.isStringType() && !paramSignature.equals(moduleId + COLON + CALLER) &&
                !kind.isXMLType() && !kind.equals(TypeDescKind.JSON) &&
                !kind.equals(TypeDescKind.TYPE_REFERENCE) &&
                !kind.equals(TypeDescKind.ARRAY) && !kind.equals(TypeDescKind.BOOLEAN) &&
                !kind.equals(TypeDescKind.INT) && !kind.equals(TypeDescKind.DECIMAL) &&
                !kind.equals(TypeDescKind.FLOAT) && !kind.equals(TypeDescKind.INTERSECTION) &&
                !kind.equals(TypeDescKind.ANYDATA) && !kind.equals(TypeDescKind.UNION);
    }

    public static void validateOnTextReturnTypes(TypeSymbol returnTypeSymbol, String functionName,
                                                 FunctionDefinitionNode resourceNode, SyntaxNodeAnalysisContext ctx) {
        if (returnTypeSymbol.typeKind() == TypeDescKind.UNION) {
            for (TypeSymbol symbol : (((UnionTypeSymbol) returnTypeSymbol).memberTypeDescriptors())) {
                if (isInvalidReturnType(symbol)) {
                    repoteDiagnostics(functionName, resourceNode, ctx,
                            PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA, symbol.signature());
                }
            }
            validateContradictingReturnTypes(returnTypeSymbol, functionName, resourceNode, ctx);
        } else if (isInvalidReturnType(returnTypeSymbol)) {
            repoteDiagnostics(functionName, resourceNode, ctx,
                    PluginConstants.CompilationErrors.INVALID_RETURN_TYPES_ON_DATA, returnTypeSymbol.signature());
        }
    }

    private static void repoteDiagnostics(String functionName, FunctionDefinitionNode resourceNode,
                      SyntaxNodeAnalysisContext ctx, PluginConstants.CompilationErrors invalidReturnTypesOnData,
                      String signature) {
        reportDiagnostics(ctx, invalidReturnTypesOnData,
                resourceNode.location(), signature,
                functionName);
    }

    private static boolean isInvalidReturnType(TypeSymbol symbol) {
        List<TypeDescKind> validReturnTypes = Arrays.asList(TypeDescKind.ERROR, TypeDescKind.TYPE_REFERENCE,
                                                            TypeDescKind.STREAM, TypeDescKind.NIL, TypeDescKind.STRING,
                                                            TypeDescKind.ARRAY, TypeDescKind.INT, TypeDescKind.BOOLEAN,
                                                            TypeDescKind.DECIMAL, TypeDescKind.JSON, TypeDescKind.XML,
                                                            TypeDescKind.FLOAT);
        return !validReturnTypes.contains(symbol.typeKind());
    }

    private static String getModuleId(ParameterSymbol inputParam) {
        String moduleId = PACKAGE_WEBSOCKET;
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

    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        if (lineRange == null) {
            return null;
        }

        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }

    public static LineRange getLineRange(CodeActionExecutionContext codeActionExecutionContext) {
        LineRange lineRange = null;
        for (CodeActionArgument argument : codeActionExecutionContext.arguments()) {
            if (Utils.NODE_LOCATION.equals(argument.key())) {
                lineRange = argument.valueAs(LineRange.class);
            }
        }
        return lineRange;
    }

    public static List<DocumentEdit> getDocumentEdits(CodeActionExecutionContext codeActionExecutionContext,
                                                      String text) {
        LineRange lineRange = Utils.getLineRange(codeActionExecutionContext);

        if (lineRange == null) {
            return Collections.emptyList();
        }

        SyntaxTree syntaxTree = codeActionExecutionContext.currentDocument().syntaxTree();
        NonTerminalNode node = Utils.findNode(syntaxTree, lineRange);
        if (!(node instanceof ClassDefinitionNode)) {
            return Collections.emptyList();
        }

        ClassDefinitionNode classDefinitionNode = (ClassDefinitionNode) node;

        List<TextEdit> textEdits = new ArrayList<>();
        TextRange onTextMessageTextRange;
        if (classDefinitionNode.members().isEmpty()) {
            onTextMessageTextRange = TextRange.from(classDefinitionNode.openBrace().textRange().endOffset(),
                    classDefinitionNode.closeBrace().textRange().startOffset() -
                            classDefinitionNode.openBrace().textRange().endOffset());
        } else {
            Node lastMember = classDefinitionNode.members().get(classDefinitionNode.members().size() - 1);
            onTextMessageTextRange = TextRange.from(lastMember.textRange().endOffset(),
                    classDefinitionNode.closeBrace().textRange().startOffset() -
                            lastMember.textRange().endOffset());
        }
        textEdits.add(TextEdit.from(onTextMessageTextRange, text));
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(new DocumentEdit(codeActionExecutionContext.fileUri(),
                SyntaxTree.from(syntaxTree, change)));
    }


    public static Optional<CodeActionInfo> getCodeActionInfo(CodeActionContext codeActionContext, String codeAction) {
        Diagnostic diagnostic = codeActionContext.diagnostic();
        if (diagnostic.location() == null) {
            return Optional.empty();
        }
        CodeActionArgument locationArg = CodeActionArgument.from(Utils.NODE_LOCATION,
                diagnostic.location().lineRange());
        return Optional.of(CodeActionInfo.from(codeAction, List.of(locationArg)));
    }

    private static boolean isCloseFrameRecordType(TypeSymbol typeSymbol) {
        if (typeSymbol instanceof TypeReferenceTypeSymbol) {
            if (typeSymbol.nameEquals(CLOSE_FRAME) &&
                    typeSymbol.getModule().flatMap(Symbol::getName).orElse("").equals(PACKAGE_WEBSOCKET)) {
                return true;
            }
            typeSymbol = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            return isCloseFrameRecordType(typeSymbol);
        } else if (typeSymbol instanceof RecordTypeSymbol bRecordTypeSymbol) {
            if (bRecordTypeSymbol.fieldDescriptors().containsKey(CLOSE_FRAME_TYPE)) {
                TypeSymbol objectType = bRecordTypeSymbol.fieldDescriptors().get(CLOSE_FRAME_TYPE).typeDescriptor();
                String moduleName = objectType.getModule().flatMap(Symbol::getName).orElse("");
                return moduleName.equals(PACKAGE_WEBSOCKET) &&
                        (objectType.nameEquals(PREDEFINED_CLOSE_FRAME_TYPE) ||
                                objectType.nameEquals(CUSTOM_CLOSE_FRAME_TYPE));
            }
        } else if (typeSymbol instanceof IntersectionTypeSymbol intersectionTypeSymbol) {
            for (TypeSymbol memberType : intersectionTypeSymbol.memberTypeDescriptors()) {
                if (isCloseFrameRecordType(memberType)) {
                    return true;
                }
            }
        }
        return false;
    }
}

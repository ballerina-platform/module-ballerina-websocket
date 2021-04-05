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

import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static io.ballerina.stdlib.websocket.plugin.WebSocketUpgradeServiceValidator.FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE;

/**
 * A class for validating websocket service.
 */
public class WebSocketServiceValidator {
    private SyntaxNodeAnalysisContext ctx;
    private String modulePrefix;
    public static final String CALLER = "Caller";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String STRING = "string";
    public static final String ERROR = "Error";
    public static final String GENERIC_ERROR = "error";
    public static final String OPTIONAL = "?";
    private static final String CODE = "WS_110";
    private static final String ON_ERROR = "onError";
    public static final String INVALID_INPUT_PARAM_FOR_ONOPEN =
            "Invalid parameters `{0}` provided for onOpen remote function";
    public static final String INVALID_INPUT_PARAM_FOR_ONCLOSE =
            "Invalid parameters `{0}` provided for onClose remote function";
    public static final String INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onClose remote function. `string` is the mandatory parameter";
    public static final String INVALID_INPUT_PARAMS_FOR_ONOPEN =
            "Invalid parameters provided for onOpen remote function. Only `{0}`:Caller is allowed as the parameter";
    public static final String INVALID_RETURN_TYPES =
            "Invalid return types provided for `{0}` remote function, return type should be either `error?` or `{1}` ";
    public static final String INVALID_INPUT_PARAMS_FOR_ONCLOSE =
            "Invalid parameters provided for onClose remote function";
    public static final String INVALID_INPUT_PARAMS_FOR_ONERROR =
            "Invalid parameters provided for onError remote function";
    public static final String INVALID_INPUT_FOR_ONERROR_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onError remote function. `error` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ONERROR =
            "Invalid parameters `{0}` provided for onError remote function";
    public static final String INVALID_INPUT_PARAMS_FOR_ONIDLETIMEOUT = "Invalid parameters provided for "
            + "OnIdleTimeout remote function. Only `{0}`:Caller is allowed as the parameter";
    public static final String INVALID_INPUT_PARAM_FOR_ONIDLETIMEOUT =
            "Invalid parameters `{0}` provided for onIdleTimeout remote function";
    public static final String INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onTextMessage remote function. `string` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ON_TEXT =
            "Invalid parameters `{0}` provided for onTextMessage remote function";
    public static final String INVALID_RETURN_TYPES_ON_DATA =
            "Invalid return type `{0}` provided for `{1}` remote function";
    public static final String INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS =
            "Invalid parameters `{0}` provided for onBinaryMessage remote function. "
                    + "`byte[]` is the mandatory parameter";
    public static final String INVALID_INPUT_FOR_ON_BINARY =
            "Invalid parameters `{0}` provided for onBinaryMessage remote function";

    WebSocketServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext, String modulePrefix) {
        this.ctx = syntaxNodeAnalysisContext;
        this.modulePrefix = modulePrefix;
    }

    public void validate() {
        ClassDefinitionNode classDefNode = (ClassDefinitionNode) ctx.node();
        classDefNode.members().stream().filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION)
                .forEach(methodNode -> {
                    FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) methodNode;
                    switch (functionDefinitionNode.functionName().toString()) {
                    case "onOpen":
                        validateOnOpenFunction(functionDefinitionNode);
                        break;
                    case "onClose":
                        validateOnCloseFunction(functionDefinitionNode);
                        break;
                    case ON_ERROR:
                        validateOnErrorFunction(functionDefinitionNode);
                        break;
                    case "onIdleTimeout":
                        validateOnIdleTimeoutFunction(functionDefinitionNode);
                        break;
                    case "onTextMessage":
                        validateOnTextMessageFunction(functionDefinitionNode);
                        break;
                    case "onBinaryMessage":
                        validateOnBinaryMessageFunction(functionDefinitionNode);
                        break;
                    default:
                        reportInvalidFunction(functionDefinitionNode);
                    }
                });
    }

    private void validateOnOpenFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() > 1) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAMS_FOR_ONOPEN,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(
                        DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(), modulePrefix));
            } else if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(modulePrefix + "Caller")) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAM_FOR_ONOPEN,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateErrorReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void validateOnCloseFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() > 3) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAMS_FOR_ONCLOSE,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(
                        DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(), modulePrefix));
            } else if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(STRING)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ONCLOSE_WITH_ONE_PARAMS,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            } else {
                parameterNodes.forEach(parameterNode -> {
                    String parameterTypeName = ((RequiredParameterNode) parameterNode).typeName().toString().trim();
                    if (!parameterTypeName.equals(STRING) && !parameterTypeName.equals("int")
                            && !parameterTypeName.equals(modulePrefix + CALLER)) {
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAM_FOR_ONCLOSE,
                                DiagnosticSeverity.ERROR);
                        ctx.reportDiagnostic(DiagnosticFactory
                                .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName));
                    }
                });
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateErrorReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void validateOnErrorFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(GENERIC_ERROR) || parameterTypeName.toString()
                        .contains(modulePrefix + ERROR)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ONERROR_WITH_ONE_PARAMS,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            } else {
                parameterNodes.forEach(parameterNode -> {
                    String parameterTypeName = ((RequiredParameterNode) parameterNode).typeName().toString().trim();
                    if (!parameterTypeName.equals(GENERIC_ERROR) && !parameterTypeName.equals(modulePrefix + ERROR)
                            && !parameterTypeName.equals(modulePrefix + CALLER)) {
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ONERROR,
                                DiagnosticSeverity.ERROR);
                        ctx.reportDiagnostic(DiagnosticFactory
                                .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName));
                    }
                });
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateErrorReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void validateOnIdleTimeoutFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() > 1) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAMS_FOR_ONIDLETIMEOUT,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(
                        DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location(), modulePrefix));
            } else if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(modulePrefix + CALLER)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_PARAM_FOR_ONIDLETIMEOUT,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateErrorReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void validateOnTextMessageFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(STRING)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ON_TEXT_WITH_ONE_PARAMS,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            } else {
                parameterNodes.forEach(parameterNode -> {
                    String parameterTypeName = ((RequiredParameterNode) parameterNode).typeName().toString().trim();
                    if (!parameterTypeName.equals(STRING) && !parameterTypeName.equals(modulePrefix + CALLER)) {
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ON_TEXT,
                                DiagnosticSeverity.ERROR);
                        ctx.reportDiagnostic(DiagnosticFactory
                                .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName));
                    }
                });
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateOnDataReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void validateOnBinaryMessageFunction(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() == 1) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(BYTE_ARRAY)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_INPUT_FOR_ON_BINARY_WITH_ONE_PARAMS, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString()));
                }
            } else {
                parameterNodes.forEach(parameterNode -> {
                    String parameterTypeName = ((RequiredParameterNode) parameterNode).typeName().toString().trim();
                    if (!parameterTypeName.equals(BYTE_ARRAY) && !parameterTypeName.equals(modulePrefix + CALLER)) {
                        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_INPUT_FOR_ON_BINARY,
                                DiagnosticSeverity.ERROR);
                        ctx.reportDiagnostic(DiagnosticFactory
                                .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName));
                    }
                });
            }
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode.functionSignature().returnTypeDesc();
            if (returnTypesNode.isPresent()) {
                validateOnDataReturnTypes(resourceNode, returnTypesNode);
            }
        }
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE,
                DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }

    private void validateErrorReturnTypes(FunctionDefinitionNode resourceNode,
            Optional<ReturnTypeDescriptorNode> returnTypesNode) {
        Node returnTypeDescriptor = returnTypesNode.get().type();
        String returnTypeDescWithoutTrailingSpace = returnTypeDescriptor.toString().split(" ")[0];
        if (!(returnTypeDescriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                && returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0
                || returnTypeDescWithoutTrailingSpace.compareTo(GENERIC_ERROR + OPTIONAL) == 0)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_RETURN_TYPES, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), resourceNode.functionName(),
                            modulePrefix + ERROR + OPTIONAL));
        }
    }

    private void validateOnDataReturnTypes(FunctionDefinitionNode resourceNode,
            Optional<ReturnTypeDescriptorNode> returnTypesNode) {
        Node returnTypeDescriptor = returnTypesNode.get().type();
        String returnTypeDescWithoutTrailingSpace = returnTypeDescriptor.toString().split(" ")[0];
        if (returnTypeDescriptor.kind() == SyntaxKind.UNION_TYPE_DESC) {
            String[] returnTypeUnion = returnTypeDescWithoutTrailingSpace.split("\\|");
            for (String retType : returnTypeUnion) {
                if (!retType.equals(STRING) && !retType.equals(STRING + OPTIONAL) && !retType.equals(BYTE_ARRAY)
                        && !retType.equals(BYTE_ARRAY + OPTIONAL) && !retType.equals(GENERIC_ERROR) && !retType
                        .equals(GENERIC_ERROR + OPTIONAL) && !retType.equals(modulePrefix + ERROR) && !retType
                        .equals(modulePrefix + ERROR + OPTIONAL)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_RETURN_TYPES_ON_DATA,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), retType,
                                    resourceNode.functionName()));
                }
            }
        } else if (returnTypeDescriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                && !(returnTypeDescWithoutTrailingSpace.compareTo(GENERIC_ERROR + OPTIONAL) == 0)
                && !(returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0)
                && !(returnTypeDescWithoutTrailingSpace.compareTo(STRING + OPTIONAL) == 0)
                && !(returnTypeDescWithoutTrailingSpace.compareTo(BYTE_ARRAY + OPTIONAL) == 0)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, INVALID_RETURN_TYPES_ON_DATA,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory
                    .createDiagnostic(diagnosticInfo, resourceNode.location(), returnTypeDescWithoutTrailingSpace,
                            resourceNode.functionName()));
        }
    }
}

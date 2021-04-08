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

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.Optional;

/**
 * Class to validate WebSocket services.
 */
public class WebSocketUpgradeServiceValidator {
    private SyntaxNodeAnalysisContext ctx;
    private FunctionDefinitionNode resourceNode;
    private String modulePrefix;
    private static final String HTTP_REQUEST = "http:Request";
    public static final String INVALID_RESOURCE_ERROR_CODE = "WS_101";
    public static final String INVALID_RESOURCE_ERROR =
            "There should be only one `get` resource for the service";
    public static final String MORE_THAN_ONE_RESOURCE_PARAM_ERROR =
            "There should be only http:Request as a parameter";
    public static final String MORE_THAN_ONE_RESOURCE_PARAM_ERROR_CODE = "WS_102";
    public static final String INVALID_RESOURCE_PARAMETER_ERROR =
            "Invalid parameter `{0}` provided for `{1}`";
    public static final String INVALID_RESOURCE_PARAMETER_ERROR_CODE = "WS_103";
    public static final String INVALID_RETURN_TYPES_IN_RESOURCE =
            "Invalid return type `{0}` provided for function `{1}`, return type should be a subtype of `{2}`";
    public static final String INVALID_RETURN_TYPES_IN_RESOURCE_CODE =
            "WS_104";
    public static final String FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE = "Function `{0}` not accepted by the service";
    public static final String FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE_CODE = "WS_105";
    public static final String UPGRADE_ERROR = "UpgradeError";

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
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RESOURCE_ERROR_CODE, INVALID_RESOURCE_ERROR,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(
                        DiagnosticFactory.createDiagnostic(diagnosticInfo, serviceDeclarationNode.location()));
            }
        } else {
            serviceDeclarationNode.members().stream()
                    .filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION
                            || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).forEach(node -> {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                String functionName = functionDefinitionNode.functionName().toString().split(" ")[0];
                if (functionName.compareTo(WebSocketConstants.GET) == 0
                        && functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                    resourceNode = functionDefinitionNode;
                } else {
                    reportInvalidFunction(functionDefinitionNode);
                }
            });
            if (resourceNode != null) {
                validateResourceParams(resourceNode);
                validateResourceReturnTypes(resourceNode);
            }
        }
    }

    private void validateResourceParams(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            SeparatedNodeList<ParameterNode> parameterNodes = resourceNode.functionSignature().parameters();
            if (parameterNodes.size() > 1) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(MORE_THAN_ONE_RESOURCE_PARAM_ERROR_CODE,
                        MORE_THAN_ONE_RESOURCE_PARAM_ERROR, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory
                        .createDiagnostic(diagnosticInfo, resourceNode.location()));
            } else if (!parameterNodes.isEmpty()) {
                RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNodes.get(0);
                Node parameterTypeName = requiredParameterNode.typeName();
                if (!parameterTypeName.toString().contains(HTTP_REQUEST)) {
                    DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RESOURCE_PARAMETER_ERROR_CODE,
                            INVALID_RESOURCE_PARAMETER_ERROR, DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory
                            .createDiagnostic(diagnosticInfo, resourceNode.location(), parameterTypeName.toString(),
                                    HTTP_REQUEST));
                }
            }
        }
    }

    private void validateResourceReturnTypes(FunctionDefinitionNode resourceNode) {
        if (resourceNode != null) {
            Optional<ReturnTypeDescriptorNode> returnTypesNode = resourceNode
                    .functionSignature().returnTypeDesc();
            if (resourceNode.functionSignature().returnTypeDesc().isEmpty()) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RETURN_TYPES_IN_RESOURCE_CODE,
                        INVALID_RETURN_TYPES_IN_RESOURCE, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, resourceNode.location()));
            }
            Node returnTypeDescriptor = returnTypesNode.get().type();
            String returnTypeDescWithoutTrailingSpace = returnTypeDescriptor.toString().split(" ")[0];
            if (!(returnTypeDescWithoutTrailingSpace.contains(modulePrefix + "Service")
                    && returnTypeDescWithoutTrailingSpace.contains(modulePrefix + UPGRADE_ERROR))) {
                DiagnosticInfo diagnosticInfo = new DiagnosticInfo(INVALID_RETURN_TYPES_IN_RESOURCE_CODE,
                        INVALID_RETURN_TYPES_IN_RESOURCE, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, returnTypeDescriptor.location(),
                        returnTypeDescriptor.toString(), resourceNode.functionName(),
                        modulePrefix + "Service| " + modulePrefix + UPGRADE_ERROR));
            }

        }
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE_CODE,
                FUNCTION_NOT_ACCEPTED_BY_THE_SERVICE, DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, functionDefinitionNode.location(),
                functionDefinitionNode.functionName().toString()));
    }
}

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

import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates a Ballerina WebSocket Upgrade Service.
 */
public class WebSocketUpgradeServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();
        AtomicReference<WebSocketServiceValidator> wsServiceValidator = new AtomicReference<>();
        expressions.forEach(expressionNode -> {
            if (expressionNode.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
                TypeDescriptorNode typeDescriptorNode = ((ExplicitNewExpressionNode) expressionNode).typeDescriptor();
                Node moduleIdentifierTokenOfListener = typeDescriptorNode.children().get(0);
                if (moduleIdentifierTokenOfListener.syntaxTree().rootNode().kind() == SyntaxKind.MODULE_PART) {
                    ModulePartNode modulePartNode = moduleIdentifierTokenOfListener.syntaxTree().rootNode();
                    modulePartNode.imports().forEach(importDeclaration -> {
                        if (importDeclaration.moduleName().get(0).toString().split(" ")[0]
                                .compareTo(WebSocketConstants.PACKAGE_WEBSOCKET) == 0) {
                            if (importDeclaration.prefix().isEmpty() && moduleIdentifierTokenOfListener.toString()
                                    .compareTo(WebSocketConstants.PACKAGE_WEBSOCKET) == 0) {
                                wsServiceValidator.set(new WebSocketServiceValidator(ctx));
                            } else if (importDeclaration.prefix().isPresent()
                                    && moduleIdentifierTokenOfListener.toString().
                                    compareTo(importDeclaration.prefix().get().children().get(1).toString()) == 0) {
                                wsServiceValidator.set(new WebSocketServiceValidator(ctx));
                            }
                        }
                    });
                }
            } else if (expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                Node moduleIdentifierTokenOfListener = expressionNode.children().get(0);
                if (moduleIdentifierTokenOfListener.syntaxTree().rootNode().kind() == SyntaxKind.MODULE_PART) {
                    ModulePartNode modulePartNode = moduleIdentifierTokenOfListener.syntaxTree().rootNode();
                    modulePartNode.imports().forEach(importDeclaration -> {
                        if (importDeclaration.moduleName().get(0).toString().split(" ")[0]
                                .compareTo(WebSocketConstants.PACKAGE_WEBSOCKET) == 0) {
                            wsServiceValidator.set(new WebSocketServiceValidator(ctx));
                        }
                    });
                }
            }
        });

        if (wsServiceValidator.get() != null) {
            wsServiceValidator.get().validate();
        }
    }
}

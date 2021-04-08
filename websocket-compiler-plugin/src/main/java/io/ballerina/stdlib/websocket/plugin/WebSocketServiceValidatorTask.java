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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeReferenceNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

/**
 * Validates a Ballerina WebSocket Service.
 */
public class WebSocketServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ClassDefinitionNode classDefNode = (ClassDefinitionNode) syntaxNodeAnalysisContext.node();
        String modulePrefix = Utils.getPrefix(syntaxNodeAnalysisContext);
        classDefNode.members().stream().filter(child -> child.kind() == SyntaxKind.TYPE_REFERENCE).forEach(node -> {
            TypeReferenceNode wsServiceNode = (TypeReferenceNode) node;
            if (wsServiceNode.typeName().toString()
                    .equals(modulePrefix + SyntaxKind.COLON_TOKEN.stringValue() + "Service")) {
                WebSocketServiceValidator webSocketServiceValidator = new WebSocketServiceValidator(
                        syntaxNodeAnalysisContext, modulePrefix + SyntaxKind.COLON_TOKEN.stringValue());
                webSocketServiceValidator.validate();
            }
        });
    }
}

/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ListenerDeclarationVisitor} find the available listener declarations.
 */
public class ListenerInitExpressionNodeVisitor extends NodeVisitor {

    private final List<ImplicitNewExpressionNode> implicitNewExpressionNodes = new ArrayList<>();
    private final List<ExplicitNewExpressionNode> explicitNewExpressionNodes = new ArrayList<>();

    @Override
    public void visit(ImplicitNewExpressionNode node) {
        if (node.parent() instanceof ListenerDeclarationNode) {
            implicitNewExpressionNodes.add(node);
        }
    }

    @Override
    public void visit(ExplicitNewExpressionNode node) {
        if (node.typeDescriptor() instanceof QualifiedNameReferenceNode) {
            explicitNewExpressionNodes.add(node);
        }
    }

    List<ImplicitNewExpressionNode> getImplicitNewExpressionNodes() {
        return implicitNewExpressionNodes;
    }

    List<ExplicitNewExpressionNode> getExplicitNewExpressionNodes() {
        return explicitNewExpressionNodes;
    }
}

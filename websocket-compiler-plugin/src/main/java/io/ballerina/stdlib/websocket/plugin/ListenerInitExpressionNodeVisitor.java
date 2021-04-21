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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code ListenerDeclarationVisitor} find the available listener declarations.
 */
public class ListenerInitExpressionNodeVisitor extends NodeVisitor {

    private final List<ImplicitNewExpressionNode> implicitNewExpressionNodes = new ArrayList<>();
    private final List<ExplicitNewExpressionNode> explicitNewExpressionNodes = new ArrayList<>();
    private SyntaxNodeAnalysisContext ctx;

    public ListenerInitExpressionNodeVisitor(SyntaxNodeAnalysisContext ctx) {
        this.ctx = ctx;
    }
    @Override
    public void visit(ImplicitNewExpressionNode node) {
        if (node.parent() instanceof ListenerDeclarationNode) {
            ListenerDeclarationNode parentNode = (ListenerDeclarationNode) node.parent();
            Optional<TypeDescriptorNode> parentTypeOpt = parentNode.typeDescriptor();
            if (parentTypeOpt.isPresent()) {
                QualifiedNameReferenceNode parentType = (QualifiedNameReferenceNode) parentTypeOpt.get();
                Optional<Symbol> parentSymbolOpt = ctx.semanticModel().symbol(parentType);
                if (parentSymbolOpt.isPresent() && parentSymbolOpt.get() instanceof TypeReferenceTypeSymbol) {
                    TypeSymbol typeSymbol = ((TypeReferenceTypeSymbol) parentSymbolOpt.get()).typeDescriptor();
                    if (typeSymbol.typeKind() == TypeDescKind.OBJECT) {
                        ObjectTypeSymbol objectSymbol = (ObjectTypeSymbol) typeSymbol;
                        Optional<ModuleID> moduleId = objectSymbol.getModule().map(ModuleSymbol::id);
                        String identifier = objectSymbol.getName().orElse("");
                        if (moduleId.isPresent() && isWebSocketListener(moduleId.get(), identifier)) {
                            implicitNewExpressionNodes.add(node);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(ExplicitNewExpressionNode node) {
        if (node.typeDescriptor() instanceof QualifiedNameReferenceNode) {
            QualifiedNameReferenceNode nameRef = (QualifiedNameReferenceNode) node.typeDescriptor();
            Optional<Symbol> symbolOpt = ctx.semanticModel().symbol(nameRef);
            if (symbolOpt.isPresent() && symbolOpt.get() instanceof TypeReferenceTypeSymbol) {
                TypeSymbol typeSymbol = ((TypeReferenceTypeSymbol) symbolOpt.get()).typeDescriptor();
                if (typeSymbol.typeKind() == TypeDescKind.UNION) {
                    Optional<TypeSymbol> refSymbolOpt = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors()
                            .stream().filter(e -> e.typeKind() == TypeDescKind.TYPE_REFERENCE).findFirst();
                    if (refSymbolOpt.isPresent()) {
                        TypeReferenceTypeSymbol refSymbol = (TypeReferenceTypeSymbol) refSymbolOpt.get();
                        TypeSymbol typeDescriptor = refSymbol.typeDescriptor();
                        Optional<ModuleID> moduleId = typeDescriptor.getModule().map(ModuleSymbol::id);
                        String identifier = typeDescriptor.getName().orElse("");
                        if (moduleId.isPresent() && isWebSocketListener(moduleId.get(), identifier)) {
                            explicitNewExpressionNodes.add(node);
                        }
                    }
                }
            }
        }
    }

    private boolean isWebSocketListener(ModuleID moduleID, String identifier) {
        String orgName = moduleID.orgName();
        String packagePrefix = moduleID.modulePrefix();
        return WebSocketConstants.PACKAGE_WEBSOCKET.equals(packagePrefix) && PluginConstants.ORG_NAME.equals(orgName)
                && PluginConstants.LISTENER_IDENTIFIER.equals(identifier);
    }

    List<ImplicitNewExpressionNode> getImplicitNewExpressionNodes() {
        return implicitNewExpressionNodes;
    }

    List<ExplicitNewExpressionNode> getExplicitNewExpressionNodes() {
        return explicitNewExpressionNodes;
    }
}

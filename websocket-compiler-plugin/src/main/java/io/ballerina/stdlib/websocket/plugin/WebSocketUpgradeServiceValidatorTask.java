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

import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.List;
import java.util.Optional;

/**
 * Validates a Ballerina WebSocket Upgrade Service.
 */
public class WebSocketUpgradeServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private static final String ORG_NAME = "ballerina";

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();

        String modulePrefix = Utils.getPrefix(ctx);
        Optional<Symbol> serviceDeclarationSymbol = ctx.semanticModel().symbol(serviceDeclarationNode);
        if (serviceDeclarationSymbol.isPresent()) {
            List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceDeclarationSymbol.get())
                    .listenerTypes();
            for (TypeSymbol listenerType : listenerTypes) {
                if (isListenerBelongsToWebSocketModule(listenerType)) {
                    validateService(ctx, modulePrefix);
                    return;
                }
            }
        }
    }

    private boolean isListenerBelongsToWebSocketModule(TypeSymbol listenerType) {
        if (listenerType.typeKind() == TypeDescKind.UNION) {
            return ((UnionTypeSymbol) listenerType).memberTypeDescriptors().stream()
                    .filter(typeDescriptor -> typeDescriptor instanceof TypeReferenceTypeSymbol)
                    .map(typeReferenceTypeSymbol -> (TypeReferenceTypeSymbol) typeReferenceTypeSymbol)
                    .anyMatch(typeReferenceTypeSymbol -> isWebSocketModule(typeReferenceTypeSymbol.getModule().get()));
        }

        if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            return isWebSocketModule(((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule().get());
        }

        return false;
    }

    private boolean isWebSocketModule(ModuleSymbol moduleSymbol) {
        return Utils.equals(moduleSymbol.getName().get(), WebSocketConstants.PACKAGE_WEBSOCKET)
                && Utils.equals(moduleSymbol.id().orgName(), ORG_NAME);
    }

    private void validateService(SyntaxNodeAnalysisContext ctx, String modulePrefix) {
        WebSocketUpgradeServiceValidator wsServiceValidator;
        wsServiceValidator = new WebSocketUpgradeServiceValidator(ctx,
                modulePrefix + SyntaxKind.COLON_TOKEN.stringValue());
        wsServiceValidator.validate();
    }
}

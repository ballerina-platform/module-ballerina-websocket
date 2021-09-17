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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.wso2.ballerinalang.compiler.diagnostic.properties.NonCatProperty;

/**
 * Code action to add resource config to a resource method.
 */
public class AddWebSocketCodeTemplate implements CodeAction {
    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(PluginConstants.CompilationErrors.TEMPLATE_CODE_GENERATION_HINT.getErrorCode());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext codeActionContext) {
        Diagnostic diagnostic = codeActionContext.diagnostic();
        List<DiagnosticProperty<?>> properties = diagnostic.properties();
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        DiagnosticProperty<?> diagnosticProperty = properties.get(0);
        if (!(diagnosticProperty instanceof NonCatProperty) ||
                !(diagnosticProperty.value() instanceof ServiceDeclarationNode)) {
            return Optional.empty();
        }

        ServiceDeclarationNode functionDefinitionNode = (ServiceDeclarationNode) diagnosticProperty.value();

        CodeActionArgument locationArg = CodeActionArgument.from("node.location",
                functionDefinitionNode.location().lineRange());
        return Optional.of(CodeActionInfo.from("Add WebSocket resource code snippet", List.of(locationArg)));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext codeActionExecutionContext) {
        LineRange lineRange = null;
        for (CodeActionArgument argument : codeActionExecutionContext.arguments()) {
            if ("node.location".equals(argument.key())) {
                lineRange = argument.valueAs(LineRange.class);
            }
        }

        if (lineRange == null) {
            return Collections.emptyList();
        }

        SyntaxTree syntaxTree = codeActionExecutionContext.currentDocument().syntaxTree();
        NonTerminalNode node = findNode(syntaxTree, lineRange);
        if (!(node instanceof ServiceDeclarationNode)) {
            return Collections.emptyList();
        }

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;
        if (serviceDeclarationNode.metadata().isPresent() &&
                !serviceDeclarationNode.metadata().get().annotations().isEmpty()) {
            return Collections.emptyList();
        }

        List<TextEdit> textEdits = new ArrayList<>();

        String insertText = "resource function get .() {\n}";
        TextRange textRange = TextRange.from(serviceDeclarationNode.openBraceToken().textRange().endOffset(), 0);
        textEdits.add(TextEdit.from(textRange, insertText));
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(new DocumentEdit(codeActionExecutionContext.fileUri(),
                SyntaxTree.from(syntaxTree, change)));
    }

    @Override
    public String name() {
        return "ADD_RESOURCE_CONFIG_ANNOTATION";
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
}

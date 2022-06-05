/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.websocket.plugin.AddWebSocketCodeTemplate.LS;

/**
 * Code action to add onError remote function.
 */
public class OnErrorCodeTemplate implements CodeAction {

    public static final String ON_ERROR_FUNCTION = LS + LS +
            "\tremote function onError(websocket:Caller caller, websocket:Error err) " +
            "returns websocket:Error? {"  + LS + LS +
            "\t}" + LS;

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(PluginConstants.CompilationErrors.ON_ERROR_GENERATION_HINT.getErrorCode());
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext codeActionContext) {
        return Utils.getCodeActionInfo(codeActionContext, "Add onError function");
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext codeActionExecutionContext) {
        return Utils.getDocumentEdits(codeActionExecutionContext, ON_ERROR_FUNCTION);
    }

    @Override
    public String name() {
        return "ADD_ON_ERROR_CODE_SNIPPET";
    }
}

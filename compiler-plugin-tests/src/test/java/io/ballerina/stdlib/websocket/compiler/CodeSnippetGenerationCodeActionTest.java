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

package io.ballerina.stdlib.websocket.compiler;

import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.ballerina.stdlib.websocket.plugin.Utils.NODE_LOCATION;

/**
 * A class for testing code actions.
 */
public class CodeSnippetGenerationCodeActionTest extends AbstractCodeActionTest {

    @Test(dataProvider = "testDataProvider")
    public void testCodeActions(String srcFile, int line, int offset, CodeActionInfo expected, String resultFile, String samplePackage)
            throws IOException {
        Path filePath = RESOURCE_PATH.resolve("ballerina_sources")
                .resolve(samplePackage)
                .resolve(srcFile);
        Path resultPath = RESOURCE_PATH.resolve("codeaction")
                .resolve(getConfigDir())
                .resolve(resultFile);

        performTest(filePath, LinePosition.from(line, offset), expected, resultPath);
    }

    @DataProvider
    private Object[][] testDataProvider() {
        return new Object[][]{
                {"service.bal", 2, 8, getExpectedCodeAction("service.bal", 2, 47), "result.bal", "sample_package_37"},
                {"service2.bal", 2, 8, getExpectedCodeAction("service2.bal", 5, 1), "result2.bal", "sample_package_37"},
                {"on_open.bal", 25, 3, getExpectedCodeActionForWsService("on_open.bal", 26, 1, "WEBSOCKET_114", "ADD_ON_OPEN_CODE_SNIPPET", "Add onOpen function"), "on_open.bal", "sample_package_37"},
                {"on_close.bal", 25, 3, getExpectedCodeActionForWsService("on_close.bal", 26, 1, "WEBSOCKET_111", "ADD_ON_CLOSE_CODE_SNIPPET", "Add onClose function"), "on_close.bal", "sample_package_46"},
                {"on_binary.bal", 25, 3, getExpectedCodeActionForWsService("on_binary.bal", 26, 1, "WEBSOCKET_110", "ADD_ON_BINARY_CODE_SNIPPET", "Add onBinaryMessage function"), "on_binary.bal", "sample_package_47"},
                {"on_text.bal", 25, 3, getExpectedCodeActionForWsService("on_text.bal", 26, 1, "WEBSOCKET_117", "ADD_ON_TEXT_CODE_SNIPPET", "Add onTextMessage function"), "on_text.bal", "sample_package_48"},
                {"on_message.bal", 25, 3, getExpectedCodeActionForWsService("on_message.bal", 26, 1, "WEBSOCKET_118", "ADD_ON_MESSAGE_CODE_SNIPPET", "Add onMessage function"), "on_message.bal", "sample_package_49"},
                {"on_ping.bal", 25, 3, getExpectedCodeActionForWsService("on_ping.bal", 26, 1, "WEBSOCKET_115", "ADD_ON_PING_CODE_SNIPPET", "Add onPing function"), "on_ping.bal", "sample_package_50"},
                {"on_pong.bal", 25, 3, getExpectedCodeActionForWsService("on_pong.bal", 26, 1, "WEBSOCKET_116", "ADD_ON_PONG_CODE_SNIPPET", "Add onPong function"), "on_pong.bal", "sample_package_51"},
                {"on_error.bal", 25, 3, getExpectedCodeActionForWsService("on_error.bal", 26, 1, "WEBSOCKET_112", "ADD_ON_ERROR_CODE_SNIPPET", "Add onError function"), "on_error.bal", "sample_package_52"},
                {"on_idletimeout.bal", 25, 3, getExpectedCodeActionForWsService("on_idletimeout.bal", 26, 1, "WEBSOCKET_113", "ADD_ON_IDLE_TIMEOUT_CODE_SNIPPET", "Add onIdleTimeout function"), "on_idletimeout.bal", "sample_package_53"}
        };
    }

    private CodeActionInfo getExpectedCodeAction(String filePath, int line, int offset) {
        LineRange lineRange = LineRange.from(filePath, LinePosition.from(2, 0),
                LinePosition.from(line, offset));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from("Insert service template", List.of(locationArg));
        codeAction.setProviderName("WEBSOCKET_107/ballerina/websocket/ADD_RESOURCE_CODE_SNIPPET");
        return codeAction;
    }

    private CodeActionInfo getExpectedCodeActionForWsService(String filePath, int line, int offset, String diagnosticCode,
                                                             String diagnosticName, String codeActionName) {
        LineRange lineRange = LineRange.from(filePath, LinePosition.from(24, 0),
                LinePosition.from(line, offset));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from(codeActionName, List.of(locationArg));
        codeAction.setProviderName(String.format("%s/ballerina/websocket/%s", diagnosticCode, diagnosticName));
        return codeAction;
    }

    protected String getConfigDir() {
        return "code_snippet_generation";
    }
}

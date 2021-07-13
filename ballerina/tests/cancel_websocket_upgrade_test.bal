// Copyright (c) 2021 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// //www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/lang.'string as strings;

listener Listener l73 = new(21073);

service /onTextString on l73 {
   resource function get .() returns Service|UpgradeError {
       UpgradeError err = error("Want to cancel the handshake");
       return err;
   }
}

@test:Config {}
public function testWsUpgradeCancel() returns Error? {
   Client|Error wsClient = new("ws://localhost:21073/onTextString/");
   if (wsClient is Error) {
       test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
   } else {
       test:assertFail("Expected an error as the WebSocket handshake failure");
   }
}

@test:Config {}
public function testWsUpgradeCancelDueToPathError() returns Error? {
   Client|Error wsClient = new("ws://localhost:21073/onTextString/xyz");
   if (wsClient is Error) {
       test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
   } else {
       test:assertFail("Expected an error as the WebSocket handshake failure");
   }
}

@test:Config {}
public function testIncorrectPath() returns Error? {
   Client|Error wsClient = new("ws://localhost:21073/xyz");
   if (wsClient is Error) {
       test:assertTrue(strings:includes(wsClient.message(), "InvalidHandshakeError: Invalid handshake response"));
   } else {
       test:assertFail("Expected an error as the WebSocket handshake failure");
   }
}

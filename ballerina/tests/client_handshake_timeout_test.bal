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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/io;

listener Listener l71 = new(21076);

service /onTextString on l71 {
   resource function get .() returns Service|UpgradeError {
       runtime:sleep(2);
       return new WsService71();
   }
}

service class WsService71 {
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) returns string? {
      runtime:sleep(5);
      io:println("server on text message");
      return data;
  }

  remote isolated function onError(error err) returns Error? {
      io:println("server on error message");
  }
}

// Tests client handshake timeout
@test:Config {}
public function testHandshakeTimeoutError() returns Error? {
   Client|Error wsClient = new("ws://localhost:21076/onTextString", { handShakeTimeout: 1 });
   if (wsClient is Client) {
      test:assertFail("Expected a handshake timeout error");
   } else {
      io:println(wsClient.message());
      test:assertEquals("Error: Handshake timed out", wsClient.message());
   }
}

// Tests reset timeout after a successful handshake
@test:Config {}
public function testResetTimeoutAfterHandshake() returns Error? {
   Client|Error wsClient = new("ws://localhost:21076/onTextString", { handShakeTimeout: 4 });
   if (wsClient is Client) {
       check wsClient->writeTextMessage("Hello Ballerina");
       Error|string data = wsClient->readTextMessage();
       if (data is string) {
          test:assertEquals("Hello Ballerina", data);
       } else {
          test:assertFail("Should read the content without any error");
       }
   } else {
      test:assertFail("Should connect without any error");
   }
}

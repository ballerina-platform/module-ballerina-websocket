// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/runtime;
import ballerina/test;
import ballerina/http;

string arrivedData = "";
boolean isClientConnectionOpen = false;

service UpgradeService /'client/'service on new Listener(21021) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new clientFailure200();
   }
}

service class clientFailure200 {
  *Service;
  remote function onOpen(Caller wsEp) {
       isClientConnectionOpen = true;
   }
}

service class callback200 {
   *CallbackService;
   remote function onText(Caller caller, string text) {
   }
}

service class ClientService200 {
   *CallbackService;
   remote function onText(AsyncClient caller, string text) {
   }
}

// Tests the client initialization without a callback service.
@test:Config {}
public function testClientSuccessWithoutService() {
   AsyncClient wsClient = new ("ws://localhost:21021/client/service");
   runtime:sleep(500);
   test:assertTrue(isClientConnectionOpen);
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

// Tests the client initialization with a WebSocketClientService but without any resources.
@test:Config {}
public function testClientSuccessWithWebSocketClientService() {
   isClientConnectionOpen = false;
   AsyncClient wsClient = new ("ws://localhost:21021/client/service", new ClientService200());
   checkpanic wsClient->pushText("Client worked");
   runtime:sleep(500);
   test:assertTrue(isClientConnectionOpen);
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

// Tests the client initialization failure when used with a WebSocketService.
@test:Config {}
public function testClientFailureWithWebSocketService() {
   isClientConnectionOpen = false;
   AsyncClient|error wsClientEp = trap new ("ws://localhost:21021/client/service", new callback200());
   runtime:sleep(500);
   if (wsClientEp is error) {
       test:assertEquals(wsClientEp.message(),
           "GenericError: The callback service should be a WebSocket Client Service");
   } else {
       test:assertFail("Mismatched output");
   }
}

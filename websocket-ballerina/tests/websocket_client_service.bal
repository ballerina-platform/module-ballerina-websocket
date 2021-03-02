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

import ballerina/lang.runtime as runtime;
import ballerina/test;

string arrivedData = "";
boolean isClientConnectionOpen = false;
listener Listener l15 = new(21021);
service /'client/'service on l15 {
   resource isolated function get bbe() returns Service|UpgradeError {
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
   *Service;
   remote function onTextMessage(string caller, string text) {
   }
}

service class ClientService200 {
   *Service;
   remote function onTextMessage(Caller caller, string text) {
   }
}

// Tests the client initialization without a callback service.
@test:Config {}
public function testClientSuccessWithoutService() returns Error? {
   AsyncClient wsClient = check new ("ws://localhost:21021/client/service/bbe");
   runtime:sleep(0.5);
   test:assertTrue(isClientConnectionOpen);
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

// Tests the client initialization with a WebSocketClientService but without any resources.
@test:Config {}
public function testClientSuccessWithWebSocketClientService() returns Error? {
   isClientConnectionOpen = false;
   AsyncClient wsClient = check new("ws://localhost:21021/client/service/bbe", new ClientService200());
   check wsClient->writeTextMessage("Client worked");
   runtime:sleep(0.5);
   test:assertTrue(isClientConnectionOpen);
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

// Tests the client initialization failure when used with a WebSocketService.
// TODO: Commenting out the test as it is not possible to shut down the dynamic listener
// as the client creation returns an error.
// @test:Config {}
// public function testClientFailureWithWebSocketService() {
//    isClientConnectionOpen = false;
//    AsyncClient|error wsClientEp = new ("ws://localhost:21021/client/service/bbe", new callback200());
//    runtime:sleep(0.5);
//    if (wsClientEp is error) {
//        test:assertEquals(wsClientEp.message(),
//            "GenericError: The callback service should be a WebSocket Client Service");
//    } else {
//        test:assertFail("Mismatched output");
//    }
//    error? result = wsClientEp->close(statusCode = 1000, timeout = 0);
// }

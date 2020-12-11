// Copyright (c) 2020 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

import ballerina/runtime;
import ballerina/test;
import ballerina/io;
import ballerina/http;

listener Listener ep1 = new (21009);
string errorMsg = "";

service UpgradeService /cancel on ep1 {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service {
       io:println("Dispatched to onUpgrade /cancel");
       var returnVal = caller->cancelWebSocketUpgrade(404, "Cannot proceed");
       if (returnVal is http:WebSocketError) {
            panic <error>returnVal;
       }
       return new simpleProxy1();
    }
}

service class simpleProxy1 {
  *Service;
  remote isolated function onOpen(Caller caller) {
      io:println("Dispatched to onOpen");
  }
}

service UpgradeService /cannot/cancel on ep1 {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service {
       var returnVal = caller->cancelWebSocketUpgrade(200, "Cannot proceed");
       if (returnVal is http:WebSocketError) {
            panic <error>returnVal;
       }
       return new simpleProxy1();
    }
}

service object {} resourceNotFoundCallbackService = service object {

    remote function onError(Client wsEp, error err) {
        errorMsg = <@untainted>err.message();
    }
};

// Tests resource not found scenario.
@test:Config {}
public function testResourceNotFound() {
    Client wsClient = new ("ws://localhost:21009/proxy/cancell",
        {callbackService: resourceNotFoundCallbackService});
    runtime:sleep(500);
    test:assertEquals(errorMsg, "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");

}

// Tests the cancelWebSocketUpgrade method.
@test:Config {}
public function testCancelUpgrade() {
    Client wsClient = new ("ws://localhost:21009/simple/cancel",
        {callbackService: resourceNotFoundCallbackService});
    runtime:sleep(500);
    test:assertEquals(errorMsg, "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
}

// Tests the cancelWebSocketUpgrade method with a success status code.
@test:Config {}
public function testCancelUpgradeSuccessStatusCode() {
    Client wsClient = new ("ws://localhost:21009/cannot/cancel",
        {callbackService: resourceNotFoundCallbackService});
    runtime:sleep(500);
    test:assertEquals(errorMsg, "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
}

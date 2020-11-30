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
import ballerina/io;
import ballerina/websocket;

string arrivedData = "";
boolean isClientConnectionOpen = false;

@websocket:WebSocketServiceConfig {
    path: "/client/service"
}
service clientFailure200 on new websocket:Listener(21021) {

    resource function onOpen(websocket:WebSocketCaller wsEp) {
        isClientConnectionOpen = true;
    }
}

service callback200 = @websocket:WebSocketServiceConfig {} service {
    resource function onText(websocket:WebSocketCaller caller, string text) {
    }
};
service ClientService200 = @websocket:WebSocketServiceConfig {} service {
    resource function onText(websocket:WebSocketClient caller, string text) {
    }
};

// Tests the client initialization without a callback service.
@test:Config {}
public function testClientSuccessWithoutService() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21021/client/service");
    runtime:sleep(500);
    test:assertTrue(isClientConnectionOpen);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests the client initialization with a WebSocketClientService but without any resources.
@test:Config {}
public function testClientSuccessWithWebSocketClientService() {
    isClientConnectionOpen = false;
    websocket:WebSocketClient wsClient = new ("ws://localhost:21021/client/service", {callbackService: ClientService200});
    checkpanic wsClient->pushText("Client worked");
    runtime:sleep(500);
    test:assertTrue(isClientConnectionOpen);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests the client initialization failure when used with a WebSocketService.
@test:Config {}
public function testClientFailureWithWebSocketService() {
    isClientConnectionOpen = false;
    websocket:WebSocketClient|error wsClientEp = trap new ("ws://localhost:21021/client/service",
        {callbackService: callback200});
    runtime:sleep(500);
    if (wsClientEp is error) {
        test:assertEquals(wsClientEp.message(),
            "GenericError: The callback service should be a WebSocket Client Service");
    } else {
        test:assertFail("Mismatched output");
    }
}

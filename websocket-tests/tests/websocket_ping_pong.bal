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

byte[] expectedPongData = [];
byte[] expectedPongData1 = [];
@websocket:WebSocketServiceConfig {
    path: "/pingpong/ws"
}
service server on new websocket:Listener(21014) {

    resource function onOpen(websocket:WebSocketCaller caller) {
    }

    resource function onPing(websocket:WebSocketCaller caller, byte[] localData) {
        var returnVal = caller->pong(localData);
        if (returnVal is websocket:WebSocketError) {
            panic <error>returnVal;
        }
    }

    resource function onPong(websocket:WebSocketCaller caller, byte[] localData) {
        var returnVal = caller->ping(localData);
        if (returnVal is websocket:WebSocketError) {
            panic <error>returnVal;
        }
    }
}

service pingPongCallbackService = @websocket:WebSocketServiceConfig {} service {

    resource function onPing(websocket:WebSocketClient wsEp, byte[] localData) {
        expectedPongData1 = <@untainted>localData;
    }

    resource function onPong(websocket:WebSocketClient wsEp, byte[] localData) {
        expectedPongData = <@untainted>localData;
    }
};

// Tests ping to Ballerina WebSocket server
@test:Config {}
public function testPingToBallerinaServer() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21014/pingpong/ws",
        {callbackService: pingPongCallbackService});
    byte[] pongData = [5, 24, 56, 243];
    checkpanic wsClient->ping(pongData);
    runtime:sleep(500);
    test:assertEquals(expectedPongData, pongData);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests pong to Ballerina WebSocket server
@test:Config {}
public function testPingFromRemoteServerToBallerinaClient() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21014/pingpong/ws",
        {callbackService: pingPongCallbackService});
    byte[] pongData = [5, 24, 34];
    checkpanic wsClient->pong(pongData);
    runtime:sleep(500);
    test:assertEquals(expectedPongData1, pongData);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

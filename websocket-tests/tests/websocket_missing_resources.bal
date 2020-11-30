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
import ballerina/websocket;

string expectedData = "";
byte[] expectedBinData = [];
byte[] expectedPingBinaryData = [];

@websocket:WebSocketServiceConfig {
    idleTimeoutInSeconds: 10
}
service onlyOnBinary on new websocket:Listener(21005) {
    resource function onBinary(websocket:WebSocketCaller caller, byte[] data) {
        checkpanic caller->pushBinary(data);
    }
}

service onlyOnText on new websocket:Listener(21006) {
    resource function onText(websocket:WebSocketCaller caller, string data) {
        checkpanic caller->pushText(data);
    }
}

service callbackService = @websocket:WebSocketServiceConfig {} service {

    resource function onText(websocket:WebSocketClient wsEp, string text) {
        expectedData = <@untainted>text;
    }

    resource function onBinary(websocket:WebSocketClient wsEp, byte[] data) {
        expectedBinData = <@untainted>data;
    }

    resource function onPing(websocket:WebSocketClient wsEp, byte[] data) {
        expectedPingBinaryData = <@untainted>data;
    }
};

// Tests behavior when onText resource is missing and a text message is received
@test:Config {}
public function testMissingOnText() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21005/onlyOnBinary", {callbackService: callbackService});
    expectedData = "";
    byte[] binaryData = [5, 24, 56, 243];
    checkpanic wsClient->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(expectedData, "", msg = "Data mismatched");
    checkpanic wsClient->pushBinary(binaryData);
    runtime:sleep(500);
    test:assertEquals(expectedBinData, binaryData, msg = "Data mismatched");
    error? result = wsClient->close();
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests behavior when onPong resource is missing and a pong is received
@test:Config {}
public function testMissingOnPong() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21005/onlyOnBinary", {callbackService: callbackService});
    byte[] binaryData = [5, 24, 56, 243];
    expectedBinData = [];
    checkpanic wsClient->pong(binaryData);
    runtime:sleep(500);
    test:assertEquals(expectedPingBinaryData, expectedBinData, msg = "Data mismatched");
    checkpanic wsClient->pushBinary(binaryData);
    runtime:sleep(500);
    test:assertEquals(expectedBinData, binaryData, msg = "Data mismatched");
    error? result = wsClient->close();
    if (result is websocket:WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests behavior when onBinary resource is missing and binary message is received
@test:Config {}
public function testMissingOnBinary() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21006/onlyOnText", {callbackService: callbackService});
    byte[] binaryData = [5, 24, 56, 243];
    expectedBinData = [];
    byte[] expectedBinData = [];
    expectedData = "";
    checkpanic wsClient->pushBinary(binaryData);
    runtime:sleep(500);
    test:assertEquals(expectedBinData, expectedBinData, msg = "Data mismatched");
    checkpanic wsClient->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
    error? result = wsClient->close();
    if (result is websocket:WebSocketError) {
        io:println("Error occurred when closing connection", result);
    }
}

// Tests behavior when onBinary resource is missing and binary message is received
@test:Config {}
public function testMissingOnIdleTimeout() {
    websocket:WebSocketClient wsClient = new ("ws://localhost:21006/onlyOnText", {callbackService: callbackService});
    expectedData = "";
    runtime:sleep(500);
    checkpanic wsClient->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
        io:println("Error occurred when closing connection", result);
    }
}

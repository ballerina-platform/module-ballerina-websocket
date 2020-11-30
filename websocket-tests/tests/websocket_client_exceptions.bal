// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/log;
import ballerina/runtime;
import ballerina/test;
import ballerina/websocket;

string errMessage = "";

websocket:WebSocketClientConfiguration config = {callbackService: errorResourceService, subProtocols: ["xml"]};

service errorResourceService = @websocket:WebSocketServiceConfig {} service {
    resource function onError(websocket:WebSocketClient clientCaller, error err) {
        errMessage = <@untainted>err.message();
    }
};

@websocket:WebSocketServiceConfig {
    path: "/websocket"
}
service errorServer on new websocket:Listener(21030) {

    resource function onOpen(websocket:WebSocketCaller caller) {
        log:printInfo("The Connection ID: " + caller.getConnectionId());
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

    resource function onText(websocket:WebSocketCaller caller, string text, boolean finalFrame) {
        var err = caller->pushText(text, finalFrame);
        if (err is websocket:WebSocketError) {
            log:printError("Error occurred when sending text message", err);
        }
    }

    resource function onBinary(websocket:WebSocketCaller caller, byte[] data) {
        var returnVal = caller->pushBinary(data);
        if (returnVal is websocket:WebSocketError) {
            panic <error>returnVal;
        }
    }

    resource function onClose(websocket:WebSocketCaller ep, int statusCode, string reason) {
    }
}

// Connection refused IO error.
@test:Config {}
public function testConnectionError() {
    websocket:WebSocketClient wsClient = new ("ws://lmnop.ls", config);
    runtime:sleep(500);
    test:assertEquals(errMessage, "ConnectionError: IO Error");
}

// SSL/TLS error
@test:Config {}
public function testSslError() {
    websocket:WebSocketClient|error wsClient = new ("wss://localhost:21030/websocket", config);
    runtime:sleep(500);
    test:assertEquals(errMessage, "GenericError: SSL/TLS Error");
}

// The frame exceeds the max frame length
@test:Config {}
public function testLongFrameError() {
    string ping = "pingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingping"
        + "pingpingpingpingpingpingpingpingpingpingpingpingpingping";
    byte[] pingData = ping.toBytes();
    websocket:WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
    runtime:sleep(500);
    var err = wsClientEp->ping(pingData);
    if (err is error) {
        test:assertEquals(err.message(), "ProtocolError: io.netty.handler.codec.TooLongFrameException: " +
            "invalid payload for PING (payload length must be <= 125, was 148");
    } else {
        test:assertFail("Mismatched output");
    }
    error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
    if (result is websocket:WebSocketError) {
       log:printError("Error occurred when closing connection", result);
    }
}

// Close the connection and push text
@test:Config {}
public function testConnectionClosedError() {
    websocket:WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
    error? result = wsClientEp->close();
    if (result is websocket:WebSocketError) {
       log:printError("Error occurred when closing connection", result);
    }
    runtime:sleep(2000);
    var err = wsClientEp->pushText("some");
    if (err is error) {
        test:assertEquals(err.message(), "ConnectionClosureError: Close frame already sent. Cannot push text data!");
    } else {
        test:assertFail("Mismatched output");
    }
}

// Handshake failing because of missing subprotocol
@test:Config {}
public function testHandshakeError() {
    websocket:WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", config);
    runtime:sleep(500);
    test:assertEquals(errMessage, "InvalidHandshakeError: Invalid subprotocol. Actual: null. Expected one of: xml");
}

// Tests the ready function using the WebSocket client. When `readyOnConnect` is true,
// calls the `ready()` function.
@test:Config {}
public function testReadyOnConnect() {
    websocket:WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
    var err = wsClientEp->ready();
    if (err is error) {
        test:assertEquals(err.message(), "GenericError: Already started reading frames");
    } else {
        test:assertFail("Mismatched output");
    }
}

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
import ballerina/http;

string errMessage = "";

WebSocketClientConfiguration config = {callbackService: errorResourceService, subProtocols: ["xml"]};

service object {} errorResourceService = @WebSocketServiceConfig {} service object {
   remote function onError(WebSocketClient clientCaller, error err) {
       errMessage = <@untainted>err.message();
   }
};

@WebSocketServiceConfig {}
service /websocket on new Listener(21030) {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError  {
       return new ErrorServer();
    }
}

service class ErrorServer {
  *Service;
   remote isolated function onOpen(Caller caller) {
       log:printInfo("The Connection ID: " + caller.getConnectionId());
   }

   remote isolated function onPing(Caller caller, byte[] localData) {
       var returnVal = caller->pong(localData);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote isolated function onPong(Caller caller, byte[] localData) {
       var returnVal = caller->ping(localData);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote isolated function onText(Caller caller, string text, boolean finalFrame) {
       var err = caller->pushText(text, finalFrame);
       if (err is WebSocketError) {
           log:printError("Error occurred when sending text message", err);
       }
   }

   remote isolated function onBinary(Caller caller, byte[] data) {
       var returnVal = caller->pushBinary(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote isolated function onClose(Caller ep, int statusCode, string reason) {
   }
}

// Connection refused IO error.
@test:Config {}
public function testConnectionError() {
   WebSocketClient wsClient = new ("ws://lmnop.ls", config);
   runtime:sleep(500);
   test:assertEquals(errMessage, "ConnectionError: IO Error");
}

// SSL/TLS error
@test:Config {}
public function testSslError() {
   WebSocketClient|error wsClient = new ("wss://localhost:21030/websocket", config);
   runtime:sleep(500);
   test:assertEquals(errMessage, "GenericError: SSL/TLS Error");
}

// The frame exceeds the max frame length
@test:Config {}
public function testLongFrameError() {
   string ping = "pingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingping"
       + "pingpingpingpingpingpingpingpingpingpingpingpingpingping";
   byte[] pingData = ping.toBytes();
   WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
   runtime:sleep(500);
   var err = wsClientEp->ping(pingData);
   if (err is error) {
       test:assertEquals(err.message(), "ProtocolError: io.netty.handler.codec.TooLongFrameException: " +
           "invalid payload for PING (payload length must be <= 125, was 148");
   } else {
       test:assertFail("Mismatched output");
   }
   error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   log:printError("Error occurred when closing connection", result);
   //}
}

// Close the connection and push text
@test:Config {}
public function testConnectionClosedError() {
   WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
   error? result = wsClientEp->close(timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   log:printError("Error occurred when closing connection", result);
   //}
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
   WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", config);
   runtime:sleep(500);
   test:assertEquals(errMessage, "InvalidHandshakeError: Invalid subprotocol. Actual: null. Expected one of: xml");
}

// Tests the ready function using the WebSocket client. When `readyOnConnect` is true,
// calls the `ready()` function.
@test:Config {}
public function testReadyOnConnect() {
   WebSocketClient wsClientEp = new ("ws://localhost:21030/websocket", {callbackService: errorResourceService});
   var err = wsClientEp->ready();
   if (err is error) {
       test:assertEquals(err.message(), "GenericError: Already started reading frames");
   } else {
       test:assertFail("Mismatched output");
   }
}

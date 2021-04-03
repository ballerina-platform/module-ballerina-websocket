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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/io;

string errMessage = "";

ClientConfiguration config = {subProtocols: ["xml"]};

service class errorResourceService {
   remote function onError(Caller clientCaller, Error err) {
       errMessage = <@untainted>err.message();
   }
}

listener Listener l14 = new(21030);
@ServiceConfig {}
service /websocket on l14 {
    resource isolated function get .() returns Service|UpgradeError  {
       return new ErrorServer();
    }
}

service class ErrorServer {
  *Service;
   remote isolated function onOpen(Caller caller) {
       io:println("The Connection ID websocket client exceptions test: " + caller.getConnectionId());
   }

   remote isolated function onPing(Caller caller, byte[] localData) {
       var returnVal = caller->pong(localData);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote isolated function onPong(Caller caller, byte[] localData) {
       var returnVal = caller->ping(localData);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote isolated function onTextMessage(Caller caller, string text) {
       var err = caller->writeTextMessage(text);
       if (err is Error) {
           io:println("Error occurred when sending text message" + err.message());
       }
   }

   remote isolated function onBinaryMessage(Caller caller, byte[] data) {
       var returnVal = caller->writeBinaryMessage(data);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote isolated function onClose(Caller ep, int statusCode, string reason) {
   }
}

// Connection refused IO error.
@test:Config {}
public function testConnectionError() returns Error? {
   Error|Client wsClient = new ("ws://lmnop.ls", config = config);
   if (wsClient is Error) {
       test:assertEquals(wsClient.message(), "ConnectionError: IO Error");
   } else {
       test:assertFail("Expected a connection error to be returned");
   }
}

// // SSL/TLS error
// @test:Config {}
// public function testSslError() returns Error? {
//    AsyncClient wsClient = check new ("wss://localhost:21030/websocket", new errorResourceService(), config);
//    runtime:sleep(0.5);
//    test:assertEquals(errMessage, "GenericError: SSL/TLS Error");
//    error? err = wsClient->close(statusCode = 1000, timeoutInSeconds = 0);
// }

// The frame exceeds the max frame length
@test:Config {}
public function testLongFrameError() returns Error? {
   string ping = "pingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingpingping"
       + "pingpingpingpingpingpingpingpingpingpingpingpingpingping";
   byte[] pingData = ping.toBytes();
   Client wsClientEp = check new ("ws://localhost:21030/websocket");
   runtime:sleep(0.5);
   var err = wsClientEp->ping(pingData);
   if (err is error) {
       test:assertEquals(err.message(), "ProtocolError: io.netty.handler.codec.TooLongFrameException: " +
           "invalid payload for PING (payload length must be <= 125, was 148");
   } else {
       test:assertFail("Mismatched output");
   }
   error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

// Close the connection and push text
@test:Config {}
public function testConnectionClosedError() returns Error? {
   Client wsClientEp = check new ("ws://localhost:21030/websocket");
   error? result = wsClientEp->close(timeout = 0);
   runtime:sleep(2);
   var err = wsClientEp->writeTextMessage("some");
   if (err is error) {
       test:assertEquals(err.message(), "ConnectionClosureError: Close frame already sent. Cannot push text data!");
   } else {
       test:assertFail("Mismatched output");
   }
}

// Handshake failing because of missing subprotocol
@test:Config {}
public function testHandshakeError() returns Error? {
   Error|Client wsClientEp = new ("ws://localhost:21030/websocket", config = config);
   if (wsClientEp is Error) {
      errMessage = wsClientEp.message();
   }
   test:assertEquals(errMessage, "InvalidHandshakeError: Invalid subprotocol. Actual: null. Expected one of: xml");
}

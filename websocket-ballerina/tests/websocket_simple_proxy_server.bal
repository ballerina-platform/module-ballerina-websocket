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

//import ballerina/log;
import ballerina/runtime;
import ballerina/test;

string proxyData = "";

service / on new Listener(21018) {
   resource isolated function onUpgrade .() returns Service|UpgradeError {
       return new ProxyService();
   }
}

service class ProxyService {
  *Service;
  remote function onOpen(Caller wsEp) {
       AsyncClient wsClientEp = new ("ws://localhost:21019/websocket", new clientCallbackService9(), {
               readyOnConnect: false
           });
       var returnVal = wsClientEp->ready();
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onString(Caller wsEp, string text) {
       var returnVal = wsEp->writeString(text);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onBytes(Caller wsEp, byte[] data) {
       var returnVal = wsEp->writeBytes(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onClose(Caller wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

}

service /websocket on new Listener(21019) {
   resource isolated function onUpgrade .() returns Service|UpgradeError {
       return new ProxyService2();
   }
}

service class ProxyService2 {
   *Service;
   remote function onOpen(Caller caller) {
       //log:print("The Connection ID: " + caller.getConnectionId());
   }

   remote function onString(Caller caller, string text, boolean finalFrame) {
       var err = caller->writeString(text, finalFrame);
       if (err is WebSocketError) {
           //log:printError("Error occurred when sending text message", err = err);
       }
   }

   remote function onBytes(Caller caller, byte[] data) {
       var returnVal = caller->writeBytes(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service class clientCallbackService9 {
   remote function onString(AsyncClient wsEp, string text) {
       var returnVal = wsEp->writeString(text);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onBytes(AsyncClient wsEp, byte[] data) {
       var returnVal = wsEp->writeBytes(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onClose(AsyncClient wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service class proxyCallbackService {
   remote function onString(AsyncClient wsEp, string text) {
       proxyData = <@untainted>text;
   }

   remote function onBytes(AsyncClient wsEp, byte[] data) {
       expectedBinaryData = <@untainted>data;
   }

   remote function onClose(AsyncClient wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

// Tests sending and receiving of text frames in WebSockets.
@test:Config {}
public function testSendText() {
   AsyncClient wsClient = new ("ws://localhost:21018", new proxyCallbackService());
   checkpanic wsClient->writeString("Hi kalai");
   runtime:sleep(500);
   test:assertEquals(proxyData, "Hi kalai", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests sending and receiving of binary frames in WebSocket.
@test:Config {}
public function testSendBinary() {
   AsyncClient wsClient = new ("ws://localhost:21018", new proxyCallbackService());
   byte[] binaryData = [5, 24, 56, 243];
   checkpanic wsClient->writeBytes(binaryData);
   runtime:sleep(500);
   test:assertEquals(expectedBinaryData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

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

import ballerina/io;
import ballerina/lang.runtime as runtime;
import ballerina/test;

string proxyData = "";
listener Listener l22 = new(21018);
service / on l22 {
   resource isolated function get .() returns Service|UpgradeError {
       return new ProxyService();
   }
}

service class ProxyService {
  *Service;
  remote function onOpen(Caller wsEp) returns Error? {
       AsyncClient wsClientEp = check new ("ws://localhost:21019/websocket", new clientCallbackService9());
   }

   remote function onTextMessage(Caller wsEp, string text) {
       var returnVal = wsEp->writeTextMessage(text);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote function onBinaryMessage(Caller wsEp, byte[] data) {
       var returnVal = wsEp->writeBinaryMessage(data);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote function onClose(Caller wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

}

listener Listener l26 = new(21019);
service /websocket on l26 {
   resource isolated function get .() returns Service|UpgradeError {
       return new ProxyService2();
   }
}

service class ProxyService2 {
   *Service;
   remote function onOpen(Caller caller) {
       io:println("The Connection ID: " + caller.getConnectionId());
   }

   remote function onTextMessage(Caller caller, string text) {
       var err = caller->writeTextMessage(text);
       if (err is Error) {
           io:println("Error occurred when sending text message: ", err);
       }
   }

   remote function onBinaryMessage(Caller caller, byte[] data) {
       var returnVal = caller->writeBinaryMessage(data);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }
}

service class clientCallbackService9 {
   *Service;
   remote function onTextMessage(Caller wsEp, string text) {
       var returnVal = wsEp->writeTextMessage(text);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote function onBinaryMessage(Caller wsEp, byte[] data) {
       var returnVal = wsEp->writeBinaryMessage(data);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote function onClose(Caller wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }
}

service class proxyCallbackService {
   *Service;
   remote function onTextMessage(Caller wsEp, string text) {
       proxyData = <@untainted>text;
   }

   remote function onBinaryMessage(Caller wsEp, byte[] data) {
       expectedBinaryData = <@untainted>data;
   }

   remote function onClose(Caller wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }
}

// Tests sending and receiving of text frames in WebSockets.
@test:Config {}
public function testSendText() returns Error? {
   AsyncClient wsClient = check new ("ws://localhost:21018", new proxyCallbackService());
   check wsClient->writeTextMessage("Hi kalai");
   runtime:sleep(0.5);
   test:assertEquals(proxyData, "Hi kalai", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests sending and receiving of binary frames in WebSocket.
@test:Config {}
public function testSendBinary() returns Error? {
   AsyncClient wsClient = check new ("ws://localhost:21018", new proxyCallbackService());
   byte[] binaryData = [5, 24, 56, 243];
   check wsClient->writeBinaryMessage(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedBinaryData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

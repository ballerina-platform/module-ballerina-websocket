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

import ballerina/log;
import ballerina/runtime;
import ballerina/test;
import ballerina/http;

final string TRUSTSTORE_PATH = "tests/certsAndKeys/ballerinaTruststore.p12";
final string KEYSTORE_PATH = "tests/certsAndKeys/ballerinaKeystore.p12";

service /sslEcho on new Listener(21027, {
       secureSocket: {
           keyStore: {
               path: KEYSTORE_PATH,
               password: "ballerina"
           }
       }
   }) {
   resource isolated function onUpgrade .(http:Caller caller, http:Request req) returns Service|UpgradeError {
       return new SslProxy();
   }
}
service class SslProxy {
   *Service;
   remote function onOpen(Caller wsEp) {
       AsyncClient wsClientEp = new ("wss://localhost:21028/websocket", new sslClientService(), {
               secureSocket: {
                   trustStore: {
                       path: TRUSTSTORE_PATH,
                       password: "ballerina"
                   }
               },
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
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service class sslClientService {
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
       var returnVal = wsEp->close(statusCode, reason);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service /websocket on new Listener(21028, {
       secureSocket: {
           keyStore: {
               path: KEYSTORE_PATH,
               password: "ballerina"
           }
       }
   }) {
   resource isolated function onUpgrade .(http:Caller caller, http:Request req) returns Service|UpgradeError {
       return new SslProxyServer();
   }
}

service class SslProxyServer {
   *Service;
   remote function onOpen(Caller caller) {
       log:print("The Connection ID: " + caller.getConnectionId());
   }

   remote function onString(Caller caller, string text, boolean finalFrame) {
       var err = caller->writeString(text, finalFrame);
       if (err is WebSocketError) {
           log:printError("Error occurred when sending text message", err = err);
       }
   }

   remote function onBytes(Caller caller, byte[] data) {
       var returnVal = caller->writeBytes(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service class sslProxyCallbackService {
   remote function onString(AsyncClient wsEp, string text) {
       proxyData = <@untainted>text;
   }

   remote function onBytes(AsyncClient wsEp, byte[] data) {
       expectedBinaryData = <@untainted>data;
   }

   remote function onClose(AsyncClient wsEp, int statusCode, string reason) {
       var returnVal = wsEp->close(statusCode = statusCode, reason = reason);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

// Tests sending and receiving of text frames in WebSockets.
@test:Config {}
public function testSslProxySendText() {
   AsyncClient wsClient = new ("wss://localhost:21027/sslEcho", new sslProxyCallbackService(), {
           secureSocket: {
               trustStore: {
                   path: TRUSTSTORE_PATH,
                   password: "ballerina"
               }
           }
       });
   checkpanic wsClient->writeString("Hi");
   runtime:sleep(500);
   test:assertEquals(proxyData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests sending and receiving of binary frames in WebSocket.
@test:Config {}
public function testSslProxySendBinary() {
   AsyncClient wsClient = new ("wss://localhost:21027/sslEcho", new sslProxyCallbackService(), {
           secureSocket: {
               trustStore: {
                   path: TRUSTSTORE_PATH,
                   password: "ballerina"
               }
           }
       });
   byte[] binaryData = [5, 24, 56];
   checkpanic wsClient->writeBytes(binaryData);
   runtime:sleep(500);
   test:assertEquals(expectedBinaryData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

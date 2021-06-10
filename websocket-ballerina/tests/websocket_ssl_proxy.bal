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

final string TRUSTSTORE_PATH = "tests/certsAndKeys/ballerinaTruststore.p12";
final string KEYSTORE_PATH = "tests/certsAndKeys/ballerinaKeystore.p12";

listener Listener l24 = new(21027, {
    secureSocket: {
        key: {
            path: KEYSTORE_PATH,
            password: "ballerina"
        }
    }
});
service /sslEcho on l24 {
   resource isolated function get .() returns Service|UpgradeError {
       return new SslProxy();
   }
}
service class SslProxy {
   *Service;
   remote function onOpen(Caller wsEp) returns Error? {
       Client wsClientEp = check new ("wss://localhost:21028/websocket", {
           secureSocket: {
               cert: {
                   path: TRUSTSTORE_PATH,
                   password: "ballerina"
               }
           }
       });
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
   }
}

listener Listener l27 = new(21028, {
    secureSocket: {
        key: {
            path: KEYSTORE_PATH,
            password: "ballerina"
        }
    }
});
service /websocket on l27 {
   resource isolated function get .() returns Service|UpgradeError {
       return new SslProxyServer();
   }
}

service class SslProxyServer {
   *Service;
   remote function onOpen(Caller caller) {
       io:println("The Connection ID ssl proxy server: " + caller.getConnectionId());
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

// Tests sending and receiving of text frames in WebSockets over SSL.
@test:Config {}
public function testSslProxySendText() returns Error? {
   Client wsClient = check new ("wss://localhost:21027/sslEcho", {
       secureSocket: {
           cert: {
               path: TRUSTSTORE_PATH,
               password: "ballerina"
           }
       }
   });
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(0.5);
   string sslProxyData = check wsClient->readTextMessage();
   test:assertEquals(sslProxyData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

// Tests sending and receiving of binary frames in WebSocket over SSL.
@test:Config {}
public function testSslProxySendBinary() returns Error? {
   Client wsClient = check new ("wss://localhost:21027/sslEcho", {
       secureSocket: {
           cert: {
               path: TRUSTSTORE_PATH,
               password: "ballerina"
           }
       }
   });
   byte[] binaryData = [5, 24, 56];
   check wsClient->writeBinaryMessage(binaryData);
   runtime:sleep(0.5);
   byte[] sslProxyBinData = check wsClient->readBinaryMessage();
   test:assertEquals(sslProxyBinData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

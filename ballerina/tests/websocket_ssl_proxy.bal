// // Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/io;
// import ballerina/lang.runtime as runtime;
// import ballerina/test;

// listener Listener l24 = new(21027, {
//     secureSocket: {
//         key: {
//             path: KEYSTORE_PATH,
//             password: "ballerina"
//         }
//     }
// });
// service /sslEcho on l24 {
//    resource isolated function get .() returns Service|UpgradeError {
//        return new SslProxy();
//    }
// }
// service class SslProxy {
//    *Service;
//    Client? wsClientEp = ();
//    remote function onOpen(Caller wsEp) returns Error? {
//        self.wsClientEp = check new ("wss://localhost:21028/websocket", {
//            secureSocket: {
//                cert: {
//                    path: TRUSTSTORE_PATH,
//                    password: "ballerina"
//                }
//            }
//        });
//    }

//    remote function onTextMessage(Caller wsEp, string text) returns Error? {
//        Client? proxyClient = self.wsClientEp;
//        if proxyClient is Client {
//            check proxyClient->writeTextMessage(text);
//            string proxyData = check proxyClient->readTextMessage();
//            check wsEp->writeTextMessage(proxyData);
//        }
//    }

//    remote function onBinaryMessage(Caller wsEp, byte[] data) returns Error? {
//        Client? proxyClient = self.wsClientEp;
//        if proxyClient is Client {
//            check proxyClient->writeBinaryMessage(data);
//            byte[] proxyData = check proxyClient->readBinaryMessage();
//            check wsEp->writeBinaryMessage(proxyData);
//        }
//    }

//    remote function onClose(Caller wsEp, int statusCode, string reason) {
//    }
// }

// listener Listener l27 = new(21028, {
//     secureSocket: {
//         key: {
//             path: KEYSTORE_PATH,
//             password: "ballerina"
//         }
//     }
// });
// service /websocket on l27 {
//    resource isolated function get .() returns Service|UpgradeError {
//        return new SslProxyServer();
//    }
// }

// service class SslProxyServer {
//    *Service;
//    remote function onOpen(Caller caller) {
//        io:println("The Connection ID ssl proxy server: " + caller.getConnectionId());
//    }

//    remote function onTextMessage(Caller caller, string text) {
//        Error? err = caller->writeTextMessage(text);
//        if err is Error {
//            io:println("Error occurred when sending text message: ", err);
//        }
//    }

//    remote function onBinaryMessage(Caller caller, byte[] data) returns error? {
//        check caller->writeBinaryMessage(data);
//    }
// }

// // Tests sending and receiving of text frames in WebSockets over SSL.
// @test:Config {}
// public function testSslProxySendText() returns Error? {
//    Client wsClient = check new ("wss://localhost:21027/sslEcho", {
//        secureSocket: {
//            cert: {
//                path: TRUSTSTORE_PATH,
//                password: "ballerina"
//            }
//        }
//    });
//    runtime:sleep(1);
//    check wsClient->writeTextMessage("Hi");
//    runtime:sleep(0.5);
//    string sslProxyData = check wsClient->readTextMessage();
//    test:assertEquals(sslProxyData, "Hi", msg = "Data mismatched");
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

// // Tests sending and receiving of binary frames in WebSocket over SSL.
// @test:Config {}
// public function testSslProxySendBinary() returns Error? {
//    Client wsClient = check new ("wss://localhost:21027/sslEcho", {
//        secureSocket: {
//            cert: {
//                path: TRUSTSTORE_PATH,
//                password: "ballerina"
//            }
//        }
//    });
//    byte[] binaryData = [5, 24, 56];
//    runtime:sleep(1);
//    check wsClient->writeBinaryMessage(binaryData);
//    runtime:sleep(0.5);
//    byte[] sslProxyBinData = check wsClient->readBinaryMessage();
//    test:assertEquals(sslProxyBinData, binaryData, msg = "Data mismatched");
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

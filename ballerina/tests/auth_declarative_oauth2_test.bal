// // Copyright (c) 2021 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // //www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/lang.runtime as runtime;
// import ballerina/test;

// listener Listener l55 = new(21325);
// string wsService55Data = "";

// @ServiceConfig {
//     auth: [
//         {
//             oauth2IntrospectionConfig: {
//                 url: "https://localhost:9445/oauth2/introspect",
//                 tokenTypeHint: "access_token",
//                 scopeKey: "scp",
//                 clientConfig: {
//                     secureSocket: {
//                        cert: {
//                            path: TRUSTSTORE_PATH,
//                            password: "ballerina"
//                        }
//                     }
//                 }
//             },
//             scopes: ["write", "update"]
//         }
//     ]
// }
// service /oauthService on l55 {
//     resource function get .() returns Service {
//         return new WsService55();
//     }
// }

// service class WsService55 {
//     *Service;
//     remote function onTextMessage(string data) {
//         wsService55Data = data;
//     }
// }

// @test:Config {
//     before: clear
// }
// public function testOAuth2ClientCredentialsGrantAuthSuccess() returns Error? {
//     Client wsClient = check new("ws://localhost:21325/oauthService/", {
//         auth: {
//             tokenUrl: "https://localhost:9445/oauth2/token",
//             clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
//             clientSecret: "9205371918321623741",
//             scopes: ["write", "update"],
//             clientConfig: {
//                 secureSocket: {
//                    cert: {
//                        path: TRUSTSTORE_PATH,
//                        password: "ballerina"
//                    }
//                 }
//             }
//         }
//     });
//     check wsClient->writeTextMessage("Hello, World!");
//     runtime:sleep(0.5);
//     test:assertEquals(wsService55Data, "Hello, World!");
//     error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

// @test:Config {
//     before: clear
// }
// public function testOAuth2PasswordGrantAuthSuccess() returns Error? {
//     Client wsClient = check new("ws://localhost:21325/oauthService/", {
//         auth: {
//             tokenUrl: "https://localhost:9445/oauth2/token",
//             username: "johndoe",
//             password: "A3ddj3w",
//             clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
//             clientSecret: "9205371918321623741",
//             scopes: ["write", "update"],
//             clientConfig: {
//                 secureSocket: {
//                    cert: {
//                        path: TRUSTSTORE_PATH,
//                        password: "ballerina"
//                    }
//                 }
//             }
//         }
//     });
//     check wsClient->writeTextMessage("Hello, World!");
//     runtime:sleep(0.5);
//     test:assertEquals(wsService55Data, "Hello, World!");
//     error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

// @test:Config {
//     before: clear
// }
// public function testOAuth2RefreshTokenGrantAuthSuccess() returns Error? {
//     Client wsClient = check new("ws://localhost:21325/oauthService/", {
//         auth: {
//             refreshUrl: "https://localhost:9445/oauth2/token",
//             refreshToken: "XlfBs91yquexJqDaKEMzVg==",
//             clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
//             clientSecret: "9205371918321623741",
//             scopes: ["write", "update"],
//             clientConfig: {
//                 secureSocket: {
//                    cert: {
//                        path: TRUSTSTORE_PATH,
//                        password: "ballerina"
//                    }
//                 }
//             }
//         }
//     });
//     check wsClient->writeTextMessage("Hello, World!");
//     runtime:sleep(0.5);
//     test:assertEquals(wsService55Data, "Hello, World!");
//     error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

// function clear() {
//     wsService55Data = "";
// }

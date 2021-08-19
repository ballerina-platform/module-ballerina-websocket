// // Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// import ballerina/test;
// import ballerina/http;
// import ballerina/io;
// import ballerina/lang.'string as strings;

// listener Listener l68 = new(21068, {
//     secureSocket: {
//         key: {
//             path: KEYSTORE_PATH,
//             password: "ballerina"
//         },
//         protocol: {
//             name: http:TLS,
//             versions: ["TLSv1.2"]
//         }
//     },
//     host: "localhost"
// });

// service /sslTest on l68 {
//     resource function get .() returns Service {
//         return new SslService4();
//     }
// }

// service class SslService4 {
//     *Service;
//     remote isolated function onTextMessage(Caller caller, string data) {
//         var returnVal = caller->writeTextMessage(data);
//         if (returnVal is Error) {
//             panic <error>returnVal;
//         }
//     }
// }

// // Tests the successful connection of sync client over mutual SSL with certs and keys
// @test:Config {}
// public function testSslProtocolError() returns Error? {
//     Client|Error wsClient = new("wss://localhost:21068/sslTest", {
//         secureSocket: {
//             cert: {
//                 path: TRUSTSTORE_PATH,
//                 password: "ballerina"
//             },
//             protocol: {
//                 name: http:TLS,
//                 versions: ["TLSv1.3"]
//             }
//         }
//     });
//     if (wsClient is Error) {
//         io:println(wsClient.message());
//         test:assertTrue(strings:includes(wsClient.message(), "Received fatal alert"));
//     } else {
//         test:assertFail(msg = "Found unexpected output: Expected an error" );
//     }
// }

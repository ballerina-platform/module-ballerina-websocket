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

// import ballerina/lang.runtime as runtime;
// import ballerina/test;
// import ballerina/http;
// import ballerina/io;

// string sslString = "";
// listener Listener l37 = new(21059, {
//     secureSocket: {
//         key: {
//             path: KEYSTORE_PATH,
//             password: "ballerina"
//         }
//     }
// });

// service /sslTest on l37 {
//     resource function get .(http:Request req) returns Service {
//         return new SyncSslService();
//     }
// }

// service class SyncSslService {
//     *Service;
//     remote isolated function onTextMessage(Caller caller, string data) {
//         var returnVal = caller->writeTextMessage(data);
//         if returnVal is Error {
//             panic <error>returnVal;
//         }
//     }
// }

// // Tests the successful connection of sync client over SSL
// @test:Config {}
// public function testSyncClientSsl() returns Error? {
//     Client wsClient = check new("wss://localhost:21059/sslTest", {
//         secureSocket: {
//             cert: {
//                 path: TRUSTSTORE_PATH,
//                 password: "ballerina"
//             }
//         }
//     });
//     wsClient.setAttribute("test", "testSyncClientSsl");
//     @strand {
//         thread:"any"
//     }
//     worker w1 {
//         io:println("Reading message starting: sync ssl client");

//         string|Error resp1 = wsClient->readTextMessage();
//         if resp1 is Error {
//             io:println("Error creating client");
//             sslString = resp1.message();
//         } else {
//             sslString = resp1;
//             io:println("1st response received at sync Ssl client :" + resp1);
//         }
//     }
//     @strand {
//         thread:"any"
//     }
//     worker w2 {
//         io:println("Waiting till SSL client starts reading text.");
//         runtime:sleep(2);
//         var resp1 = wsClient->writeTextMessage("Hi world1");
//         string removedAttr = <string> wsClient.removeAttribute("test");
//         test:assertEquals(removedAttr, "testSyncClientSsl");
//         if resp1 is Error {
//             io:println("Error occured when sending the text to ssl server");
//         } else {
//             io:println("Succesfully sent frame to ssl service");
//         }
//         runtime:sleep(2);
//     }
//     _ = wait {w1, w2};
//     string msg = "Hi world1";
//     test:assertEquals(sslString, msg);
//     test:assertEquals(wsClient.isSecure(), true);
// }

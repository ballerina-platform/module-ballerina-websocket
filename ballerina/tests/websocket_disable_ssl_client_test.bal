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
// import ballerina/io;

// listener Listener l67 = new(21067, {
//     secureSocket: {
//         key: {
//             certFile: "tests/certsAndKeys/public.crt",
//             keyFile: "tests/certsAndKeys/private.key"
//         }
//     }
// });

// service /sslTest on l67 {
//     resource function get .() returns Service {
//         return new SslService3();
//     }
// }

// service class SslService3 {
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
// public function testDisabledSsl() returns Error? {
//     Client|Error wsClient = new("wss://localhost:21067/sslTest", {
//         secureSocket: {
//             enable: false
//         }
//     });
//     if (wsClient is Error) {
//         io:println(wsClient.message());
//         test:assertFail("Expected a successful TLS connection");
//     } else {
//         test:assertEquals(wsClient.isSecure(), true);
//     }
// }

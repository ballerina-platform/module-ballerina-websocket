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

// import ballerina/test;
// import ballerina/io;
// import ballerina/lang.runtime as runtime;

// string idleTimeOutError = "";
// string secondReadResp = "";
// listener Listener l34 = new(21056);
// service /onIdleTimeoutService on l34 {
//     resource function get .() returns Service|UpgradeError {
//         return new OnIdleTimeoutService();
//     }
// }

// service class OnIdleTimeoutService {
//     *Service;
//     remote isolated function onTextMessage(Caller caller, string data) returns Error? {
//         check caller->writeTextMessage(data);
//     }

//     remote isolated function onClose(Caller caller, string data) returns Error? {
//         check caller->writeTextMessage(data);
//     }
// }

// // Tests the idle timeout error returned from readTextMessage and then read again
// // to check if the idle state handler gets reset.
// @test:Config {}
// public function testSyncIdleTimeOutError() returns Error? {
//     Client wsClient = check new("ws://localhost:21056/onIdleTimeoutService", config = {readTimeout: 2});
//     @strand {
//         thread:"any"
//     }
//     worker w1 {
//         io:println("Reading message starting: sync idle timeout client");

//         string|Error resp1 = wsClient->readTextMessage();
//         if resp1 is Error {
//             idleTimeOutError = resp1.message();
//         } else {
//             io:println("1st response received at sync idle timeout client :" + resp1);
//         }
//         string|Error resp2 = wsClient->readTextMessage();
//         if resp2 is Error {
//             idleTimeOutError = resp2.message();
//         } else {
//             secondReadResp = resp2;
//         }
//     }
//     @strand {
//         thread:"any"
//     }
//     worker w2 {
//         io:println("Waiting till idle timeout client starts reading text.");
//         runtime:sleep(3);
//         Error? resp1 = wsClient->writeTextMessage("Hi world1");
//         runtime:sleep(2);
//     }
//     _ = wait {w1, w2};
//     string msg = "Read timed out";
//     test:assertEquals(idleTimeOutError, msg);
//     test:assertEquals(secondReadResp, "Hi world1");
// }

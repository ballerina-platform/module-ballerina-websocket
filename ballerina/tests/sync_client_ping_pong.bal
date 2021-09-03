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

// string pingPongMsg = "";
// listener Listener l35 = new(21057);
// service /pingpong on l35 {
//     resource function get .() returns Service|UpgradeError {
//         return new WsServiceSyncPingPong();
//     }
// }

// service class WsServiceSyncPingPong {
//     *Service;
//     remote isolated function onTextMessage(Caller caller, string data) returns Error? {
//         io:println("On ping pong server text");
//         byte[] pingData = [5, 24, 56, 243];
//         check caller->ping(pingData);
//     }

//     remote isolated function onPing(Caller caller, byte[] localData) returns byte[] {
//         io:println("On server ping");
//         return localData;
//     }

//     remote isolated function onPong(Caller caller, byte[] localData) {
//         io:println("On server pong");
//         var returnVal = caller->writeTextMessage("pong received");
//         if (returnVal is Error) {
//             panic <error>returnVal;
//         }
//     }

//     remote isolated function onClose(Caller caller, string data) returns Error? {
//         check caller->writeTextMessage(data);
//     }
// }

// service isolated class clientPingPongCallbackService {
//     *PingPongService;
//     remote isolated function onPing(Caller caller, byte[] localData) returns byte[] {
//         io:println("On sync client ping");
//         return localData;
//     }

//     remote isolated function onPong(Caller caller, byte[] localData) {
//         io:println("On sync client pong");
//         var returnVal = caller->writeTextMessage("pong received");
//         if (returnVal is Error) {
//            panic <error>returnVal;
//         }
//     }
// }

// // Tests the receiving of ping messages asynchronously in WebSocket synchronous client.
// // Ping messages are dispatched to the registered callback service.
// @test:Config {}
// public function testSyncClientPingPong() returns Error? {
//     Client wsClient = check new("ws://localhost:21057/pingpong", config = {pingPongHandler : new clientPingPongCallbackService()});
//     @strand {
//         thread:"any"
//     }
//     worker w1 {
//         io:println("Reading message starting: sync ping pong client");

//         string|Error resp1 = wsClient->readTextMessage();
//         if (resp1 is Error) {
//             pingPongMsg = resp1.message();
//         } else {
//             pingPongMsg = resp1;
//         }
//     }
//     @strand {
//         thread:"any"
//     }
//     worker w2 {
//         io:println("Waiting till ping pong client starts reading text.");
//         runtime:sleep(2);
//         Error? resp1 = wsClient->writeTextMessage("Hi world1");
//         runtime:sleep(2);
//     }
//     _ = wait {w1, w2};
//     string msg = "pong received";
//     test:assertEquals(pingPongMsg, msg, msg = "");
// }

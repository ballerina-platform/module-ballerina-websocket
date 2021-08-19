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

// listener Listener l74 = new(21074);

// service /onDispatchError on l74 {
//     resource function get .() returns Service|UpgradeError {
//         return new WsService74();
//     }
// }

// service class WsService74 {
//     *Service;
//     remote function onTextMessage(Caller caller, string data) returns error? {
//         return error("error returned");
//     }
// }

// // Tests error returned from remote function.
// @test:Config {}
// public function testReturningError() returns Error? {
//     Client wsClient = check new ("ws://localhost:21074/onDispatchError/");
//     check wsClient->writeTextMessage("Hi");
//     error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

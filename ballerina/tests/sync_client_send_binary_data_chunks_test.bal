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

// byte[] BinSyncData = [];

// listener Listener l41 = new(21311);

// service /onBinaryDataSync on l41 {
//    resource function get .() returns Service|UpgradeError {
//        return new WsService41();
//    }
// }

// service class WsService41 {
//   *Service;
//   remote function onBinaryMessage(Caller caller, byte[] data) returns Error? {
//       BinSyncData = data;
//   }
// }

// // Tests writing binary data as continuation frames chunked by the given maxFrameSize using the sync client.
// @test:Config {}
// public function testChunkBinaryDataSync() returns Error? {
//    Client wsClient = check new("ws://localhost:21311/onBinaryDataSync/", config = {maxFrameSize: 1});
//    byte[] binaryData = [5, 24, 56];
//    check wsClient->writeBinaryMessage(binaryData);
//    runtime:sleep(5);
//    test:assertEquals(BinSyncData, binaryData, msg = "Failed testChunkBinaryDataSync");
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
// }

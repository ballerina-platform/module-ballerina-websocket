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
// import ballerina/io;

// listener Listener l78 = new(21078);

// @ServiceConfig {
//     maxFrameSize: 2147483647
// }
// service /onTextString on l78 {
//    resource function get .() returns Service|UpgradeError {
//        //runtime:sleep(2);
//        return new WsService78();
//    }
// }

// service class WsService78 {
//   *Service;
//   remote isolated function onTextMessage(Caller caller, string data) returns string? {
//       io:println(data);
//       return data;
//   }

//   remote isolated function onBinaryMessage(Caller caller, byte[] data) returns string? {
//        return "Hello";
//   }

//   remote isolated function onError(error err) returns Error? {
//       io:println("server on error message");
//   }
// }

// // Tests client handshake timeout
// @test:Config {}
// public function testWriteTimeoutError() returns error? {
//    string imagePath = "/Users/bhashinee/Documents/img2.jpeg";
//    byte[] bytes = check io:fileReadBytes(imagePath);
//    Client wsClient = check new("ws://localhost:21078/onTextString", config = {maxFrameSize: 2147483647, writeTimeout: 1});
//    Error? err = wsClient->writeBinaryMessage(bytes);
//    if err is Error {
//        io:println("Errorrrrrr");
//        io:println(err);
//    }
//    runtime:sleep(2);
//    Error? err2 = wsClient->writeTextMessage("2nd Message");
//    if err2 is Error {
//        io:println("Errorrrrrr2222222222");
//        io:println(err2);
//    }
// }

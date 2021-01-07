// // Copyright (c) 2020 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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
// import ballerina/http;
// import ballerina/io;
// import ballerina/runtime;

// service /onTextString on new Listener(21000) {
//    resource function onUpgrade .(http:Caller caller, http:Request req) returns Service|UpgradeError {
//        io:println("on upgrade");
//        return new WsServiceSync();
//    }
// }

// service class WsServiceSync {
//   *Service;
//   remote isolated function onString(Caller caller, string data, boolean finalFrame) {
//       io:println("onString");
//       checkpanic caller->writeString(data);
//   }

//   remote isolated function onClose(Caller caller, string data, boolean finalFrame) {
//         io:println("onClose");
//         checkpanic caller->writeString(data);
//   }
// }

// @test:Config {}
// public function testSyncClient() {
//    SyncClient wsClient = new("ws://localhost:21000/onTextString");
//     @strand {
//            thread:"any"
//        }
//    worker w1 {
//       io:println("reading message");
//       var resp2 = wsClient->readString();
//       if (resp2 is string) {
//         io:println("Response received" + resp2);
//       } else {
//         io:println("error");
//       }
//       //runtime:sleep(3000);
//       var resp4 = wsClient->readString();
//         if (resp4 is string) {
//           io:println("Response received" + resp4);
//         } else {
//           io:println("error");
//         }
//         var resp5 = wsClient->readString();
//         if (resp5 is string) {
//           io:println("Response received" + resp5);
//         } else {
//           io:println("error");
//         }
//         runtime:sleep(3000);
//         var resp6 = wsClient->readString();
//         if (resp6 is string) {
//           io:println("Response received" + resp6);
//         } else {
//           io:println("error");
//         }
//         var resp7 = wsClient->readString();
//         if (resp7 is string) {
//           io:println("Response received" + resp7);
//         } else {
//           io:println("error");
//         }
//    }
//     @strand {
//            thread:"any"
//        }
//    worker w2 {
//        io:println("before sleep");
//        runtime:sleep(2000);
//        io:println("sending Hi");
//        var resp3 = wsClient->writeString("Hi world");
//        runtime:sleep(2000);
//        var resp5 = wsClient->writeString("Hi world2");
//        runtime:sleep(2000);
//        var resp6 = wsClient->writeString("Hi world3");
//        var resp7 = wsClient->writeString("Hi world4");
//        var resp8 = wsClient->writeString("Hi world5");
//    }
//    runtime:sleep(3000);
// }
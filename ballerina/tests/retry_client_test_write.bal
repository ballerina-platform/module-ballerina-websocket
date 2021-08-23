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

// import ballerina/lang.runtime as runtime;
// import ballerina/test;
// import ballerina/io;
// //import ballerina/jballerina.java;

// // Tests string read
// @test:Config {}
// public function testClientRetryWrite() returns error? {
//    @strand {
//        thread:"any"
//    }
//    worker w1 returns error? {
//        startRemoteServer();
//        Client wsClient = check new("ws://localhost:21003/websocket", { retryConfig: {maxCount: 10} });
//        string firstResp = check wsClient->readTextMessage();
//        io:println(firstResp);
//        stopRemoteServer();
//        Error? wResult = wsClient->writeTextMessage("Hi");
//        if (wResult is Error) {
//            io:println("Error--------------");
//            io:println(wResult);
//        } else {
//            io:println("---------Sent message--------------");
//        }
//        Error? wResult2 = wsClient->writeTextMessage("Hello");
//        if (wResult2 is Error) {
//            io:println("Error2--------------");
//            io:println(wResult2);
//            io:println("Error2--------------");
//        } else {
//            io:println("---------Sent 2nd message--------------");
//        }
//    }
//    @strand {
//        thread:"any"
//    }
//    worker w2 returns error? {
//       runtime:sleep(10);
//       startRemoteServer();
//       runtime:sleep(10);
//    }
//    var waitResp = wait {w1, w2};
// }

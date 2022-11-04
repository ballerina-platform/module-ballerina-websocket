// // Copyright (c) 2022 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
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

// public type ChatMessage record {|
//     string name = "";
//     string message = "";
// |};

// listener Listener streamLis = new(21402);

// service /onStream on streamLis {
//     resource function get .() returns Service|UpgradeError {
//         return new StreamStringSvc();
//     }
// }

// service class StreamStringSvc {
//     *Service;
//     remote isolated function onMessage(Caller caller, json data) returns stream<string>|error {
//       string[] greets = ["Hi Sam", "Hey Sam", "GM Sam"];
//       return greets.toStream();
//     }
// }

// service /onRecordStream on streamLis {
//     resource function get .() returns Service|UpgradeError {
//        return new StreamRecordSvc();
//     }
// }

// service class StreamRecordSvc {
//     *Service;
//     remote isolated function onMessage(Caller caller, json data) returns stream<ChatMessage>|error {
//         ChatMessage mes1 = {name:"Sam", message:"Hi"};
//         ChatMessage mes2 = {name:"Sam", message:"GM"};
//         ChatMessage[] chatMsges = [mes1, mes2];
//         return chatMsges.toStream();
//     }
// }

// class EvenNumberGenerator {
//     int i = 0;
//     public isolated function next() returns record {| int value; |}|error? {
//         self.i += 2;
//         return { value: self.i };
//     }
// }

// service /onIntStreamWithError on streamLis {
//     resource function get .() returns Service|UpgradeError {
//        return new StreamIntWithErrorSvc();
//     }
// }

// service class StreamIntWithErrorSvc {
//     *Service;
//     remote isolated function onMessage(Caller caller, json data) returns stream<int, error?>|error {
//        EvenNumberGenerator evenGen = new();
//        stream<int, error?> evenNumberStream = new(evenGen);
//        return evenNumberStream;
//     }
// }

// class ErrorGenerator {
//     int i = 0;
//     public isolated function next() returns record {| int value; |}|error? {
//         return error("panic from next");
//     }
// }

// service /onErrorStream on streamLis {
//     resource function get .() returns Service|UpgradeError {
//        return new StreamErrorSvc();
//     }
// }

// service class StreamErrorSvc {
//     *Service;
//     remote function onMessage(Caller caller, json data) returns stream<int, error?>|error {
//        ErrorGenerator errGen = new();
//        stream<int, error?> errNumberStream = new(errGen);
//        return errNumberStream;
//     }
// }

// @test:Config {}
// public function testStreamString() returns Error? {
//     Client wsClient = check new("ws://localhost:21402/onStream/");
//     string[] greets = ["Hi", "Hey", "GM"];
//     check wsClient->writeMessage(greets);
//     string data = check wsClient->readTextMessage();
//     test:assertEquals(data, "Hi Sam");
//     string data2 = check wsClient->readTextMessage();
//     test:assertEquals(data2, "Hey Sam");
//     string data3 = check wsClient->readTextMessage();
//     test:assertEquals(data3, "GM Sam");
// }

// @test:Config {}
// public function testRecord() returns Error? {
//     Client wsClient = check new("ws://localhost:21402/onRecordStream/");
//     string[] greets = ["Hi", "Hey", "GM"];
//     check wsClient->writeMessage(greets);
//     ChatMessage data = check wsClient->readMessage();
//     test:assertEquals(data, {name:"Sam", message:"Hi"});
//     ChatMessage data2 = check wsClient->readMessage();
//     test:assertEquals(data2, {name:"Sam", message:"GM"});
// }

// @test:Config {}
// public function testIntWithError() returns Error? {
//     Client wsClient = check new("ws://localhost:21402/onIntStreamWithError/");
//     string[] greets = ["Hi", "Hey", "GM"];
//     check wsClient->writeMessage(greets);
//     json data = check wsClient->readMessage();
//     test:assertEquals(data, 2);
//     json data2 = check wsClient->readMessage();
//     test:assertEquals(data2, 4);
// }

// @test:Config {}
// public function testError() returns Error? {
//     Client wsClient = check new("ws://localhost:21402/onErrorStream/");
//     string[] greets = ["Hi", "Hey", "GM"];
//     check wsClient->writeMessage(greets);
//     string data = check wsClient->readMessage();
//     test:assertEquals(data, "panic from next");
// }




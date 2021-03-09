// Copyright (c) 2020 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// //www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/http;

string expectedData = "";
byte[] binData = [];
byte[] expectedPingBinaryData = [];
listener Listener l17 = new(21005);

@ServiceConfig {
   idleTimeout: 10
}
service /onlyOnBinary on l17 {
   resource isolated function get .(http:Request req) returns Service|UpgradeError {
       return new OnlyOnBinary();
   }
}

service class OnlyOnBinary {
  *Service;
   remote function onBinaryMessage(Caller caller, byte[] data) returns Error? {
       binData = <@untainted>data;
   }
}

listener Listener l25 = new(21006);

service /onlyOnText on l25 {
   resource isolated function get .(http:Request req) returns Service|UpgradeError {
       return new OnlyOnText();
   }
}

service class OnlyOnText {
   *Service;
   remote function onTextMessage(Caller caller, string data) returns Error? {
       expectedData = <@untainted>data;
   }
}

service class callbackService {
   *Service;
   remote function onTextMessage(Caller wsEp, string text) {
       expectedData = <@untainted>text;
   }

   remote function onBinaryMessage(Caller wsEp, byte[] data) {
       binData = <@untainted>data;
   }

   remote function onPing(Caller wsEp, byte[] data) {
       expectedPingBinaryData = <@untainted>data;
   }
}

// Tests behavior when onTextMessage resource is missing and a text message is received
@test:Config {}
public function testMissingOnText() returns Error? {
   Client wsClient = check new ("ws://localhost:21005/onlyOnBinary");
   expectedData = "";
   byte[] binaryData = [5, 24, 56, 243];
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "", msg = "Data mismatched");
   check wsClient->writeBinaryMessage(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(binData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(timeout = 0);
}

// Tests behavior when onPong resource is missing and a pong is received
@test:Config {}
public function testMissingOnPong() returns Error? {
   Client wsClient = check new ("ws://localhost:21005/onlyOnBinary");
   byte[] binaryData = [5, 24, 56, 243];
   binData = [];
   check wsClient->pong(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedPingBinaryData, binData, msg = "Data mismatched");
   check wsClient->writeBinaryMessage(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(binData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(timeout = 0);
}

// Tests behavior when onBinaryMessage resource is missing and binary message is received
@test:Config {}
public function testMissingOnBinary() returns Error? {
   Client wsClient = check new ("ws://localhost:21006/onlyOnText");
   byte[] binaryData = [5, 24, 56, 243];
   binData = [];
   byte[] expBinData = [];
   check wsClient->writeBinaryMessage(binaryData);
   runtime:sleep(0.5);
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(timeout = 0);
}

// Tests behavior when onBinaryMessage resource is missing and binary message is received
@test:Config {}
public function testMissingOnIdleTimeout() returns Error? {
   Client wsClient = check new ("ws://localhost:21006/onlyOnText");
   runtime:sleep(0.5);
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

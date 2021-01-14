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
byte[] expectedBinData = [];
byte[] expectedPingBinaryData = [];
listener Listener l17 = check new(21005);
@ServiceConfig {
   idleTimeoutInSeconds: 10
}
service /onlyOnBinary on l17 {
   resource isolated function get .(http:Request req) returns Service|UpgradeError {
       return new OnlyOnBinary();
   }
}
service class OnlyOnBinary {
  *Service;
   remote function onBytes(Caller caller, byte[] data) returns Error? {
       check caller->writeBytes(data);
   }
}

listener Listener l25 = check new(21006);
service /onlyOnText on l25 {
   resource isolated function get .(http:Request req) returns Service|UpgradeError {
       return new OnlyOnText();
   }
}

service class OnlyOnText {
   *Service;
   remote function onString(Caller caller, string data) returns Error? {
       check caller->writeString(data);
   }
}

service class callbackService {
   *Service;
   remote function onString(Caller wsEp, string text) {
       expectedData = <@untainted>text;
   }

   remote function onBytes(Caller wsEp, byte[] data) {
       expectedBinData = <@untainted>data;
   }

   remote function onPing(Caller wsEp, byte[] data) {
       expectedPingBinaryData = <@untainted>data;
   }
}

// Tests behavior when onString resource is missing and a text message is received
@test:Config {}
public function testMissingOnText() returns Error? {
   AsyncClient wsClient = new ("ws://localhost:21005/onlyOnBinary", new callbackService());
   expectedData = "";
   byte[] binaryData = [5, 24, 56, 243];
   check wsClient->writeString("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "", msg = "Data mismatched");
   check wsClient->writeBytes(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedBinData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(timeoutInSeconds = 0);
}

// Tests behavior when onPong resource is missing and a pong is received
@test:Config {}
public function testMissingOnPong() returns Error? {
   AsyncClient wsClient = new ("ws://localhost:21005/onlyOnBinary", new callbackService());
   byte[] binaryData = [5, 24, 56, 243];
   expectedBinData = [];
   check wsClient->pong(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedPingBinaryData, expectedBinData, msg = "Data mismatched");
   check wsClient->writeBytes(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedBinData, binaryData, msg = "Data mismatched");
   error? result = wsClient->close(timeoutInSeconds = 0);
}

// Tests behavior when onBytes resource is missing and binary message is received
@test:Config {}
public function testMissingOnBinary() returns Error? {
   AsyncClient wsClient = new ("ws://localhost:21006/onlyOnText", new callbackService());
   byte[] binaryData = [5, 24, 56, 243];
   expectedBinData = [];
   byte[] expectedBinData = [];
   expectedData = "";
   check wsClient->writeBytes(binaryData);
   runtime:sleep(0.5);
   test:assertEquals(expectedBinData, expectedBinData, msg = "Data mismatched");
   check wsClient->writeString("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(timeoutInSeconds = 0);
}

// Tests behavior when onBytes resource is missing and binary message is received
@test:Config {}
public function testMissingOnIdleTimeout() returns Error? {
   AsyncClient wsClient = new ("ws://localhost:21006/onlyOnText", new callbackService());
   expectedData = "";
   runtime:sleep(0.5);
   check wsClient->writeString("Hi");
   runtime:sleep(0.5);
   test:assertEquals(expectedData, "Hi", msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

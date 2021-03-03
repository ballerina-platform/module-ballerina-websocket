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
import ballerina/io;

string data = "";

listener Listener l2 = new(21003);

service /onTextString on l2 {
   resource function get .() returns Service|UpgradeError {
       return new WsService1();
   }
}

service class WsService1 {
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) returns string? {
      io:println("server on text message");
      return data;
  }

  remote isolated function onError(error err) returns Error? {
      io:println("server on error message");
  }
}

// Tests string support for writeTextMessage and onTextMessage
@test:Config {}
public function testString() returns Error? {
   Client wsClient = check new("ws://localhost:21003/onTextString/");
   check wsClient->writeTextMessage("Hi");
   data = check wsClient->readTextMessage();
   runtime:sleep(0.5);
   test:assertEquals(data, "Hi", msg = "Failed writeTextMessage");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

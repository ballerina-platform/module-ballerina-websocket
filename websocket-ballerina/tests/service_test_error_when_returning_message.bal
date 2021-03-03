// Copyright (c) 2021 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

string errdata = "";

listener Listener l60 = new(2160);

service /onTextString on l60 {
   resource function get .() returns Service|UpgradeError {
       return new WsService60();
   }
}

service class WsService60 {
  *Service;
  remote function onTextMessage(Caller caller, string data) returns string? {
      io:println("On text message");
      error? result = caller->close(1001, "Close the connection", timeoutInSeconds = 0);
      return data;
  }

  remote function onError(error err) returns Error? {
      io:println("server on error message");
      errdata = err.message();
  }
}

// Tests trying to return a text data from onTextMessage remote function when the connection is closed.
// It should be dispatched to the onError resource
@test:Config {}
public function testErrorReturningFromRemoteFunction() returns Error? {
   Client wsClient = check new("ws://localhost:2160/onTextString/");
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(1);
   test:assertEquals(errdata, "ConnectionClosureError: Close frame already sent. Cannot push text data!");
}

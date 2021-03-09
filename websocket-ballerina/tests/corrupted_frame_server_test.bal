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

string data2 = "";

listener Listener l30 = new(21103);

@ServiceConfig {
   maxFrameSize: 1
}
service /onCorrupt on l30 {
   resource function get .() returns Service|UpgradeError {
       return new corruptedService();
   }
}

service class corruptedService {
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) returns Error? {
      check caller->writeTextMessage("xyz");
  }
  remote function onError(Caller wsEp, error err) {
      io:println("on server error");
      data2 = err.message();
  }
  remote isolated function onClose(Caller wsEp, error err) {
      io:println(err);
  }
}

// Tests the error when a corrupted frame is sent
@test:Config {}
public function testCorruptedFrame() returns Error? {
   Client wsClient = check new("ws://localhost:21103/onCorrupt/");
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(3);
   test:assertEquals(data2, "PayloadTooLargeError: Max frame length of 1 has been exceeded.", msg = "Failed testCorruptedFrame");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

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

listener Listener l30 = check new(21103);

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
  remote isolated function onString(Caller caller, string data) returns Error? {
      check caller->writeString("xyz");
  }
  remote function onError(Caller wsEp, error err) {
      io:println("on server error");
      data2 = err.message();
  }
  remote isolated function onClose(Caller wsEp, error err) {
      io:println(err);
  }
}

service class clientCbackService {
    *Service;
    remote function onString(Caller wsEp, string text) {
        data2 = <@untainted>text;
    }

    remote isolated function onError(Caller wsEp, error err) {
        io:println(<@untainted>err.message());
    }

    remote isolated function onConnect(Caller wsEp) {
        io:println("On connect resource");
    }

    remote isolated function onClose(Caller wsEp, error err) {
        io:println(err.message());
    }
}

// Tests string support for writeString and onString
@test:Config {}
public function testCorruptedFrame() returns Error? {
   AsyncClient wsClient = check new("ws://localhost:21103/onCorrupt/", new clientCbackService());
   check wsClient->writeString("Hi");
   runtime:sleep(0.5);
   test:assertEquals(data2, "PayloadTooBigError: Max frame length of 1 has been exceeded.", msg = "Failed testCorruptedFrame");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

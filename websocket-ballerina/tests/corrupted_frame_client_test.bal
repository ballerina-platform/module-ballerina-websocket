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

string data3 = "";

listener Listener l31 = check new(21104);

@ServiceConfig {
   maxFrameSize: 10
}
service /onCorruptClient on l31 {
   resource function get .() returns Service|UpgradeError {
       return new corruptedClService();
   }
}

service class corruptedClService {
  *Service;
  remote isolated function onString(Caller caller, string data) returns Error? {
      check caller->writeString("xyz");
  }
  remote function onError(Caller wsEp, error err) {
      io:println("on server error");
  }
  remote isolated function onClose(Caller wsEp, error err) {
      io:println(err);
  }
}

service class clientCBService {
    *Service;
    remote function onString(Caller wsEp, string text) {
        data2 = <@untainted>text;
    }

    remote function onError(Caller wsEp, error err) {
        data3 = err.message();
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
public function testCorruptedFrameClient() returns Error? {
   AsyncClient wsClient = new("ws://localhost:21104/onCorruptClient/", new clientCBService(), config = {maxFrameSize: 1});
   check wsClient->writeString("Hi");
   runtime:sleep(0.5);
   test:assertEquals(data3, "PayloadTooBigError: Max frame length of 1 has been exceeded.", msg = "Failed testCorruptedFrameClient");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

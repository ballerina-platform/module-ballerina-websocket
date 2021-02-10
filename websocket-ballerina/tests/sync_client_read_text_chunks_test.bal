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

string chunkTxtData = "";

listener Listener l43 = new(21313);

@ServiceConfig {
   maxFrameSize: 4
}
service /onTxtDataSync on l43 {
   resource function get .() returns Service|UpgradeError {
       return new WsService43();
   }
}

service class WsService43 {
  *Service;
  remote function onTextMessage(Caller caller, string data) returns Error? {
      check caller->writeTextMessage("chunked message");
  }
}

// Tests writing binary data as continuation frames chunked by the given maxFrameSize.
@test:Config {}
public function testReadTextDataChunk() returns Error? {
   Client wsClient = check new("ws://localhost:21313/onTxtDataSync/");
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(3);
   string resp = check wsClient->readTextMessage();
   test:assertEquals("chunked message", resp, msg = "Failed testReadTextDataChunk");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

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

string chunkTextData = "";

listener Listener l42 = new(21312);

service /onTextDataSync on l42 {
   resource function get .() returns Service|UpgradeError {
       return new WsService42();
   }
}

service class WsService42 {
  *Service;
  remote function onTextMessage(Caller caller, string data) returns Error? {
      chunkTextData = data;
  }
}

// Tests writing text data as continuation frames chunked by the given maxFrameSize using sync client.
@test:Config {}
public function testSendTextDataChunkSync() returns Error? {
   Client wsClient = check new("ws://localhost:21312/onTextDataSync/", config = {maxFrameSize: 1});
   string textData = "text data";
   check wsClient->writeTextMessage(textData);
   runtime:sleep(5);
   test:assertEquals(chunkTextData, textData, msg = "Failed testSendTextDataChunkSync");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

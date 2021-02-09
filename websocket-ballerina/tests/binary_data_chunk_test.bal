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

byte[] BinData = [];

listener Listener l40 = new(21310);

service /onBinaryData on l40 {
   resource function get .() returns Service|UpgradeError {
       return new WsService40();
   }
}

service class WsService40 {
  *Service;
  remote function onBinaryMessage(Caller caller, byte[] data) returns Error? {
      BinData = data;
  }
}

// Tests writing binary data as continuation frames chunked by the given maxFrameSize.
@test:Config {}
public function testBinaryData() returns Error? {
   AsyncClient wsClient = check new("ws://localhost:21310/onBinaryData/", config = {maxFrameSize: 1});
   byte[] binaryData = [5, 24, 56];
   check wsClient->writeBinaryMessage(binaryData);
   io:println("Sleeping 5 secs");
   runtime:sleep(5);
   io:println("Slept 5 secs");
   test:assertEquals(BinData, binaryData, msg = "Failed testBinaryData");
   io:println("Asserted");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

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

import ballerina/test;
import ballerina/io;
import ballerina/lang.runtime as runtime;

string aggregatedByteOutput = "";
type outputByteArr byte[];

listener Listener l8 = new(21053);
service /onTextBytes on l8 {
   resource function get .() returns Service|UpgradeError {
       return new WsServiceSyncBytes();
   }
}

service class WsServiceSyncBytes {
  *Service;
  remote isolated function onBinaryMessage(Client caller, byte[] data) returns Error? {
      check caller->writeBinaryMessage(data);
  }

  remote isolated function onClose(Client caller, string data) returns Error? {
        check caller->writeTextMessage(data);
  }
}

// Tests the readBinaryMessage in synchronous client
@test:Config {}
public function testSyncClientByteArray() returns Error? {
   Client wsClient = check new("ws://localhost:21053/onTextBytes");
   @strand {
      thread:"any"
   }
   worker w1 {
      io:println("Reading message starting: sync byte[] client");
      byte[] resp1 = checkpanic wsClient->readBinaryMessage();
      aggregatedByteOutput = aggregatedByteOutput + resp1.toString();
      io:println("1st response received at sync byte[] client :" + resp1.toString());

      byte[] resp2 = checkpanic wsClient->readBinaryMessage();
      aggregatedByteOutput = aggregatedByteOutput + resp2.toString();
      io:println("2nd response received at sync byte[] client :" + resp2.toString());

      byte[] resp3 = checkpanic wsClient->readBinaryMessage();
      aggregatedByteOutput = aggregatedByteOutput + resp3.toString();
      io:println("3rd response received at sync byte[] client :" + resp3.toString());

      byte[] resp4 = checkpanic wsClient->readBinaryMessage();
      aggregatedByteOutput = aggregatedByteOutput + resp4.toString();
      io:println("4th response received at sync byte[] client :" + resp4.toString());

      byte[] resp5 = checkpanic wsClient->readBinaryMessage();
      aggregatedByteOutput = aggregatedByteOutput + resp5.toString();
      io:println("final response received at sync byte[] client :" + resp5.toString());
   }
   @strand {
      thread:"any"
   }
   worker w2 {
      io:println("Waiting till client starts reading byte[].");
      runtime:sleep(2);
      var resp1 = wsClient->writeBinaryMessage("Hello".toBytes());
      runtime:sleep(2);
      var resp2 = wsClient->writeBinaryMessage("Hello2".toBytes());
      runtime:sleep(2);
      var resp3 = wsClient->writeBinaryMessage("Hello3".toBytes());
      var resp4 = wsClient->writeBinaryMessage("Hello4".toBytes());
      var resp5 = wsClient->writeBinaryMessage("Hello5".toBytes());
   }
   _ = wait {w1, w2};
   string msg = "[72,101,108,108,111][72,101,108,108,111,50][72,101,108,108,111,51][72,101,108,108,111,52][72,101,108,108,111,53]";
   test:assertEquals(aggregatedByteOutput, msg, msg = "");
   runtime:sleep(2);
}

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
import ballerina/lang.runtime;

string readMessageStringOutput = "";
string readMessageByteOutput = "";

listener Listener l76 = new(21011);

service /testReadMessage on l76 {
   resource function get .() returns Service|UpgradeError {
       return new WsServiceSync76();
   }
}

service class WsServiceSync76 {
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) returns Error? {
      check caller->writeTextMessage(data);
  }

  remote isolated function onBinaryMessage(Caller caller, byte[] data) returns Error? {
      check caller->writeBinaryMessage(data);
  }
}

// Tests the readMessage function in client
@test:Config {}
public function testSyncClientReadMessage() returns error? {
   Client wsClient = check new("ws://localhost:21011/testReadMessage");
   @strand {
      thread:"any"
   }
   worker w1 returns error? {
      byte[]|string|Error resp1 = wsClient->readMessage();
      if resp1 is string {
         readMessageStringOutput = readMessageStringOutput + resp1;
      }

      byte[]|string|Error resp2 = check wsClient->readMessage();
      if resp2 is string {
         readMessageStringOutput = readMessageStringOutput + resp2;
      } else if resp2 is byte[] {
         readMessageByteOutput = resp2.toString();
      }

      byte[]|string|Error resp3 = check wsClient->readMessage();
      if resp3 is string {
         readMessageStringOutput = readMessageStringOutput + resp3;
      }

      runtime:sleep(3);

      byte[]|string|Error resp4 = check wsClient->readMessage();
      if resp4 is string {
         readMessageStringOutput = readMessageStringOutput + resp4;
      } else if resp4 is byte[] {
         readMessageByteOutput = readMessageByteOutput + resp4.toString();
      }

      byte[]|string|Error resp5 = check wsClient->readMessage();
      if resp5 is string {
         readMessageStringOutput = readMessageStringOutput + resp5;
      }
   }
   @strand {
      thread:"any"
   }
   worker w2 {
      runtime:sleep(2);
      Error? resp1 = wsClient->writeTextMessage("Hi world1");
      runtime:sleep(2);
      Error? resp2 = wsClient->writeBinaryMessage("Hi world2".toBytes());
      runtime:sleep(2);
      Error? resp3 = wsClient->writeTextMessage("Hi world3");
      Error? resp4 = wsClient->writeBinaryMessage("Hi world4".toBytes());
      Error? resp5 = wsClient->writeTextMessage("Hi world5");
   }
   var waitResp = wait {w1, w2};
   string msg = "Hi world1Hi world3Hi world5";
   string byteOutput = "[72,105,32,119,111,114,108,100,50][72,105,32,119,111,114,108,100,52]";
   test:assertEquals(readMessageStringOutput, msg);
   test:assertEquals(readMessageByteOutput, byteOutput);
   runtime:sleep(3);
}

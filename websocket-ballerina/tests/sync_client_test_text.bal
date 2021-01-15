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

string aggregatedTextOutput = "";
listener Listener l11 = check new(21000);
service /onTextString on l11 {
   resource function get .() returns Service|UpgradeError {
       return new WsServiceSync();
   }
}

service class WsServiceSync {
  *Service;
  remote isolated function onString(Caller caller, string data) returns Error? {
      check caller->writeString(data);
  }

  remote isolated function onClose(Caller caller, string data) returns Error? {
        check caller->writeString(data);
  }
}

@test:Config {}
public function testSyncClient() returns Error? {
   SyncClient wsClient = check new("ws://localhost:21000/onTextString");
   @strand {
      thread:"any"
   }
   worker w1 {
      io:println("Reading message starting: sync text client");

      string resp1 = checkpanic wsClient->readString();
      aggregatedTextOutput = aggregatedTextOutput + resp1;
      io:println("1st response received at sync text client :" + resp1);

      var resp2 = checkpanic wsClient->readString();
      aggregatedTextOutput = aggregatedTextOutput + resp2;
      io:println("2nd response received at sync text client :" + resp2);

      var resp3 = checkpanic wsClient->readString();
      aggregatedTextOutput = aggregatedTextOutput + resp3;
      io:println("3rd response received at sync text client :" + resp3);

      runtime:sleep(3);

      var resp4 = checkpanic wsClient->readString();
      aggregatedTextOutput = aggregatedTextOutput + resp4;
      io:println("4th response received at sync text client :" + resp4);

      var resp5 = checkpanic wsClient->readString();
      aggregatedTextOutput = aggregatedTextOutput + resp5;
      io:println("Final response received at sync text client :" + resp5);
   }
   @strand {
      thread:"any"
   }
   worker w2 {
      io:println("Waiting till client starts reading text.");
      runtime:sleep(2);
      var resp1 = wsClient->writeString("Hi world1");
      runtime:sleep(2);
      var resp2 = wsClient->writeString("Hi world2");
      runtime:sleep(2);
      var resp3 = wsClient->writeString("Hi world3");
      var resp4 = wsClient->writeString("Hi world4");
      var resp5 = wsClient->writeString("Hi world5");
   }
   _ = wait {w1, w2};
   string msg = "Hi world1Hi world2Hi world3Hi world4Hi world5";
   test:assertEquals(aggregatedTextOutput, msg, msg = "");
   runtime:sleep(3);
}

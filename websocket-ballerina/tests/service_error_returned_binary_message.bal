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

string errbindata = "";

listener Listener l61 = new(2161);

service /onBinString on l61 {
   resource function get .() returns Service|UpgradeError {
       io:println("On upgrade");
       return new WsService61();
   }
}

service class WsService61 {
  *Service;
  remote function onBinaryMessage(Caller caller, byte[] data) returns byte[] {
      io:println("On binary message");
      error? result = caller->close(1001, "Close the connection", timeout = 0);
      return data;
  }

  remote function onError(error err) returns Error? {
      io:println("server on error message");
      errbindata = err.message();
  }
}

// Tests trying to return a binary data from onBinaryMessage remote function when the connection is closed.
// It should be dispatched to the onError resource
@test:Config {}
public function testErrorReturningFromRemoteFunctionForBinary() returns Error? {
   Client wsClient = check new("ws://localhost:2161/onBinString/");
   check wsClient->writeBinaryMessage("Hi".toBytes());
   runtime:sleep(1);
   test:assertEquals(errbindata, "ConnectionClosureError: Close frame already sent. Cannot push binary data.");
}

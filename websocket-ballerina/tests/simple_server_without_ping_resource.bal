// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/lang.runtime as runtime;
import ballerina/test;

byte[] expectedAutoPongData = [];
listener Listener l23 = new(21020);
service / on l23 {
   resource isolated function get .() returns Service|UpgradeError {
       return new TestService();
   }
}

service class TestService {
   *Service;
   remote function onOpen(Caller wsEp) {
       io:println("New Client Connected");
   }
}

service class PongService {
   *Service;
   remote function onPong(Caller wsEp, byte[] data) {
       expectedAutoPongData = <@untainted>data;
   }
}

// Tests the auto ping pong support in Ballerina if there is no onPing resource
@test:Config {}
public function testAutoPingPongSupport() returns Error? {
   AsyncClient wsClient = check new ("ws://localhost:21020", new PongService());
   byte[] pingData = [5, 24, 56, 243];
   check wsClient->ping(pingData);
   runtime:sleep(0.5);
   test:assertEquals(expectedAutoPongData, pingData, msg = "Data mismatched");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

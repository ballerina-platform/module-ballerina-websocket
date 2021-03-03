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

import ballerina/lang.runtime as runtime;
import ballerina/test;

byte[] expectedPongData = [];
byte[] expectedPongData1 = [];

listener Listener l19 = new(21014);

service /pingpong/ws on l19 {
    resource isolated function get .() returns Service|UpgradeError  {
       return new ServerPingPongService();
    }
}

service class ServerPingPongService {
  *Service;
   remote isolated function onOpen(Caller caller) {
   }

   remote isolated function onPing(Caller caller, byte[] localData) {
       var returnVal = caller->pong(localData);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }

   remote isolated function onPong(Caller caller, byte[] localData) {
       var returnVal = caller->ping(localData);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }
}

service class pingPongCallbackService {
   *PingPongService;
   remote function onPing(Caller wsEp, byte[] localData) {
       expectedPongData1 = <@untainted>localData;
   }

   remote function onPong(Caller wsEp, byte[] localData) {
       expectedPongData = <@untainted>localData;
   }
}

// Tests ping to Ballerina WebSocket server
@test:Config {}
public function testPingToBallerinaServer() returns Error? {
   Client wsClient = check new ("ws://localhost:21014/pingpong/ws", new pingPongCallbackService());
   byte[] pongData = [5, 24, 56, 243];
   check wsClient->ping(pongData);
   runtime:sleep(0.5);
   test:assertEquals(expectedPongData, pongData);
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// // Tests pong to Ballerina WebSocket server
// @test:Config {}
// public function testPingFromRemoteServerToBallerinaClient() returns Error? {
//    Client wsClient = check new ("ws://localhost:21014/pingpong/ws", new pingPongCallbackService());
//    byte[] pongData = [5, 24, 34];
//    check wsClient->pong(pongData);
//    runtime:sleep(0.5);
//    test:assertEquals(expectedPongData1, pongData);
//    error? result = wsClient->close(statusCode = 1000, timeoutInSeconds = 0);
// }

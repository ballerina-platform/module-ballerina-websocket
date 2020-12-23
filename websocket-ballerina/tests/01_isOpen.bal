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

import ballerina/runtime;
import ballerina/test;
import ballerina/http;
import ballerina/io;

listener Listener socketListener = new(21001);
string output = "";
int x = 0;

service UpgradeService /isOpen/abc on socketListener {
    remote function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError  {
       if (x < 1) {
          x = x + 1;
          return new MyWSService();
       } else {
          return new MyWSService2();
       }
    }
}

service class MyWSService {
  *Service;
  remote function onText(Caller caller, string text) {
      io:println("Service 1");
      io:println(caller.getConnectionId());
      WebSocketError? err = caller->close(timeoutInSeconds = 0);
      output = <@untainted>("In onText isOpen " + caller.isOpen().toString());
  }
}

service class MyWSService2 {
  *Service;
  remote function onText(Caller caller, string text) {
      io:println("Service 2");
      io:println(caller.getConnectionId());
      WebSocketError? err = caller->close(timeoutInSeconds = 0);
      output = <@untainted>("In onText isOpen " + caller.isOpen().toString());
  }
}

// Test isOpen when close is called
@test:Config {}
public function testIsOpenCloseCalled() {
    Client wsClient = new("ws://localhost:21001/isOpen/abc");
    checkpanic wsClient->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(output, "In onText isOpen false");

    Client wsClient2 = new("ws://localhost:21001/isOpen/abc");
    checkpanic wsClient2->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(output, "In onText isOpen false");

    Client wsClient3 = new("ws://localhost:21001/isOpen/abc");
    checkpanic wsClient3->pushText("Hi");
    runtime:sleep(500);
    test:assertEquals(output, "In onText isOpen false");
}

// Test isOpen when a close frame is received
// Disable due to https://github.com/ballerina-platform/module-ballerina-http/issues/71#issuecomment-707017984
@test:Config {enable : false}
public function testIsOpenCloseFrameReceived() {
    Client wsClient = new ("ws://localhost:21001");
    checkpanic wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 300);
    runtime:sleep(500);
    test:assertEquals(output, "In onClose isOpen true");
}

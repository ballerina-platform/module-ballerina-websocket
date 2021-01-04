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
import ballerina/runtime;
import ballerina/test;
import ballerina/http;

string serverOutput = "";

service UpgradeService /server/errors on new Listener(21031) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new ServerError();
   }
}
service class ServerError {
   *Service;
   remote function onText(Caller caller, string text) {
       checkpanic caller->writeString("Hello World!", false);
       string hello = "hello";
       byte[] data = hello.toBytes();
       var err = caller->writeBytes(data, false);
       if (err is error) {
           serverOutput = <@untainted>err.message();
       } else {
           serverOutput = <@untainted>"";
       }
   }

   remote function onBinary(Caller caller, byte[] data, boolean finalFrame) {
       var returnVal = caller->writeBytes(data, finalFrame);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onError(Caller caller, error err) {
       io:println(err);
   }
}

// Frame continuation error
@test:Config {}
public function testContinuationFrameError() {
   AsyncClient wsClientEp = new ("ws://localhost:21031/server/errors");
   var err = trap wsClientEp->writeString("Hi kalai");
   runtime:sleep(500);
   test:assertEquals(serverOutput, "InvalidContinuationFrameError: Cannot interrupt WebSocket" +
       " text frame continuation");
}

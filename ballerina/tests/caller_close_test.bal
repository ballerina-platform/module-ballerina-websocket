// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/test;
import ballerina/lang.runtime;

string closeResult = "";
string close2Result = "";

service /basic/ws on new Listener(9090) {
   resource function get .() returns Service|Error {
       return new WsService();
   }
}

service class WsService {
    *Service;
    remote function onMessage(Caller caller, json text) returns Error? {
        Error? close = caller->close(timeout = 3);
        if close is Error {
           closeResult = close.message();
        } else {
           closeResult = "success";
        }
        Error? close2 = caller->close(timeout = 3);
        if close2 is Error {
           close2Result = close2.message();
        } else {
           close2Result = "success";
        }
    }
}

@test:Config {}
public function testCallerClose() returns Error? {
   Client wsClient = check new("ws://localhost:9090/basic/ws");
   check wsClient->writeMessage({"Text": "message"});
   json|Error resp = wsClient->readMessage();
   if resp is json {
       test:assertFail("Expected a connection closure error");
   }
   runtime:sleep(8);
   test:assertEquals(closeResult, "success");
   test:assertEquals(close2Result, "ConnectionClosureError: Close frame already sent. Cannot send close frame again.");
}

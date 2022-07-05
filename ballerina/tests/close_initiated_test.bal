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

boolean callerInitiatedClose = false;

service /basic2 on new Listener(9091) {
   resource function get .() returns Service|Error {
       return new closeService();
   }
}

service class closeService {
    *Service;
    remote function onMessage(Caller caller, json text) returns Error? {
        check caller->close(timeout = 3);
        if caller.isClosed() {
            callerInitiatedClose = true;
        }
    }
}

@test:Config {}
public function testCallerInitiatedClose() returns Error? {
   Client wsClient = check new("ws://localhost:9091/basic2");
   check wsClient->writeMessage({"Text": "message"});
   json|Error resp = wsClient->readMessage();
   if resp is json {
       test:assertFail("Expected a connection closure error");
   }
   runtime:sleep(5);
   test:assertTrue(callerInitiatedClose);
}

@test:Config {}
public function testClientInitiatedClose() returns Error? {
   Client wsClient = check new("ws://localhost:9091/basic2");
   check wsClient->close(timeout = 3);
   boolean clientInitiatedClose = false;
   if wsClient.isClosed() {
       clientInitiatedClose = true;
   }
   test:assertTrue(clientInitiatedClose);
}

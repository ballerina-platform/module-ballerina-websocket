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

service /echo on new Listener(9092) {
   resource function get .() returns Service|Error {
       return new WsService9092();
   }
}

service class WsService9092 {
    *Service;
    remote function onOpen() {
    }
}

@test:Config {}
public function testServiceNotFound() returns Error? {
    Client|Error wsClient = new("ws://localhost:9092");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
    } else {
        test:assertFail("Expected an service not found error");
    }
}

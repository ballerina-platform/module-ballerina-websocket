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

string closeError = "";
listener Listener l32 = new(21002);
service /onCloseText on l32 {
   resource function get .() returns Service|UpgradeError {
       return new WsServiceSyncClose();
   }
}

service class WsServiceSyncClose {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) returns Error? {
        Error? closeResp = caller->close(statusCode = 1000, reason = "Close the connection");
    }

    remote isolated function onClose(Caller caller, string data) returns Error? {
        check caller->close();
    }
}

// Tests the connection close in readTextMessage in synchronous client
@test:Config {}
public function testSyncClientClose() returns Error? {
    Client wsClient = check new("ws://localhost:21002/onCloseText");
    io:println("Reading message starting: sync close client");
    Error? resp1 = wsClient->writeTextMessage("Hi world1");
    string|Error resp2 = wsClient->readTextMessage();
    if resp2 is Error {
        closeError = resp2.message();
    } else {
        io:println("1st response received at sync close client :" + resp2);
    }
    string msg = "Close the connection: Status code: 1000";
    test:assertEquals(closeError, msg, msg = "");
}

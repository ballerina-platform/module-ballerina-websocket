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

listener Listener l72 = new(21072);
string dispatchedTextData = "not received";
byte[] dispatchedBinaryData = [5, 24, 56];

service /onDispatchError on l72 {
    resource function get .() returns Service|UpgradeError {
        return new WsService72();
    }
}

service class WsService72 {
    *Service;
    remote function onTextMessage(Caller caller, string data, int x) returns string? {
        dispatchedTextData = "text received";
        io:println("server on text message");
        return data;
    }

    remote function onBinaryMessage(Caller caller, byte[] data, string sData) returns Error? {
        dispatchedBinaryData = data;
    }

    remote isolated function onError(error err) returns Error? {
        io:println("server on error message");
    }
}

// Tests dispatching error onTextMessage
@test:Config {}
public function testDispatchingErrorOnTextMessage() returns Error? {
    Client wsClient = check new ("ws://localhost:21072/onDispatchError/");
    check wsClient->writeTextMessage("Hi");
    runtime:sleep(2);
    test:assertEquals(dispatchedTextData, "not received");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

// Tests dispatching error onBinaryMessage
@test:Config {}
public function testDispatchingErrorOnBinaryMessage() returns Error? {
    Client wsClient = check new ("ws://localhost:21072/onDispatchError/");
    byte[] data = [5, 24, 56, 45];
    check wsClient->writeBinaryMessage(data);
    runtime:sleep(2);
    test:assertEquals(dispatchedBinaryData, [5, 24, 56]);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

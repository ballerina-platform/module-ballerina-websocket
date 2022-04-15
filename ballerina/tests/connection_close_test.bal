// Copyright (c) 2022 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

import ballerina/io;
import ballerina/test;

listener Listener closeLis = new(22079);

service /ws on closeLis {
    resource function get ser () returns Service {
        return new WsService100();
    }
}

service class WsService100 {
    *Service;

    remote function onTextMessage(string text) returns error? {
        io:println(text);
    }
}

service /ws1 on closeLis {
    resource function get onClose () returns Service {
        return new WsService101();
    }
}

service class WsService101 {
    *Service;

    remote function onTextMessage(string text) returns error? {
        io:println(text);
    }

    remote function onClose(Caller caller) returns error? {
        Error? status = caller->close();
    }
}

@test:Config {}
public function testConnectionErrorWithoutOnClose() returns Error? {
    Client cl = check new ("ws://localhost:22079/ws/ser");
    check cl->writeTextMessage("hello");
    Error? conClose = cl->close(timeout = 60);
    if conClose is error {
        test:assertFail("Connection close failed");
    }
}

@test:Config {}
public function testConnectionErrorWithOnClose() returns Error? {
    Client cl = check new ("ws://localhost:22079/ws1/onClose");
    check cl->writeTextMessage("hello");
    Error? conClose = cl->close(timeout = 60);
    if conClose is error {
        test:assertFail("Connection close failed");
    }
}

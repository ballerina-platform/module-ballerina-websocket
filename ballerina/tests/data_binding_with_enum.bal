// Copyright (c) 2023 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
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

enum Point {
    A,
    B,
    C
}

service on new Listener(8000) {
    resource function get .() returns Service|Error {
        return new EnumService();
    }
}

service class EnumService {
    *Service;

    remote function onMessage(Caller caller, Point data) returns Error? {
        check caller->writeMessage(B);
    }
}

@test:Config {}
public function testEnum() returns Error? {
    Client cl = check new("ws://localhost:8000");
    check cl->writeMessage(A);
    Point p = check cl->readMessage();
}

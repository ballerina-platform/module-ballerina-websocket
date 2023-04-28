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
import ballerina/io;

listener Listener onErrorListener = new(21402);

service on onErrorListener {
    resource function get .() returns Service|Error {
        return new ServiceWithOnError();
    }
}

service class ServiceWithOnError {
    *Service;
    remote function onError(Caller caller, error data) returns Error? {
        io:println(data);
        check caller->writeMessage({"event": "onError"});
    }

    remote function onMessage(Caller caller, int data) returns Error? {
        check caller->writeMessage({"event": "onMessage"});
    }
}

@test:Config {}
public function testBindingFailureWithOnError() returns Error? {
    Client cl = check new("ws://localhost:21402");
    check cl->writeMessage("event".toBytes());
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"event": "onError"});
}
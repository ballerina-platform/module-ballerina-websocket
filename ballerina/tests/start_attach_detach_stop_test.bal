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

import ballerina/io;
import ballerina/test;

listener Listener lis = new(21077);

UpgradeService dummyService = service object {
    resource function get .() returns Service|UpgradeError {
        return new WsService70();
    }
};

service class WsService70 {
    *Service;
    remote function onTextMessage(Caller caller, string text) {
        io:println(text);
    }
}

@test:Config {}
public function testAttachDetachGracefulStop() returns error? {
    check lis.attach(dummyService);
    check lis.'start();
    Client client1 = check new("ws://localhost:21077");
    check client1->writeTextMessage("Testing 123");
    check lis.detach(dummyService);
    check lis.gracefulStop();

    Client|Error client2 = new("ws://localhost:21077");
    if client2 is Error {
        test:assertEquals(client2.message(), "ConnectionError: IO Error");
    } else {
        test:assertFail("Expecting a connection error");
    }
}

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

import ballerina/lang.runtime as runtime;
import ballerina/test;

listener Listener l77 = new(21012);

service /onReadonlyBinary on l77 {
    resource function get .() returns Service|UpgradeError {
        return new WsService77();
    }
}

service class WsService77 {
    *Service;
    remote isolated function onBinaryMessage(Caller caller, readonly & byte[] data) returns byte[]? {
        return data;
    }

    remote isolated function onPing(Caller caller, readonly & byte[] data) returns byte[]? {
        return data;
    }
}

// Tests readonly support for onBinaryMessage
@test:Config {}
public function testReadonlyBinary() returns Error? {
    Client wsClient = check new("ws://localhost:21012/onReadonlyBinary/");
    byte[] bindata = [5, 24, 56];
    check wsClient->writeBinaryMessage(bindata);
    byte[] data = check wsClient->readBinaryMessage();
    check wsClient->ping(data);
    runtime:sleep(0.5);
    test:assertEquals(data, bindata);
}

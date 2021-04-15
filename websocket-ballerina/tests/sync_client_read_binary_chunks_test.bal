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

string chunkBinData = "";

listener Listener l44 = new(21314);

@ServiceConfig {
   maxFrameSize: 4
}
service /onBinDataSync on l44 {
    resource function get .() returns Service|UpgradeError {
        return new WsService44();
    }
}

service class WsService44 {
    *Service;
    remote function onTextMessage(string data) returns byte[]? {
        return [5, 24, 56, 5, 56, 24];
    }
}

// Tests reading binary data coming as continuation frames and aggragating them to a single binary message.
@test:Config {}
public function testReadBinaryDataChunkSync() returns Error? {
    Client wsClient = check new("ws://localhost:21314/onBinDataSync/");
    byte[] binaryData = [5, 24, 56, 5, 56, 24];
    check wsClient->writeTextMessage("Hi");
    runtime:sleep(3);
    byte[] resp = check wsClient->readBinaryMessage();
    test:assertEquals(resp, binaryData, msg = "Failed testReadBinaryDataChunkSync");
    error? result = wsClient->close(reason = "Close the connection", timeout = 0);
}

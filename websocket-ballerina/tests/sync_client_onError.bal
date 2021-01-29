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
import ballerina/lang.runtime as runtime;

string corruptedFrameError = "";
listener Listener l33 = new(21055);
service /onErrorText on l33 {
    resource function get .() returns Service|UpgradeError {
        return new WsServiceSyncError();
    }
}

service class WsServiceSyncError {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) returns Error? {
        check caller->writeTextMessage(data);
    }

    remote isolated function onClose(Caller caller, string data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

// Tests the corrupted frame error returned from readTextMessage
@test:Config {}
public function testSyncClientError() returns Error? {
    Client wsClient = check new("ws://localhost:21055/onErrorText", config = {maxFrameSize: 1});
    @strand {
        thread:"any"
    }
    worker w1 {
        io:println("Reading message starting: sync error client");

        string|Error resp1 = wsClient->readTextMessage();
        if (resp1 is Error) {
            corruptedFrameError = resp1.message();
        } else {
            io:println("1st response received at sync close client :" + resp1);
        }
    }
    @strand {
        thread:"any"
    }
    worker w2 {
        io:println("Waiting till error client starts reading text.");
        runtime:sleep(2);
        var resp1 = wsClient->writeTextMessage("Hi world1");
        runtime:sleep(2);
    }
    _ = wait {w1, w2};
    string msg = "Max frame length of 1 has been exceeded.";
    test:assertEquals(corruptedFrameError, msg);
    runtime:sleep(3);
}

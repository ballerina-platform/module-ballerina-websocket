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

string readMessageIdleTimeOutError = "";
string readMessageSecondReadResp = "";

listener Listener l66 = new(21012);
service /onIdleTimeout on l66 {
    resource function get .() returns Service|UpgradeError {
        return new wsService66();
    }
}

service class wsService66 {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) returns Error? {
        check caller->writeTextMessage(data);
    }

    remote isolated function onBinaryMessage(Caller caller, byte[] data) returns Error? {
        check caller->writeBinaryMessage(data);
    }
}

// Tests the idle timeout error returned from readTextMessage and then read again
// to check if the idle state handler gets reset.
@test:Config {}
public function testSyncReadMessageIdleTimeOutError() returns Error? {
    Client wsClient = check new("ws://localhost:21012/onIdleTimeout", config = {readTimeout: 2});
    @strand {
        thread:"any"
    }
    worker w1 {
        io:println("Reading message starting: sync read message idle timeout client");

        string|byte[]|Error resp1 = wsClient->readMessage();
        if (resp1 is Error) {
            readMessageIdleTimeOutError = resp1.message();
        } else {
            io:println("1st response received at sync read message idle timeout client");
        }
        string|byte[]|Error resp2 = wsClient->readMessage();
        if (resp2 is Error) {
            readMessageIdleTimeOutError = resp2.message();
        } else if (resp2 is byte[]) {
            readMessageSecondReadResp = resp2.toString();
        }
    }
    @strand {
        thread:"any"
    }
    worker w2 {
        io:println("Waiting till idle timeout client starts reading text.");
        runtime:sleep(3);
        Error? resp1 = wsClient->writeBinaryMessage("Hi world1".toBytes());
        runtime:sleep(2);
    }
    _ = wait {w1, w2};
    string msg = "Read timed out";
    test:assertEquals(readMessageIdleTimeOutError, msg);
    test:assertEquals(readMessageSecondReadResp, "[72,105,32,119,111,114,108,100,49]");
}

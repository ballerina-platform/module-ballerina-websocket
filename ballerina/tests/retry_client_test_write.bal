// Copyright (c) 2020 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

@test:Config {dependsOn: [testReadRetryFailure]}
public function testWriteRetryForTextMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryForTextMessages--------------------------------");
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        startRemoteServer();
        Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 10} });
        check wsClient->writeTextMessage("Hi");
        string firstResp = check wsClient->readTextMessage();
        io:println("Received first connected response from server " + firstResp);
        string rdata1 = check wsClient->readTextMessage();
        io:println("Received echo response from server " + rdata1);
        stopRemoteServer();
        runtime:sleep(0.5);
        Error? writeResp = wsClient->writeTextMessage("Hello");
        if writeResp is Error {
            io:println("Error writing message " + writeResp.message());
        }
        Error? writeResp2 = wsClient->writeTextMessage("Hello");
        if writeResp2 is Error {
            test:assertFail(msg = "Test testWriteRetryForTextMessages Failed!");
        }
        stopRemoteServer();
    }
    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(6);
        startRemoteServer();
    }
    var waitResp = wait {w1, w2};
}

@test:Config {dependsOn: [testWriteRetryForTextMessages]}
public function testWriteRetryFailureForTextMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryFailureForTextMessages--------------------------------");
    startRemoteServer();
    Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 3} });
    stopRemoteServer();
    runtime:sleep(0.5);
    check wsClient->writeTextMessage("Hi");
    Error? writeResp = wsClient->writeTextMessage("Hello");
    if writeResp is Error {
        test:assertEquals(writeResp.message(), "ConnectionError: IO Error");
    } else {
        test:assertFail(msg = "Test testWriteRetryFailure Failed!");
    }
    stopRemoteServer();
}

@test:Config {dependsOn: [testWriteRetryFailureForTextMessages]}
public function testWriteRetryWithFragmentsForTextMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryWithFragmentsForTextMessages--------------------------------");
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        startRemoteServer();
        Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 10}, maxFrameSize: 20 });
        string firstResp = check wsClient->readTextMessage();
        io:println("Received first connected response from server " + firstResp);
        stopRemoteServer();
        runtime:sleep(0.5);
        check wsClient->writeTextMessage("Hi");
        Error? writeResp = wsClient->writeTextMessage("Hello Hello Hello Hello Hello Hello");
        if writeResp is Error {
            test:assertFail(msg = "Test testWriteRetryFailure Failed!");
        }
    }
    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(6);
        startRemoteServer();
        runtime:sleep(4);
        stopRemoteServer();
    }
    var waitResp = wait {w1, w2};
}

@test:Config { dependsOn: [testWriteRetryWithFragmentsForTextMessages] }
public function testWriteRetryForBinaryMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryWithFragmentsForTextMessages--------------------------------");
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        startRemoteServer();
        Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 10} });
        io:println("Hi".toBytes());
        check wsClient->writeBinaryMessage("Hi".toBytes());
        string firstResp = check wsClient->readTextMessage();
        io:println("Received first connected response from server " + firstResp);
        byte[] rdata1 = check wsClient->readBinaryMessage();
        io:println("********************Received echo response from server ");
        io:println(rdata1);
        stopRemoteServer();
        runtime:sleep(0.5);
        Error? writeResp = wsClient->writeBinaryMessage("Hello".toBytes());
        if writeResp is Error {
            io:println("Error writing message " + writeResp.message());
        }
        Error? writeResp2 = wsClient->writeBinaryMessage("Hello".toBytes());
        if writeResp2 is Error {
            io:println("Error writing message " + writeResp2.message());
        }
        string rResp3 = check wsClient->readTextMessage();
        io:println("**********************Received echo response from server " + rResp3);
        byte[]|Error rResp4 = wsClient->readBinaryMessage();
        if (rResp4 is Error) {
            io:println("Error occurred at the 2nd read " + rResp4.message());
            test:assertFail(msg = "Test testWriteRetryForBinaryMessages Failed!");
        } else {
            io:println("Received echo response from server ");
            io:println(rResp4);
            io:println("Hello".toBytes());
            test:assertEquals(rResp4, "Hello".toBytes());
        }
    }
    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(6);
        startRemoteServer();
        runtime:sleep(4);
        stopRemoteServer();
    }
    var waitResp = wait {w1, w2};
}

@test:Config {dependsOn: [testWriteRetryForBinaryMessages]}
public function testWriteRetryFailureForBinaryMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryFailureForBinaryMessages--------------------------------");
    startRemoteServer();
    Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 3} });
    stopRemoteServer();
    runtime:sleep(0.5);
    check wsClient->writeBinaryMessage("Hi".toBytes());
    Error? writeResp = wsClient->writeBinaryMessage("Hello".toBytes());
    if writeResp is Error {
        test:assertEquals(writeResp.message(), "ConnectionError: IO Error");
    } else {
        test:assertFail(msg = "Test testWriteRetryFailure Failed!");
    }
    stopRemoteServer();
}

@test:Config { dependsOn: [testWriteRetryFailureForBinaryMessages] }
public function testWriteRetryWithFragmentsForBinaryMessages() returns error? {
    io:println("------------------------------Executing testWriteRetryWithFragmentsForBinaryMessages--------------------------------");
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        startRemoteServer();
        Client wsClient = check new("ws://localhost:21078/websocket", { retryConfig: {maxCount: 10}, maxFrameSize: 20 });
        string firstResp = check wsClient->readTextMessage();
        io:println("Received first connected response from server " + firstResp);
        stopRemoteServer();
        runtime:sleep(0.5);
        check wsClient->writeBinaryMessage("Hello Hello Hello Hello Hello Hello".toBytes());
        Error? writeResp = wsClient->writeBinaryMessage("Hello Hello Hello Hello Hello Hello".toBytes());
        if writeResp is Error {
            test:assertFail(msg = "Test testWriteRetryWithFragmentsForBinaryMessages Failed!");
        } else {
            io:println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        }
    }
    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(6);
        startRemoteServer();
        runtime:sleep(4);
        stopRemoteServer();
    }
    var waitResp = wait {w1, w2};
}



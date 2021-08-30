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
import ballerina/jballerina.java;

string rdata2 = "";

@test:Config {dependsOn: [testWsUpgradeCancelDueToPathError]}
public function testReadRetryHandshake() returns error? {
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        io:println("Executing testReadRetryHandshake...");
        Client|Error wsClient = new("ws://localhost:21078/websocket", {retryConfig: {maxCount: 10, maxWaitInterval: -1}, readTimeout: 40});
        if (wsClient is Error) {
            test:assertFail(msg = "Test testReadRetryHandshake Failed!");
        }
        stopRemoteServer();
    }
    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(2);
        startRemoteServer();
    }
    var waitResp = wait {w1, w2};
}

@test:Config {dependsOn: [testReadRetryHandshake]}
public function testReadRetry() returns error? {
    Client? websocketClient = ();
    io:println("Executing testReadRetry...");
    @strand {
        thread:"any"
    }
    worker w1 returns error? {
        startRemoteServer();
        Client wsClient = check new("ws://localhost:21078/websocket", {retryConfig: {maxCount: 10}});
        websocketClient = wsClient;
        string data = check wsClient->readTextMessage();
        io:println("Received first connected response from server after retrying " + data);
        runtime:sleep(2);
        rdata2 = check wsClient->readTextMessage();
        io:println("Received connected response from server after retrying " + rdata2);
        stopRemoteServer();
    }

    @strand {
        thread:"any"
    }
    worker w2 returns error? {
        runtime:sleep(2);
        stopRemoteServer();
        runtime:sleep(6);
        startRemoteServer();
    }

    var waitResp = wait {w1, w2};
    test:assertEquals(rdata2, "Connected");
}

@test:Config {dependsOn: [testReadRetry]}
public function testReadRetryFailure() returns error? {
    io:println("Executing testReadRetryFailure...");
    Client|Error wsClient = new("ws://localhost:21800/websocket", {retryConfig: {maxCount: 3}});
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "ConnectionError: IO Error");
    } else {
        test:assertFail(msg = "Test testReadRetryFailure Failed!");
    }
}

public function startRemoteServer() = @java:Method {
    name: "start",
    'class: "io.ballerina.stdlib.websocket.testutils.WebSocketRemoteServer"
} external;

public function stopRemoteServer() = @java:Method {
    name: "stop",
    'class: "io.ballerina.stdlib.websocket.testutils.WebSocketRemoteServer"
} external;

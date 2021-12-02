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

listener Listener l49 = new(21318);
string wsService49Data = "";

@ServiceConfig {
    auth: [
        {
            fileUserStoreConfig: {},
            scopes: ["write", "update"]
        }
    ]
}
service /basicAuth on l49 {
    resource function get .() returns Service {
        return new WsService49();
    }
}

service class WsService49 {
    *Service;
    remote function onTextMessage(string data) {
        wsService49Data = data;
    }
}

@test:Config {}
public function testBasicAuthServiceAuthSuccess() returns Error? {
    Client wsClient = check new("ws://localhost:21318/basicAuth/", {
        auth: {
            username: "alice",
            password: "xxx"
        }
    });
    check wsClient->writeTextMessage("Hello, World!");
    runtime:sleep(0.5);
    test:assertEquals(wsService49Data, "Hello, World!");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testBasicAuthServiceAuthzFailure() {
    Client|Error wsClient = new("ws://localhost:21318/basicAuth/", {
        auth: {
            username: "bob",
            password: "yyy"
        }
    });
    test:assertTrue(wsClient is Error);
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 403 Forbidden");
    }
}

@test:Config {}
public function testBasicAuthServiceAuthnFailure() {
    Client|Error wsClient = new("ws://localhost:21318/basicAuth/", {
        auth: {
            username: "peter",
            password: "123"
        }
    });
    test:assertTrue(wsClient is Error);
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 401 Unauthorized");
    }
}

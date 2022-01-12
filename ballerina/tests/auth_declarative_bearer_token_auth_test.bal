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

listener Listener l54 = new(21324);
string wsService54Data = "";

@ServiceConfig {
    auth: [
        {
            oauth2IntrospectionConfig: {
                url: "https://localhost:9445/oauth2/introspect",
                tokenTypeHint: "access_token",
                scopeKey: "scp",
                clientConfig: {
                    secureSocket: {
                       cert: {
                           path: TRUSTSTORE_PATH,
                           password: "ballerina"
                       }
                    }
                }
            },
            scopes: ["write", "update"]
        }
    ]
}
service /bearerTokenAuth on l54 {
    resource function get .() returns Service {
        return new WsService54();
    }
}

service class WsService54 {
    *Service;
    remote function onTextMessage(string data) {
        wsService54Data = data;
    }
}

@test:Config {}
public function testBearerTokenAuthServiceAuthSuccess() returns Error? {
    Client wsClient = check new("ws://localhost:21324/bearerTokenAuth/", {
        auth: {
            token: ACCESS_TOKEN_1
        }
    });
    check wsClient->writeTextMessage("Hello, World!");
    runtime:sleep(0.5);
    test:assertEquals(wsService54Data, "Hello, World!");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testBearerTokenAuthServiceAuthzFailure() {
    Client|Error wsClient = new("ws://localhost:21324/bearerTokenAuth/", {
        auth: {
            token: ACCESS_TOKEN_2
        }
    });
    test:assertTrue(wsClient is Error);
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 403 Forbidden");
    }
}

@test:Config {}
public function testBearerTokenAuthServiceAuthnFailure() {
    Client|Error wsClient = new("ws://localhost:21324/bearerTokenAuth/", {
        auth: {
            token: ACCESS_TOKEN_3
        }
    });
    test:assertTrue(wsClient is Error);
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 401 Unauthorized");
    }
}

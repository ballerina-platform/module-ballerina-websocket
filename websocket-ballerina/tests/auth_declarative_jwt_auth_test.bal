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

listener Listener l51 = new(21320);
string wsService51Data = "";

@ServiceConfig {
    auth: [
        {
            jwtValidatorConfig: {
                issuer: "ballerina",
                audience: ["ballerina", "ballerina.org", "ballerina.io"],
                signatureConfig: {
                    trustStoreConfig: {
                        trustStore: {
                            path: KEYSTORE_PATH,
                            password: "ballerina"
                        },
                        certAlias: "ballerina"
                    }
                },
                scopeKey: "scp"
            },
            scopes: ["write", "update"]
        }
    ]
}
service /jwtAuth on l51 {
    resource function get .() returns Service {
        return new WsService51();
    }
}

service class WsService51 {
    *Service;
    remote function onTextMessage(string data) {
        wsService51Data = data;
    }
}

@test:Config {}
public function testJwtAuthServiceAuthSuccess() returns Error? {
    Client wsClient = check new("ws://localhost:21320/jwtAuth/", {
        auth: {
            username: "wso2",
            issuer: "ballerina",
            audience: ["ballerina", "ballerina.org", "ballerina.io"],
            keyId: "5a0b754-895f-4279-8843-b745e11a57e9",
            customClaims: { "scp": "write" },
            expTime: 3600,
            signatureConfig: {
                config: {
                    keyAlias: "ballerina",
                    keyPassword: "ballerina",
                    keyStore: {
                        path: KEYSTORE_PATH,
                        password: "ballerina"
                    }
                }
            }
        }
    });
    check wsClient->writeTextMessage("Hello, World!");
    runtime:sleep(0.5);
    test:assertEquals(wsService51Data, "Hello, World!");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testJwtAuthServiceAuthzFailure() {
    Client|Error wsClient = new("ws://localhost:21320/jwtAuth/", {
        auth: {
            username: "wso2",
            issuer: "ballerina",
            audience: ["ballerina", "ballerina.org", "ballerina.io"],
            keyId: "5a0b754-895f-4279-8843-b745e11a57e9",
            customClaims: { "scp": "read" },
            expTime: 3600,
            signatureConfig: {
                config: {
                    keyAlias: "ballerina",
                    keyPassword: "ballerina",
                    keyStore: {
                        path: KEYSTORE_PATH,
                        password: "ballerina"
                    }
                }
            }
        }
    });
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 403 Forbidden");
    } else {
        test:assertFail(msg = "Test Failed!");
        error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
    }
}

@test:Config {}
public function testJwtAuthServiceAuthnFailure() {
    Client|Error wsClient = new("ws://localhost:21320/jwtAuth/", {
        auth: {
            username: "wso2",
            issuer: "wso2",
            audience: ["ballerina", "ballerina.org", "ballerina.io"],
            keyId: "5a0b754-895f-4279-8843-b745e11a57e9",
            customClaims: { "scp": "write" },
            expTime: 3600,
            signatureConfig: {
                config: {
                    keyAlias: "ballerina",
                    keyPassword: "ballerina",
                    keyStore: {
                        path: KEYSTORE_PATH,
                        password: "ballerina"
                    }
                }
            }
        }
    });
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 401 Unauthorized");
    } else {
        test:assertFail(msg = "Test Failed!");
        error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
    }
}

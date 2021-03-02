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

import ballerina/http;
import ballerina/jwt;
import ballerina/lang.runtime as runtime;
import ballerina/test;

listener Listener l51 = new(21320);
string strSyncData = "";

http:JwtValidatorConfig jwtConfig = {
        issuer: "ballerina",
        audience: ["ballerina", "ballerina.org", "ballerina.io"],
        signatureConfig: {
            trustStoreConfig: {
                trustStore: {
                    path: "tests/certsAndKeys/ballerinaKeystore.p12",
                    password: "ballerina"
                },
                certAlias: "ballerina"
            }
        },
        scopeKey: "scp"
    };

http:ListenerJwtAuthHandler handler = new(jwtConfig);

service /jwtSyncAuthService on l51 {
    resource function get .(http:Request req) returns Service|UpgradeError {
        jwt:Payload|http:Unauthorized authn1 = handler.authenticate(req);
        if (authn1 is jwt:Payload) {
            return new WsService51();
        } else {
            return error UpgradeError("Authentication failed");
        }
    }
}

service class WsService51 {
    *Service;
    remote function onTextMessage(Caller caller, string data) returns Error? {
        strSyncData = data;
    }
}

@test:Config {}
public function testSyncJwtAuth() returns Error? {
    Client wsClient = check new("ws://localhost:21320/jwtSyncAuthService/", config = {
            auth: {
                    username: "wso2",
                    issuer: "ballerina",
                    audience: ["ballerina", "ballerina.org", "ballerina.io"],
                    keyId: "5a0b754-895f-4279-8843-b745e11a57e9",
                    customClaims: { "scp": "hello" },
                    expTimeInSeconds: 3600,
                    signatureConfig: {
                        config: {
                            keyAlias: "ballerina",
                            keyPassword: "ballerina",
                            keyStore: {
                                path: "tests/certsAndKeys/ballerinaKeystore.p12",
                                password: "ballerina"
                            }
                        }
                    }
                }
            });
    check wsClient->writeTextMessage("Authentication successful");
    runtime:sleep(0.5);
    test:assertEquals(strSyncData, "Authentication successful");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/http;

listener Listener l65 = new(21065, {
                secureSocket: {
                    key: {
                        path: "tests/certsAndKeys/ballerinaKeystore.p12",
                        password: "ballerina"
                    },
                    mutualSsl: {
                        verifyClient: http:REQUIRE,
                        cert: {
                            path: "tests/certsAndKeys/ballerinaTruststore.p12",
                            password: "ballerina"
                        }
                    },
                    protocol: {
                        name: http:TLS,
                        versions: ["TLSv1.2","TLSv1.1"]
                    },
                    ciphers:["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
                    handshakeTimeout: 20,
                    sessionTimeout: 200
                }
            });

service /sslTest on l65 {
    resource function get .(http:Request req) returns Service {
        return new SslService();
    }
}

service class SslService {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) {
        var returnVal = caller->writeTextMessage(data);
        if (returnVal is Error) {
            panic <error>returnVal;
        }
    }
}

// Tests the successful connection of sync client over mutual SSL
@test:Config {}
public function testMutualSslWithKeyStores() returns Error? {
    Client|Error wsClient = new("wss://localhost:21065/sslTest", config = {
                       secureSocket: {
                           key:{
                               path: "tests/certsAndKeys/ballerinaKeystore.p12",
                               password: "ballerina"
                           },
                           cert: {
                               path: "tests/certsAndKeys/ballerinaTruststore.p12",
                               password: "ballerina"
                           },
                           protocol:{
                               name: http:TLS,
                               versions: ["TLSv1.2", "TLSv1.1"]
                           },
                           ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
                           handshakeTimeout: 20,
                           sessionTimeout: 200
                       }
                   });
    if (wsClient is Error) {
        test:assertFail("Expected a successful mTLS connection");
    } else {
        test:assertEquals(wsClient.isSecure(), true);
    }
}

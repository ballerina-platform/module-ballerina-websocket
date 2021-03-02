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
import ballerina/lang.runtime as runtime;
import ballerina/test;

listener Listener l55 = new(21325);
string oauthHeader = "";

service /oauthService on l55 {
    resource function get .(http:Request req) returns Service|UpgradeError {
        string|error header = req.getHeader("Authorization");
        if (header is string) {
            oauthHeader = header;
            return new WsService55();
        } else {
            authHeader = "Header not found";
            return error UpgradeError("Authentication failed");
        }
    }
}

service class WsService55 {
    *Service;
    remote function onTextMessage(Client caller, string data) returns Error? {
    }
}

OAuth2ClientCredentialsGrantConfig config1 = {
    tokenUrl: "https://localhost:9401/oauth2/token",
    clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
    clientSecret: "9205371918321623741",
    scopes: ["token-scope1", "token-scope2"],
    clientConfig: {
        secureSocket: {
           trustStore: {
               path: "tests/certsAndKeys/ballerinaTruststore.p12",
               password: "ballerina"
           }
        }
    }
};

OAuth2PasswordGrantConfig config2 = {
    tokenUrl: "https://localhost:9401/oauth2/token",
    username: "johndoe",
    password: "A3ddj3w",
    clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
    clientSecret: "9205371918321623741",
    scopes: ["token-scope1", "token-scope2"],
    clientConfig: {
        secureSocket: {
           trustStore: {
               path: "tests/certsAndKeys/ballerinaTruststore.p12",
               password: "ballerina"
           }
        }
    }
};

OAuth2DirectTokenConfig config3 = {
    refreshUrl: "https://localhost:9401/oauth2/token/refresh",
    refreshToken: "XlfBs91yquexJqDaKEMzVg==",
    clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
    clientSecret: "9205371918321623741",
    scopes: ["token-scope1", "token-scope2"],
    clientConfig: {
        secureSocket: {
           trustStore: {
               path: "tests/certsAndKeys/ballerinaTruststore.p12",
               password: "ballerina"
           }
        }
    }
};

@test:Config {}
public function testOAuth2ClientCredentialsGrant() returns Error? {
    Client wsClient = check new("ws://localhost:21325/oauthService/", config = {auth: config1});
    runtime:sleep(0.5);
    test:assertEquals(oauthHeader, "Bearer 2YotnFZFEjr1zCsicMWpAA");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

@test:Config {}
public function testOAuth2PasswordGrant() returns Error? {
    Client wsClient = check new("ws://localhost:21325/oauthService/", config = {auth: config2});
    runtime:sleep(0.5);
    test:assertEquals(oauthHeader, "Bearer 2YotnFZFEjr1zCsicMWpAA");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

@test:Config {}
public function testOAuth2DirectToken() returns Error? {
    Client wsClient = check new("ws://localhost:21325/oauthService/", config = {auth: config3});
    runtime:sleep(0.5);
    test:assertEquals(oauthHeader, "Bearer 2YotnFZFEjr1zCsicMWpAA");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

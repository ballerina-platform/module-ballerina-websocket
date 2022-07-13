// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

// NOTE: All the tokens/credentials used in this test are dummy tokens/credentials and used only for testing purposes.

import ballerina/http;
import ballerina/regex;

const string KEYSTORE_PATH = "tests/certsAndKeys/ballerinaKeystore.p12";
const string TRUSTSTORE_PATH = "tests/certsAndKeys/ballerinaTruststore.p12";

const string ACCESS_TOKEN_1 = "2YotnFZFEjr1zCsicMWpAA";
const string ACCESS_TOKEN_2 = "1zCsicMWpAA2YotnFZFEjr";
const string ACCESS_TOKEN_3 = "invalid-token";

type AuthResponse record {|
    *http:Ok;
    json body?;
|};

// The mock authorization server, based with https://hub.docker.com/repository/docker/ldclakmal/ballerina-sts
listener http:Listener sts = new(9445, {
    secureSocket: {
        key: {
            path: KEYSTORE_PATH,
            password: "ballerina"
        }
    }
});

service /oauth2 on sts {
    resource function post token() returns AuthResponse {
        return { 
            body: {
                "access_token": ACCESS_TOKEN_1,
                "token_type": "example",
                "expires_in": 3600,
                "example_parameter": "example_value"
            }    
        };
    }

    resource function post introspect(http:Request request) returns AuthResponse {
        string|http:ClientError payload = request.getTextPayload();
        if payload is string {
            string[] parts = regex:split(payload, "&");
            foreach string part in parts {
                if part.indexOf("token=") is int {
                    string token = regex:split(part, "=")[1];
                    if token == ACCESS_TOKEN_1 {
                        return { body: {"active": true, "exp": 3600, "scp": "write update" }};
                    } else if token == ACCESS_TOKEN_2 {
                        return { body: { "active": true, "exp": 3600, "scp": "read" }};
                    } else {
                        return {body: { "active": false }};
                    }
                }
            }
        }
        return {};
    }
}

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

import ballerina/http;

const string ACCESS_TOKEN = "2YotnFZFEjr1zCsicMWpAA";

// Mock OAuth2 authorization server implementation, which treats the APIs with successful responses.
listener http:Listener oauth2Listener = new(9401, {
    secureSocket: {
        key: {
            path: "tests/certsAndKeys/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

service /oauth2 on oauth2Listener {
    resource function post token() returns json {
        json response = {
            "access_token": ACCESS_TOKEN,
            "token_type": "example",
            "expires_in": 3600,
            "example_parameter": "example_value"
        };
        return response;
    }

    resource function post token/refresh() returns json {
        json response = {
            "access_token": ACCESS_TOKEN,
            "token_type": "example",
            "expires_in": 3600,
            "example_parameter": "example_value"
        };
        return response;
    }
}

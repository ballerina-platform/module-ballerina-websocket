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

string authHeader = "";

listener Listener l53 = new(21321);

service /bearerTokenService on l53 {
    resource function get .(http:Request req) returns Service|UpgradeError {
        string|error header = req.getHeader("Authorization");
        if (header is string) {
            authHeader = header;
            return new WsService53();
        } else {
            authHeader = "Header not found";
            return error UpgradeError("Authentication failed");
        }
    }
}

service class WsService53 {
    *Service;
    remote function onTextMessage(Caller caller, string data) returns Error? {
    }
}

@test:Config {}
public function testAsyncBearerToken() returns Error? {
    AsyncClient wsClient = check new("ws://localhost:21321/bearerTokenService/", config = {
            auth: {
              token: "JlbmMiOiJBMTI4Q0JDLUhTMjU2In"
            }
        });
    runtime:sleep(0.5);
    test:assertEquals(authHeader, "Bearer JlbmMiOiJBMTI4Q0JDLUhTMjU2In");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

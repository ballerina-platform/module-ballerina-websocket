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
import ballerina/http;

listener Listener l48 = new(21317);

service /basicAuthService on l48 {
   resource function get .(http:Request req) returns Service|UpgradeError {
       string|error header = req.getHeader("Authorization");
       if (header is string) {
           authHeader = header;
       } else {
           authHeader = "Header not found";
       }
       return new WsService48();
   }
}

service class WsService48 {
  *Service;
  remote function onTextMessage(Caller caller, string data) returns Error? {
  }
}

@test:Config {}
public function testAsyncBasicAuth() returns Error? {
   AsyncClient wsClient = check new("ws://localhost:21317/basicAuthService/", config = {
            auth: {
                username: "alice",
                password: "123"
            }
        });
   runtime:sleep(0.5);
   test:assertEquals(authHeader, "Basic YWxpY2U6MTIz");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

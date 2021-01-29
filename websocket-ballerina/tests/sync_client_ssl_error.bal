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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/http;

string sslErrString = "";
listener Listener l36 = new(21058);

service /sslTest on l36 {
   resource function get .(http:Request req) returns Service {
       return new SyncSslErrorService();
   }
}

service class SyncSslErrorService {
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) {
       var returnVal = caller->writeTextMessage(data);
       if (returnVal is Error) {
           panic <error>returnVal;
       }
   }
}

// Tests the Ssl error returned when creating the sync client
@test:Config {}
public function testSyncClientSslError() {
   Client|Error wsClient = new("wss://localhost:21058/sslTest", config = {
                       secureSocket: {
                           trustStore: {
                               path: "tests/certsAndKeys/ballerinaTruststore.p12",
                               password: "ballerina"
                           }
                       }
                   });
   if (wsClient is Error) {
      sslErrString = wsClient.message();
   }
   test:assertEquals(sslErrString, "GenericError: SSL/TLS Error");
   runtime:sleep(3);
}

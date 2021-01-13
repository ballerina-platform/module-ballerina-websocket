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

import ballerina/lang.runtime as runtime;
import ballerina/test;

string expectedErr = "";
service class errorHandlingService {
   *Service;
   remote function onError(Caller caller, error err) {
       expectedErr = <@untainted>err.toString();
   }
}

// Tests the client initialization failing in a resource.
@test:Config {}
public function testClientEndpointFailureInResource() {
   AsyncClient wsClientEp = new ("ws://localhost:21010/websocketxyz", new errorHandlingService(), {
           readyOnConnect: false
       });
   var err = wsClientEp->ready();
   if (err is Error) {
       test:assertEquals(err.message(), "ConnectionError: The WebSocket connection has not been made");
   } else {
       test:assertFail("Couldn't find the expected output");
   }
   runtime:sleep(0.5);
   test:assertEquals(expectedErr, "error(\"ConnectionError: IO Error\")");
}

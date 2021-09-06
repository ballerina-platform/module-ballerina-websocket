// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
////www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.runtime as runtime;
import ballerina/test;

string errorMsg2 = "";
listener Listener l20 = new(21008);
service /pushTextFailureService on l20 {
   resource isolated function get .() returns Service|UpgradeError {
       return new PushTextFailureService();
   }
}

service class PushTextFailureService {
   *Service;
   remote function onOpen(Caller caller) {
       Error? err1 = caller->close(timeout = 0);
       var err = caller->writeTextMessage("hey");
       if (err is Error) {
           errorMsg2 = <@untainted>err.message();
       }
   }
}

// Checks for the log that is printed when writeTextMessage fails.
@test:Config {}
public function pushTextFailure() returns Error? {
   Client wsClient = check new("ws://localhost:21008/pushTextFailureService");
   runtime:sleep(0.5);
   test:assertEquals(errorMsg2, "ConnectionClosureError: Close frame already sent. Cannot push text data!",
       msg = "Data mismatched");
}

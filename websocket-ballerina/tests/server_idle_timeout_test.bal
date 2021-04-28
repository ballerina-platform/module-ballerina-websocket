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

listener Listener l69 = new(21069);
listener Listener l70 = new(21070);

string timeOutString = "";

@ServiceConfig {
   idleTimeout: 2
}
service /idleTimeoutError on l69 {
    resource function get .() returns Service|UpgradeError {
        return new WsService69();
    }
}

service class WsService69 {
    *Service;
    remote function onIdleTimeout(Caller caller) {
        timeOutString = "timeOut occured";
    }
}

// Tests idle time out remote function in server
@test:Config {}
public function testServerIdletimeout() returns Error? {
    Client wsClient = check new("ws://localhost:21069/idleTimeoutError/");
    runtime:sleep(4);
    Error? res = wsClient->writeTextMessage("Hi");
    test:assertEquals(timeOutString, "timeOut occured");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testDetachError() returns error? {
    Service dummyService = service object {
           isolated remote function onTextMessage(Caller caller)  {

           }
       };
    check l70.gracefulStop();
    error? detachedRes = l70.detach(dummyService);
    if (detachedRes is error) {
        test:assertEquals(detachedRes.message(), "Error: Cannot detach service. Service has not been registered");
    } else {
        test:assertFail("Expected a detaching error");
    }
    error? immStop = l70.immediateStop();
    if (immStop is error) {
        test:assertEquals(immStop.message(), "not implemented");
    } else {
        test:assertFail("Expected an error");
    }
}

@test:Config {}
public function testConnectionRefused() returns Error? {
    Error|Client wsClient = new("ws://localhost:21071/idleTimeoutError/");
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "ConnectionError: IO Error");
    } else {
        test:assertFail("Expected a connection refused error");
    }
}

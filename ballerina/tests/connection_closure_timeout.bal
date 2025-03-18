//  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
//
//  WSO2 LLC. licenses this file to you under the Apache License,
//  Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License.
//  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied. See the License for the
//  specific language governing permissions and limitations
//  under the License.

import ballerina/lang.runtime;
import ballerina/test;

listener Listener connectionClosureTimeoutListener = new (22100);

@ServiceConfig {
    connectionClosureTimeout: 10
}
service / on connectionClosureTimeoutListener {
    resource function get .() returns Service|UpgradeError {
        return new ConnectionClosureTimeoutService();
    }
}

service class ConnectionClosureTimeoutService {
    *Service;

    remote function onMessage(Caller caller, string data) returns error? {
        _ = start caller->close();
        runtime:sleep(1);
        test:assertTrue(caller.isOpen());
        runtime:sleep(10);
        test:assertTrue(!caller.isOpen());
    }
}

@test:Config {
    groups: ["connectionClosureTimeout"]
}
public function testConnectionClosureTimeoutFromServer() returns Error? {
    Client wsClient = check new ("ws://localhost:22100/");
    check wsClient->writeMessage("Hi");
    runtime:sleep(20);
}

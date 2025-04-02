//  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

map<Caller> callers = {};

@ServiceConfig {
    dispatcherKey: "event",
    connectionClosureTimeout: 5
}
service / on connectionClosureTimeoutListener {
    resource function get .() returns Service|UpgradeError {
        return new ConnectionClosureTimeoutService();
    }
}

service class ConnectionClosureTimeoutService {
    *Service;

    remote function onSubscribe(Caller caller) returns error? {
        callers["onSubscribe"] = caller;
        _ = start caller->close();
    }

    remote function onChat(Caller caller) returns NormalClosure? {
        callers["onChat"] = caller;
        return NORMAL_CLOSURE;
    }

    remote function onIsClosed(record {string event; string name;} data) returns boolean|error {
        Caller? caller = callers[data.name];
        if caller is Caller {
            return !caller.isOpen();
        }
        return error("Caller not found");
    }
}

@test:Config {
    groups: ["connectionClosureTimeout"]
}
public function testConnectionClosureTimeoutCaller() returns error? {
    Client wsClient1 = check new ("ws://localhost:22100/");
    check wsClient1->writeMessage({event: "subscribe"});
    runtime:sleep(8);

    // Check if the connection is closed using another client
    Client wsClient2 = check new ("ws://localhost:22100/");
    check wsClient2->writeMessage({event: "is_closed", name: "onSubscribe"});
    boolean isClosed = check wsClient2->readMessage();
    test:assertTrue(isClosed);
}

@test:Config {
    groups: ["connectionClosureTimeout"]
}
public function testConnectionClosureTimeoutCloseFrames() returns error? {
    Client wsClient1 = check new ("ws://localhost:22100/");
    check wsClient1->writeMessage({event: "chat"});
    runtime:sleep(8);

    // Check if the connection is closed using another client
    Client wsClient2 = check new ("ws://localhost:22100/");
    check wsClient2->writeMessage({event: "is_closed", name: "onChat"});
    boolean isClosed = check wsClient2->readMessage();
    test:assertTrue(isClosed);
}

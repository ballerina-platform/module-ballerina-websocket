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

import ballerina/test;

listener Listener l102 = new (22081);
listener Listener l103 = new (22082);
listener Listener l104 = new (22083);
listener Listener l105 = new (22084);
listener Listener l106 = new (22085);
listener Listener l107 = new (22086);
listener Listener l108 = new (22087);

service /onCloseFrame on l102 {
    resource function get .() returns Service|UpgradeError {
        return new WsService102();
    }
}

service /onCloseFrame on l103 {
    resource function get .() returns Service|UpgradeError {
        return new WsService103();
    }
}

service /onCloseFrame on l104 {
    resource function get .() returns Service|UpgradeError {
        return new WsService104();
    }
}

service /onCloseFrame on l105 {
    resource function get .() returns Service|UpgradeError {
        return new WsService105();
    }
}

service /onCloseFrame on l106 {
    resource function get .() returns Service|UpgradeError {
        return new WsService106();
    }
}

service /onCloseFrame on l107 {
    resource function get .() returns Service|UpgradeError {
        return new WsService107();
    }
}

service /onCloseFrame on l108 {
    resource function get .() returns Service|UpgradeError {
        return new WsService108();
    }
}

service class WsService102 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return NORMAL_CLOSURE;
    }
}

service class WsService103 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return GOING_AWAY;
    }
}

service class WsService104 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return UNSUPPORTED_DATA;
    }
}

service class WsService105 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return INVALID_PAYLOAD;
    }
}

service class WsService106 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return POLICY_VIOLATION;
    }
}

service class WsService107 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return MESSAGE_TOO_BIG;
    }
}

service class WsService108 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return INTERNAL_SERVER_ERROR;
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testNormalClosure() returns Error? {
    Client wsClient = check new ("ws://localhost:22081/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Connection closed Status code: 1000");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testGoingAway() returns Error? {
    Client wsClient = check new ("ws://localhost:22082/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Connection closed Status code: 1001");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testUnsupportedData() returns Error? {
    Client wsClient = check new ("ws://localhost:22083/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Endpoint received unsupported frame: Status code: 1003");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testInvalidPayload() returns Error? {
    Client wsClient = check new ("ws://localhost:22084/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Payload does not match the expected format or encoding: Status code: 1007");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testPolicyViolation() returns Error? {
    Client wsClient = check new ("ws://localhost:22085/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Received message violates its policy: Status code: 1008");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testMessageTooBig() returns Error? {
    Client wsClient = check new ("ws://localhost:22086/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "The received message exceeds the allowed size limit: Status code: 1009");
    }
}

@test:Config {
    groups: ["closeFrame"]
}
public function testInternalServerError() returns Error? {
    Client wsClient = check new ("ws://localhost:22087/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Internal server error occurred: Status code: 1011");
    }
}

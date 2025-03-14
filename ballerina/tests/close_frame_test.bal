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

const decimal sleepingInterval = 1;

type Heartbeat record {|
    string event;
|};

listener Listener l102 = new (22082);
listener Listener l103 = new (22083);
listener Listener l104 = new (22084);
listener Listener l105 = new (22085);
listener Listener l106 = new (22086);
listener Listener l107 = new (22087);
listener Listener l108 = new (22088);
listener Listener l109 = new (22089);
listener Listener l110 = new (22090);
listener Listener l111 = new (22091);
listener Listener l112 = new (22092);
listener Listener l113 = new (22093);
listener Listener l114 = new (22094);

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

service /onCloseFrame on l109 {
    resource function get .() returns Service|UpgradeError {
        return new WsService109();
    }
}

service /onCloseFrame on l110 {
    resource function get .() returns Service|UpgradeError {
        return new WsService110();
    }
}

@ServiceConfig {
    dispatcherKey: "event"
}
service /onCloseFrame on l111 {
    resource function get .() returns Service|UpgradeError {
        return new WsService111();
    }
}

service /onCloseFrame on l112 {
    resource function get .() returns Service|UpgradeError {
        return new WsService112();
    }
}

@ServiceConfig {
    dispatcherKey: "event"
}
service /onCloseFrame on l113 {
    resource function get .() returns Service|UpgradeError {
        return new WsService113();
    }
}

@ServiceConfig {
    idleTimeout: 5
}
service /onCloseFrame on l114 {
    resource function get .() returns Service|UpgradeError {
        return new WsService114();
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
        return PROTOCOL_ERROR;
    }
}

service class WsService105 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return UNSUPPORTED_DATA;
    }
}

service class WsService106 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return INVALID_PAYLOAD;
    }
}

service class WsService107 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return POLICY_VIOLATION;
    }
}

service class WsService108 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return MESSAGE_TOO_BIG;
    }
}

service class WsService109 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return INTERNAL_SERVER_ERROR;
    }
}

service class WsService110 {
    *Service;

    remote function onMessage(Caller caller, string data) returns CloseFrame {
        return {status: 3555, reason: "Custom close frame message"};
    }
}

service class WsService111 {
    *Service;

    remote function onHeartbeat(Heartbeat data) returns CloseFrame {
        return NORMAL_CLOSURE;
    }
}

service class WsService112 {
    *Service;

    remote function onMessage(int data) {
        return;
    }

    remote function onError(error 'error) returns InvalidPayload {
        return INVALID_PAYLOAD;
    }
}

service class WsService113 {
    *Service;

    remote function onHeartbeat(int data) {
        return;
    }

    remote function onHeartbeatError(error 'error) returns InvalidPayload {
        return INVALID_PAYLOAD;
    }
}

service class WsService114 {
    *Service;

    remote function onIdleTimeout() returns NormalClosure {
        return NORMAL_CLOSURE;
    }
}

public function getErrorMessage(CloseFrame closeFrame) returns string {
    string? reason = closeFrame.reason;
    if reason is string {
        return reason + ": Status code: " + closeFrame.status.toString();
    }
    return "Connection closed Status code: " + closeFrame.status.toString();
}

public function isConnectionClosed(Client wsClient) returns boolean {
    runtime:sleep(sleepingInterval);
    return !wsClient.isOpen();
}

@test:Config {
    groups: ["closeFrame"]
}
public function testNormalClosure() returns Error? {
    Client wsClient = check new ("ws://localhost:22082/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(NORMAL_CLOSURE));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testGoingAway() returns Error? {
    Client wsClient = check new ("ws://localhost:22083/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(GOING_AWAY));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testProtocolError() returns Error? {
    Client wsClient = check new ("ws://localhost:22084/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(PROTOCOL_ERROR));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testUnsupportedData() returns Error? {
    Client wsClient = check new ("ws://localhost:22085/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(UNSUPPORTED_DATA));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testInvalidPayload() returns Error? {
    Client wsClient = check new ("ws://localhost:22086/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(INVALID_PAYLOAD));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testPolicyViolation() returns Error? {
    Client wsClient = check new ("ws://localhost:22087/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(POLICY_VIOLATION));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testMessageTooBig() returns Error? {
    Client wsClient = check new ("ws://localhost:22088/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(MESSAGE_TOO_BIG));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testInternalServerError() returns Error? {
    Client wsClient = check new ("ws://localhost:22089/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(INTERNAL_SERVER_ERROR));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testCustomCloseFrame() returns Error? {
    Client wsClient = check new ("ws://localhost:22090/onCloseFrame");
    check wsClient->writeMessage("Hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), "Custom close frame message: Status code: 3555");
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testCustomDispatcher() returns Error? {
    Client wsClient = check new ("ws://localhost:22091/onCloseFrame");
    check wsClient->writeMessage({"event": "heartbeat"});
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(NORMAL_CLOSURE));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testOnErrorDispatcher() returns Error? {
    Client wsClient = check new ("ws://localhost:22092/onCloseFrame");
    check wsClient->writeMessage("hi");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(INVALID_PAYLOAD));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testOnCustomErrorDispatcher() returns Error? {
    Client wsClient = check new ("ws://localhost:22093/onCloseFrame");
    check wsClient->writeMessage({"event": "heartbeat", "name": "Hi"});
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(INVALID_PAYLOAD));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

@test:Config {
    groups: ["closeFrame"]
}
public function testOnIdleTimeout() returns Error? {
    Client wsClient = check new ("ws://localhost:22094/onCloseFrame");
    anydata|Error res = wsClient->readMessage();
    test:assertTrue(res is Error);
    if res is Error {
        test:assertEquals(res.message(), getErrorMessage(NORMAL_CLOSURE));
    }
    test:assertTrue(isConnectionClosed(wsClient));
}

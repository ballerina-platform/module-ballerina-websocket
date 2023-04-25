// Copyright (c) 2023 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
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

import ballerina/test;
import ballerina/io;

listener Listener customDispatchingLis = new(21401);

@ServiceConfig {dispatcherKey: "event"}
service on customDispatchingLis {
    resource function get .() returns Service|Error {
        return new SingleMessageService();
    }
}

service class SingleMessageService {
    *Service;
    remote function onPing(Caller caller, json data) returns Error? {
        io:println(data);
        check caller->writeMessage({"event": "pong"});
    }
}

@ServiceConfig {dispatcherKey: "event"}
service /subscribe on customDispatchingLis {
    resource function get .() returns Service|Error {
        return new SubscribeMessageService();
    }
}

service class SubscribeMessageService {
    *Service;
    remote function onSubscribe(Caller caller, json data) returns Error? {
        io:println(data);
        check caller->writeMessage({"type": "subscribe", "id":"1", "payload":{"query": "{ __schema { types { name } } }"}});
    }

    remote function onHeartbeat(Caller caller, json data) returns json {
        return {"event": "heartbeat"};
    }
}

@ServiceConfig {dispatcherKey: "event"}
service /onMessage on customDispatchingLis {
    resource function get .() returns Service|Error {
        return new DefaultRemoteFunctionService();
    }
}

service class DefaultRemoteFunctionService {
    *Service;
    remote function onMessage(Caller caller, json data) returns Error? {
        check caller->writeMessage({"event": "heartbeat"});
    }
}

@ServiceConfig {dispatcherKey: "event"}
service /noRemoteMethod on customDispatchingLis {
    resource function get .() returns Service|Error {
        return new NoRemoteFunctionService();
    }
}

service class NoRemoteFunctionService {
    *Service;
    remote function onMessages(Caller caller, json data) returns Error? {
        check caller->writeMessage({"event": "onMessages"});
    }
}

@ServiceConfig {dispatcherKey: "event"}
service /dataBindingFailure on customDispatchingLis {
 resource function get .() returns Service|Error {
     return new DataBindingFailureService();
 }
}

service class DataBindingFailureService {
 *Service;
 remote function onMessages(Caller caller, byte[] data) returns Error? {
     check caller->writeMessage({"event": "onMessages"});
 }
}

@ServiceConfig {dispatcherKey: "type"}
service /underscore on customDispatchingLis {
    resource function get .() returns Service|Error {
     return new UnderscoreService();
    }
}

service class UnderscoreService {
    *Service;
    remote function onPing(Caller caller, json data) returns Error? {
     check caller->writeMessage({"event": "onMessages"});
    }
}

@ServiceConfig {dispatcherKey: "type"}
service /lotofunderscores on customDispatchingLis {
    resource function get .() returns Service|Error {
     return new LotOfUnderscoreService();
    }
}

service class LotOfUnderscoreService {
    *Service;
    remote function onThisPingMessage(Caller caller, json data) returns Error? {
     check caller->writeMessage({"event": "onMessages"});
    }
}

@ServiceConfig {dispatcherKey: "type"}
service /onMessageWithCustom on customDispatchingLis {
    resource function get .() returns Service|Error {
        return new OnMessageService();
    }
}

service class OnMessageService {
    *Service;
    remote function onMessage(Caller caller, json data) returns Error? {
        check caller->writeMessage({"type": "onMessage"});
    }

    remote function onConnectionInit(Caller caller, string text) returns Error? {
        check caller->writeMessage({"type": "onConnectionInit"});
    }
}

@test:Config {}
public function testPingMessage() returns Error? {
    Client cl = check new("ws://localhost:21401");
    check cl->writeMessage({"event": "ping"});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"event": "pong"});
}

@test:Config {}
public function testSubscribeMessage() returns Error? {
    Client cl = check new("ws://localhost:21401/subscribe");
    check cl->writeMessage({"event": "subscribe", "pair": ["XBT/USD", "XBT/EUR"], "subscription": {"name": "ticker"}});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"type": "subscribe", "id":"1", "payload":{"query": "{ __schema { types { name } } }"}});
    check cl->writeMessage({"event": "heartbeat"});
    json resp2 = check cl->readMessage();
    test:assertEquals(resp2, {"event": "heartbeat"});
}

@test:Config {}
public function testDispatchingToDefaultRemoteMethod() returns Error? {
    Client cl = check new("ws://localhost:21401/onMessage");
    check cl->writeMessage({"event": "heartbeat"});
    check cl->writeMessage({"event": "heartbeat"});
    json resp2 = check cl->readMessage();
    test:assertEquals(resp2, {"event": "heartbeat"});
}

@test:Config {}
public function testDispatchingToNone() returns Error? {
    Client cl = check new("ws://localhost:21401/noRemoteMethod");
    check cl->writeMessage({"event": "heartbeat"});
    check cl->writeMessage({"event": "Messages"});
    json resp2 = check cl->readMessage();
    test:assertEquals(resp2, {"event": "onMessages"});
}

@test:Config {}
public function testDatabindingFailure() returns Error? {
    Client cl = check new("ws://localhost:21401/dataBindingFailure");
    check cl->writeMessage({"event": "Messages"});
    json|Error resp2 = cl->readMessage();
    if resp2 is Error {
        test:assertTrue(resp2.message().startsWith("data binding failed:"));
    } else {
        test:assertFail("Expected a binding error");
    }
}

@test:Config {}
public function testUnderscore() returns Error? {
    Client cl = check new("ws://localhost:21401/underscore");
    check cl->writeMessage({"type": "_ping"});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"event": "onMessages"});
}

@test:Config {}
public function testUnderscoresAndSpaces() returns Error? {
    Client cl = check new("ws://localhost:21401/lotofunderscores");
    check cl->writeMessage({"type": "this_ping message"});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"event": "onMessages"});
}

@test:Config {}
public function testOnMessageAtTheBeginning() returns Error? {
    Client cl = check new("ws://localhost:21401/onMessageWithCustom");
    check cl->writeMessage({'type: "connection_init"});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"type": "onConnectionInit"});
}

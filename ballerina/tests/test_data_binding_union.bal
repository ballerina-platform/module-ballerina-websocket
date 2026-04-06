// Copyright (c) 2023 WSO2 LLC. (www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

type ConnectionInitMessage record {|
    WS_INIT 'type;
    map<json> payload?;
|};

type PingMessage record {|
    WS_PING 'type;
    map<json> payload?;
|};

type PongMessage record {|
    WS_PONG 'type;
    map<json> payload?;
|};

type SubscribeMessage record {|
    string id;
    WS_SUBSCRIBE 'type;
    record {|
        string operationName?;
        string query;
        map<json> variables?;
        map<json> extensions?;
    |} payload;
|};

type CompleteMessage record {|
    string id;
    WS_COMPLETE 'type;
|};

const WS_PING = "ping";
const WS_PONG = "pong";
const WS_SUBSCRIBE = "subscribe";
const WS_COMPLETE = "complete";
const WS_INIT = "init";

type Message ConnectionInitMessage|PingMessage|PongMessage|SubscribeMessage|CompleteMessage;

service / on new Listener(20010) {

    resource function get .() returns Service {
        return new MyService();
    }
}

service class MyService {
    *Service;

    remote function onMessage(Caller caller, Message message) returns error? {
        if message is PingMessage {
           check self.handlePingMessage(caller, message);
        }
    }

    private function handlePingMessage(Caller caller, PingMessage ping) returns error? {
        check caller->writeMessage({'type: WS_PONG});
    }
}

@test:Config {}
public function testUnionAsBTypeReferenceType() returns Error? {
    Client chatClient = check new ("ws://localhost:20010");
    check chatClient->writeMessage({'type: WS_PING});
    PongMessage message = check chatClient->readMessage();
    test:assertEquals(message, {"type":"pong"});
}

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

import ballerina/test;

type Subscribe record {|
    string event = "subscribe";
    string data;
|};

@ServiceConfig {
    dispatcherKey: "event"
}
service / on new Listener(22103) {
    resource function get .() returns Service|UpgradeError {
        return new WsService22103();
    }
}

service class WsService22103 {
    *Service;

    @DispatcherConfig {
        payloadType: "subscribe"
    }
    remote function onSubscribeMessage(Subscribe message) returns string {
        return "onSubscribeMessage";
    }

    remote function onSubscribeMessageError(Caller caller, error message) returns error? {
        check caller->writeMessage("onSubscribeMessageError");
    }
}

@test:Config {
    groups: ["dispatcherConfigAnnotation"]
}
public function testDispatcherConfigAnnotation() returns error? {
    Client wsClient = check new ("ws://localhost:22103/");
    check wsClient->writeMessage({event: "subscribe", data: "test"});
    string res = check wsClient->readMessage();
    test:assertEquals(res, "onSubscribeMessage");
}

@test:Config {
    groups: ["dispatcherConfigAnnotation"]
}
public function testDispatcherConfigAnnotationWithCustomOnError() returns error? {
    Client wsClient = check new ("ws://localhost:22103/");
    check wsClient->writeMessage({event: "subscribe", invalidField: "test"});
    string res = check wsClient->readMessage();
    test:assertEquals(res, "onSubscribeMessageError");
}

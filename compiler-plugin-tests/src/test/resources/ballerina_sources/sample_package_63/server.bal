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

import ballerina/websocket;

type Subscribe record {|
    string event = "subscribe";
    string data;
|};

@websocket:ServiceConfig {
    dispatcherKey: "event"
}
service / on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;

    remote function onSubscribe(Subscribe message) returns string {
        return "onSubscribe";
    }

    @websocket:DispatcherConfig {
        value: "subscribe"
    }
    remote function onSubscribeMessage(Subscribe message) returns string {
        return "onSubscribeMessage";
    }

    @websocket:DispatcherConfig {
        value: "subscribe"
    }
    remote function onSubscribeText(Subscribe message) returns string {
        return "onSubscribeText";
    }
}

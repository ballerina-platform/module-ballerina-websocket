// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/io;
import ballerina/log;
import ballerina/websocket;

@websocket:WebSocketServiceConfig {
    path: "/error/ws"
}
service errorService on new websocket:Listener(21013) {
    resource function onOpen(websocket:WebSocketCaller ep) {
        log:printInfo("connection open");
    }

    resource function onText(websocket:WebSocketCaller ep, string text) {
        log:printError(string `text received: ${text}`);
        var returnVal = ep->pushText(text);
        if (returnVal is websocket:WebSocketError) {
            panic <error>returnVal;
        }
    }

    resource function onError(websocket:WebSocketCaller ep, error err) {
        io:println(err.message());
    }

    resource function onClose(websocket:WebSocketCaller ep, int statusCode, string reason) {
        log:printError(string `Connection closed with ${statusCode}, ${reason}`);
    }
}

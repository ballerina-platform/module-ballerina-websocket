// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/websocket;

final string USERNAME = "username";
map<websocket:Caller> connectionsMap = {};

service /chat on new websocket:Listener(9090) {
    resource function get [string username]() returns websocket:Service|websocket:UpgradeError {
        if (username != "") {
            // Server can accept a WebSocket connection by returning a `websocket:Service`.
            return new ChatServer(username);
        } else {
            // Server can cancel the WebSocket upgrade by returning an error.
            websocket:UpgradeError err = error("Username must be a non-empty value");
            return err;
        }
    }
}

service class ChatServer {
    *websocket:Service;

    string username;

    public function init(string username) {
        self.username = username;
    }

    remote function onOpen(websocket:Caller caller) returns websocket:Error? {
        string welcomeMsg = "Hi " + self.username + "! You have successfully connected to the chat";
        websocket:Error? err = check caller->writeTextMessage(welcomeMsg);
        string msg = self.username + " connected to chat";
        broadcast(msg);
        caller.setAttribute(USERNAME, self.username);
        lock {
            connectionsMap[caller.getConnectionId()] = caller;
        }
    }

    remote function onTextMessage(websocket:Caller caller, string text) {
        string msg = getAttributeStr(caller, USERNAME) + ": " + text;
        io:println(msg);
        @strand {
            thread:"any"
        }
        worker broadcast returns error? {
            broadcast(msg);
        }
    }

    remote function onClose(websocket:Caller caller, int statusCode, string reason) {
        lock {
            _ = connectionsMap.remove(caller.getConnectionId());
        }
        string msg = getAttributeStr(caller, USERNAME) + " left the chat";
        broadcast(msg);
    }
}

// Function to perform the broadcasting of text messages.
function broadcast(string text) {
    foreach websocket:Caller con in connectionsMap {
        websocket:Error? err = con->writeTextMessage(text);
        if err is websocket:Error {
            io:println("Error sending message to the :" + getAttributeStr(con, USERNAME) +
                        ". Reason: " + err.message());
        }
    }
}

function getAttributeStr(websocket:Caller ep, string key) returns string {
    var name = ep.getAttribute(key);
    if name is string {
        return name;
    }
    return "";
}

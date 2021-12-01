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

import ballerina/http;
import ballerina/io;
import ballerina/websocket;

listener websocket:Listener taxiMgtListener = new websocket:Listener(9091);
isolated map<string> driversMap = {};
isolated map<websocket:Caller> clientsMap = {};

// This service is for drivers to register and send locations.
service /taxi on taxiMgtListener {
    resource isolated function get [string name]() returns websocket:Service|websocket:UpgradeError {
        return new DriverService(name);
    }
}

isolated service class DriverService {
    *websocket:Service;

    final string driverName;

    public isolated function init(string username) {
        self.driverName = username;
    }

    remote isolated function onOpen(websocket:Caller caller) returns websocket:Error? {
        string welcomeMsg = "Hi " + self.driverName + "! Your location will be shared with the riders";
        check caller->writeTextMessage(welcomeMsg);
        lock {
            driversMap[caller.getConnectionId()] = self.driverName;
        }
        broadcast("Driver " + self.driverName + " ready for a ride");
    }

    // 'onTextMessage' remote function will receive the location updates from drivers.
    remote function onTextMessage(websocket:Caller caller, string location) returns websocket:Error? {
        @strand {
            thread:"any"
        }
        worker broadcast returns error? {
            lock {
                string? driverName = driversMap[caller.getConnectionId()];
                if (driverName is string) {
                    string locationUpdateMsg = driverName + " updated the location " + location;
                    // Broadcast the live locations to registered riders.
                    broadcast(locationUpdateMsg);
                }
            }
        }
    }

    remote isolated function onClose(websocket:Caller caller, int statusCode, string reason) {
        lock {
            _ = driversMap.remove(caller.getConnectionId());
        }
    }
}

// This service is for clients to get registered. Once registered, clients will get notified about the live locations
// of the drivers.
service /subscribe on taxiMgtListener {
    resource function get [string name](http:Request req) returns websocket:Service|websocket:UpgradeError {
        return new SubscriberService(name);
    }
}

service class SubscriberService {
    *websocket:Service;

    final string clientName;

    public isolated function init(string username) {
        self.clientName = username;
    }

    remote function onOpen(websocket:Caller caller) returns websocket:Error? {
        string welcomeMsg = "Hi " + self.clientName + "! You have successfully connected.";
        check caller->writeTextMessage(welcomeMsg);
        lock {
            clientsMap[caller.getConnectionId()] = caller;
        }
    }

    remote isolated function onClose(websocket:Caller caller, int statusCode, string reason) {
        lock {
            _ = clientsMap.remove(caller.getConnectionId());
        }
    }
}

// Function to perform the broadcasting of text messages.
isolated function broadcast(string msg) {
    lock {
        foreach var con in clientsMap {
            websocket:Error? err = con->writeTextMessage(msg);
            if (err is websocket:Error) {
                io:println("Error sending message:" + err.message());
            }
        }   
    } 
}

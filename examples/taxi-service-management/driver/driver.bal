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
import ballerina/lang.runtime;
import ballerina/random;
import ballerina/websocket;

public function main() returns error? {
    string username = io:readln("Enter username: ");
    websocket:Client driverClient = check new(string `ws://localhost:9091/taxi/${username}`);
    string connectedMsg = check driverClient->readTextMessage();
    io:println(connectedMsg);
    check updateLocation(driverClient);
}

// This function simulates a real time gps location updates. 
// This updated location will be sent to the server every 2 seconds.
function updateLocation(websocket:Client driver) returns error? {
    while true {
        runtime:sleep(2);
        float lon = random:createDecimal() + 79.0;
        float lat = random:createDecimal() + 6.0;
        string location = "'Latitude'=" + lat.toString() + " 'Longititude'=" + lon.toString();
        check driver->writeTextMessage(location);
    }
}

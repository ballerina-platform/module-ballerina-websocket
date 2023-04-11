// Copyright (c) 2023 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
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

listener Listener l21104 = new(21104);

service /on\-Text\-String on l21104 {
    resource function get .() returns Service|UpgradeError {
       return new WsService21104();
    }
}

service class WsService21104 {
    *Service;
    remote isolated function onMessage(string data) returns string {
        return data;
    }
}

@test:Config {}
public function testSpecialCharachter() returns Error? {
    Client wsClient = check new("ws://localhost:21104/on-Text-String/");
    check wsClient->writeTextMessage("Hi");
    string data = check wsClient->readTextMessage();
    test:assertEquals(data, "Hi");
}

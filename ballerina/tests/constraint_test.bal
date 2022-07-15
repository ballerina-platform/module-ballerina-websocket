// Copyright (c) 2022 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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
import ballerina/constraint;

listener Listener l22003 = new(22003);

public type Cord record {
    @constraint:Int {
        minValue: 0
    }
    int x;
    @constraint:Int {
        minValue: 5
    }
    int y;
};

public type Coord1 record {
    int x;
    int y;
};

Coord1 validRecord = {x: 1, y: 6};

Coord1 invalidRecord = {x: 1, y: 2};

@constraint:String {
    length: 2
}
public type Data string;

service /onTextString on l22003 {
    resource function get .() returns Service|UpgradeError {
        return new WsServicel22003();
    }
}

service class WsServicel22003 {
    *Service;
    remote isolated function onMessage(Caller caller, string data) returns string {
        return data;
    }

    remote isolated function onError(error err) returns Error? {
        io:println("server on error message");
    }
}

service /onRecord on l22003 {
    resource function get .() returns Service|UpgradeError {
        return new WsServicel22003();
    }
}

service class WsServicel22004 {
    *Service;
    remote isolated function onMessage(Caller caller, Cord data) returns Cord {
        return data;
    }

    remote isolated function onError(error err) returns Error? {
        io:println("server on error message");
    }
}

@test:Config {}
public function testConstraintString() returns Error? {
    Client wsClient = check new("ws://localhost:22003/onTextString/");
    check wsClient->writeMessage("Hi");
    Data data = check wsClient->readMessage();
    test:assertEquals(data, "Hi");
}

@test:Config {}
public function testConstraintErrorString() returns Error? {
    Client wsClient = check new("ws://localhost:22003/onTextString/");
    check wsClient->writeMessage("recordVal1234567");
    Data|Error data = wsClient->readMessage();
    test:assertTrue(data is PayloadValidationError);
    if (data is PayloadValidationError) {
        test:assertEquals(data.message(), "data validation failed: error Error (\"Validation failed for 'length' constraint(s).\")");
    } 
}

@test:Config {}
public function testConstraintRecord() returns Error? {
    Client wsClient = check new("ws://localhost:22003/onRecord/");
    check wsClient->writeMessage(validRecord);
    Cord data = check wsClient->readMessage();
    test:assertEquals(data, validRecord);
}

@test:Config {}
public function testConstraintErrorRecord() returns Error? {
    Client wsClient = check new("ws://localhost:22003/onRecord/");
    check wsClient->writeMessage(invalidRecord);
    Cord|Error data = wsClient->readMessage();
    test:assertTrue(data is PayloadValidationError);
    if (data is PayloadValidationError) {
        test:assertEquals(data.message(), "data validation failed: error Error (\"Validation failed for 'minValue' constraint(s).\")");
    } 
}

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

public type Coord record {
    int x;
    int y;
};

json jsonVal = {x: 1, y: 2};
xml xmlVal = xml `<book>The Lost World</book>`;
Coord recordVal = {x: 1, y: 2};
Coord[] recordArrVal = [{x: 1, y: 2}, {x: 3, y: 4}];

listener Listener l78 = new(22078);

service /onRecord on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService78();
    }
}

service class WsService78 {
    *Service;
    remote isolated function onTextMessage(Caller caller, Coord data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onJson on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService82();
    }
}

service class WsService82 {
    *Service;
    remote isolated function onTextMessage(Caller caller, json data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onXml on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService80();
    }
}

service class WsService80 {
    *Service;
    remote isolated function onTextMessage(Caller caller, xml data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onRecordArr on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService81();
    }
}

service class WsService81 {
    *Service;
    remote isolated function onTextMessage(Caller caller, Coord[] data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

@test:Config {}
public function testJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson/");
    check wsClient->writeTextMessage(jsonVal);
    json data = check wsClient->readTextMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onXml/");
    check wsClient->writeTextMessage(xmlVal);
    xml data = check wsClient->readTextMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onRecord/");
    check wsClient->writeTextMessage(recordVal);
    Coord cord = check wsClient->readTextMessage();
    test:assertEquals(cord, recordVal);
}

@test:Config {}
public function testRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onRecordArr");
    check wsClient->writeTextMessage(recordArrVal);
    Coord[] cord = check wsClient->readTextMessage();
    test:assertEquals(cord, recordArrVal);
}

@test:Config {}
public function testErrorRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onRecord");
    check wsClient->writeTextMessage(recordVal);
    Coord[]|Error cord = wsClient->readTextMessage();
    string errMessage = "data binding failed: error(\"{ballerina/lang.value}ConversionError\",message=\"'map" 
                        + "<json>' value cannot be converted to 'websocket:Coord[]'\")";
    if cord is Coord[] {
        test:assertFail("Expected a binding error");
    } else {
        test:assertEquals(cord.message(), errMessage);
    }
}

@test:Config {}
public function testErrorRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onXml");
    check wsClient->writeTextMessage(xmlVal);
    Coord|Error cord = wsClient->readTextMessage();
    string errMessage = "data binding failed: error(\"unrecognized token '<book>The' at line: 1 column: 11\")";
    if cord is Coord {
        test:assertFail("Expected a binding error");
    } else {
        test:assertEquals(cord.message(), errMessage);
    }
}

@test:Config {}
public function testErrorXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson");
    check wsClient->writeTextMessage(jsonVal);
    xml|Error data = wsClient->readTextMessage();
    if data is xml {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith("data binding failed: error(\"failed to parse xml:"));
    }
}

@test:Config {}
public function testErrorJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onXml");
    check wsClient->writeTextMessage(xmlVal);
    string errMessage = "data binding failed: error(\"unrecognized token '<book>The' at line: 1 column: 11\")";
    json|Error data = wsClient->readTextMessage();
    if data is json {
        test:assertFail("Expected a binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson");
    check wsClient->writeTextMessage(xmlVal);
    json|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: unrecognized token '<book>The' at line: 1 column: 11: Status code: 1003";
    if data is json {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onXml");
    check wsClient->writeTextMessage(jsonVal);
    xml|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: failed to parse xml: Unexpected character '{' (code 123) in prolog; expected '<'...: Status code: 1003";
    if data is xml {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onRecordArr");
    check wsClient->writeTextMessage(jsonVal);
    Coord|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: {ballerina/lang.value}ConversionError: Status code: 1003";
    if data is Coord {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onRecord");
    check wsClient->writeTextMessage(recordArrVal);
    Coord|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: {ballerina/lang.value}ConversionError: Status code: 1003";
    if data is Coord {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

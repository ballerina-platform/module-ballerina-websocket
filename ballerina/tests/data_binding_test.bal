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
json[] jsonArr = [{"name":"John","salary":100},{"name":"Jane","salary":200}];

public type Employee record {
    readonly string name;
    int salary;
};

table<Employee> key(name) t = table [
    { name: "John", salary: 100 },
    { name: "Jane", salary: 200 }
];

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

service /onInt on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService87();
    }
}

service class WsService87 {
    *Service;
    remote isolated function onTextMessage(Caller caller, int data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onFloat on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService84();
    }
}

service class WsService84 {
    *Service;
    remote isolated function onTextMessage(Caller caller, float data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onDecimal on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService85();
    }
}

service class WsService85 {
    *Service;
    remote isolated function onTextMessage(Caller caller, decimal data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onBoolean on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService86();
    }
}

service class WsService86 {
    *Service;
    remote isolated function onTextMessage(Caller caller, boolean data) returns Error? {
        check caller->writeTextMessage(data);
    }
}

service /onReturnInt on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService88();
    }
}

service class WsService88 {
    *Service;
    remote isolated function onTextMessage(Caller caller, int data) returns int {
        return data;
    }
}

service /onReturnFloat on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService89();
    }
}

service class WsService89 {
    *Service;
    remote isolated function onTextMessage(Caller caller, float data) returns float {
        return data;
    }
}

service /onReturnDecimal on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService90();
    }
}

service class WsService90 {
    *Service;
    remote isolated function onTextMessage(Caller caller, decimal data) returns decimal {
        return data;
    }
}

service /onReturnRecord on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService91();
    }
}

service class WsService91 {
    *Service;
    remote isolated function onTextMessage(Caller caller, Coord data) returns Coord {
        return data;
    }
}

service /onReturnJson on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService92();
    }
}

service class WsService92 {
    *Service;
    remote isolated function onTextMessage(Caller caller, json data) returns json {
        return data;
    }
}

service /onReturnXml on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService93();
    }
}

service class WsService93 {
    *Service;
    remote isolated function onTextMessage(Caller caller, xml data) returns xml {
        return data;
    }
}

service /onJsonArr on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService95();
    }
}

service class WsService95 {
    *Service;
    remote isolated function onTextMessage(Caller caller, json[] data) returns error? {
        check caller->writeTextMessage(data);
    }
}

service /onTable on l78 {
    resource function get .() returns Service|UpgradeError {
        return new WsService94();
    }
}

service class WsService94 {
    *Service;
    remote isolated function onTextMessage(Caller caller, table<Employee> key(name) data) returns error? {
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
public function testIntDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onInt/");
    check wsClient->writeTextMessage(1);
    int data = check wsClient->readTextMessage();
    test:assertEquals(1, data);
}

@test:Config {}
public function testFloatDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onFloat/");
    check wsClient->writeTextMessage(1.0);
    float data = check wsClient->readTextMessage();
    test:assertEquals(1.0, data);
}

@test:Config {}
public function testDecimalDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onDecimal/");
    decimal val = 3.0;
    check wsClient->writeTextMessage(val);
    decimal data = check wsClient->readTextMessage();
    test:assertEquals(val, data);
}

@test:Config {}
public function testBooleanDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onBoolean/");
    check wsClient->writeTextMessage("true");
    boolean data = check wsClient->readTextMessage();
    test:assertTrue(data);
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

@test:Config {}
public function testClientIntErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson/");
    check wsClient->writeTextMessage(jsonVal);
    int|Error data = wsClient->readTextMessage();
    if data is int {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith("data binding failed"));
    }
}

@test:Config {}
public function testClientFloatErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson/");
    check wsClient->writeTextMessage(jsonVal);
    float|Error data = wsClient->readTextMessage();
    if data is float {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith("data binding failed"));
    }
}

@test:Config {}
public function testClientDecimalErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJson/");
    check wsClient->writeTextMessage(jsonVal);
    decimal|Error data = wsClient->readTextMessage();
    if data is decimal {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith("data binding failed"));
    }
}

@test:Config {}
public function testDispatchingErrorIntDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onInt");
    check wsClient->writeTextMessage("Hello");
    int|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: unrecognized token 'Hello' at line: 1 column: 7: Status code: 1003";
    if data is int {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorFloatDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onFloat");
    check wsClient->writeTextMessage("Hello");
    float|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: unrecognized token 'Hello' at line: 1 column: 7: Status code: 1003";
    if data is float {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testDispatchingErrorDecimalDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onDecimal");
    check wsClient->writeTextMessage("Hello");
    decimal|Error data = wsClient->readTextMessage();
    string errMessage = "data binding failed: unrecognized token 'Hello' at line: 1 column: 7: Status code: 1003";
    if data is decimal {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertEquals(data.message(), errMessage);
    }
}

@test:Config {}
public function testReturnInt() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnInt");
    check wsClient->writeTextMessage(55);
    int data = check wsClient->readTextMessage();
    test:assertEquals(data, 55);
}

@test:Config {}
public function testReturnFloat() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnFloat");
    check wsClient->writeTextMessage(55.01);
    float data = check wsClient->readTextMessage();
    test:assertEquals(data, 55.01);
}

@test:Config {}
public function testReturnDecimal() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnDecimal");
    decimal val = 55.014;
    check wsClient->writeTextMessage(val);
    decimal data = check wsClient->readTextMessage();
    test:assertEquals(data, val);
}

@test:Config {}
public function testReturnRecord() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnRecord");
    check wsClient->writeTextMessage(recordVal);
    Coord data = check wsClient->readTextMessage();
    test:assertEquals(data, recordVal);
}

@test:Config {}
public function testReturnJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnJson/");
    check wsClient->writeTextMessage(jsonVal);
    json data = check wsClient->readTextMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testReturnXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onReturnXml/");
    check wsClient->writeTextMessage(xmlVal);
    xml data = check wsClient->readTextMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testJsonArrDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJsonArr/");
    check wsClient->writeTextMessage(jsonArr);
    json[] data = check wsClient->readTextMessage();
    test:assertEquals(data, jsonArr);
}

@test:Config {}
public function testAnydataDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onJsonArr/");
    check wsClient->writeTextMessage(jsonArr);
    anydata data = check wsClient->readTextMessage();
    test:assertEquals(data, jsonArr);
}

@test:Config {}
public function testTableDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22078/onTable/");
    check wsClient->writeTextMessage(t);
    table<Employee> key(name) data = check wsClient->readTextMessage();
    test:assertEquals(data, t);
}

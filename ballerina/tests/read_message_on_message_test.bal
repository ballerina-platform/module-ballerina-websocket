// Copyright (c) 2022 WSO2 Inc. (//www.wso2.org) All Rights Reserved.

// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at

// //www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;

listener Listener l94 = new(22080);

service /onRecord on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service78();
    }
}

service class service78 {
    *Service;
    remote isolated function onMessage(Caller caller, Coord data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onJson on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service82();
    }
}

service class service82 {
    *Service;
    remote isolated function onMessage(Caller caller, json data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onXml on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service80();
    }
}

service class service80 {
    *Service;
    remote isolated function onMessage(Caller caller, xml data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onRecordArr on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service81();
    }
}

service class service81 {
    *Service;
    remote isolated function onMessage(Caller caller, Coord[] data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onInt on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service87();
    }
}

service class service87 {
    *Service;
    remote isolated function onMessage(Caller caller, byte[]|string data) returns error? {
        check caller->writeMessage(data);
    }
}

service /onFloat on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service84();
    }
}

service class service84 {
    *Service;
    remote isolated function onMessage(Caller caller, float data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onDecimal on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service85();
    }
}

service class service85 {
    *Service;
    remote isolated function onMessage(Caller caller, decimal data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onBoolean on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service86();
    }
}

service class service86 {
    *Service;
    remote isolated function onMessage(Caller caller, boolean data) returns Error? {
        check caller->writeMessage(data.toString().toBytes());
    }
}

service /onReturnInt on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service88();
    }
}

service class service88 {
    *Service;
    remote isolated function onMessage(int data) returns int {
        return data;
    }
}

service /onReturnFloat on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service89();
    }
}

service class service89 {
    *Service;
    remote isolated function onMessage(Caller caller, float data) returns float {
        return data;
    }
}

service /onReturnDecimal on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service90();
    }
}

service class service90 {
    *Service;
    remote isolated function onMessage(decimal data) returns byte[] {
        return data.toString().toBytes();
    }
}

service /onReturnRecord on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service91();
    }
}

service class service91 {
    *Service;
    remote isolated function onMessage(Caller caller, Coord data) returns byte[] {
        return data.toJsonString().toBytes();
    }
}

service /onReturnJson on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service92();
    }
}

service class service92 {
    *Service;
    remote isolated function onMessage(json data) returns json {
        return data.toJsonString().toBytes();
    }
}

service /onReturnXml on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service93();
    }
}

service class service93 {
    *Service;
    remote isolated function onMessage(Caller caller, xml data) returns byte[] {
        return data.toString().toBytes();
    }
}

service /onJsonArr on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service95();
    }
}

service class service95 {
    *Service;
    remote isolated function onMessage(Caller caller, json[] data) returns error? {
        check caller->writeMessage(data.toJsonString().toBytes());
    }
}

service /onTable on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service94();
    }
}

service class service94 {
    *Service;
    remote isolated function onMessage(Caller caller, table<Employee> key(name) data) returns error? {
        check caller->writeMessage(data.toJsonString().toBytes());
    }
}

service /onReadonlyjson on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service96();
    }
}

service class service96 {
    *Service;
    remote isolated function onMessage(Caller caller, readonly & json data) returns Error? {
        check caller->writeMessage(data is json & readonly);
    }
}

service /onReadonlyXml on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service97();
    }
}

service class service97 {
    *Service;
    remote isolated function onMessage(Caller caller, readonly & xml data) returns Error? {
        check caller->writeMessage(data is readonly);
    }
}

service /onReadonlyRecord on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service98();
    }
}

service class service98 {
    *Service;
    remote isolated function onMessage(Caller caller, readonly & Coord data) returns Error? {
        check caller->writeMessage(data is readonly);
    }
}

service /onReadonlyInt on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service99();
    }
}

service class service99 {
    *Service;
    remote isolated function onMessage(Caller caller, readonly & int data) returns Error? {
        check caller->writeMessage(data is readonly);
    }
}

service /onString on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service100();
    }
}

service class service100 {
    *Service;
    remote isolated function onMessage(Caller caller, string data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onBinary on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service101();
    }
}

service class service101 {
    *Service;
    remote isolated function onMessage(Caller caller, byte[]|string data) returns Error? {
        check caller->writeMessage(data);
    }
}

service /onText on l94 {
    resource function get .() returns Service|UpgradeError {
        return new service102();
    }
}

service class service102 {
    *Service;
    remote isolated function onMessage(Caller caller, string data) returns Error? {
        check caller->writeMessage(data);
    }
}

@test:Config {}
public function testOnMessageJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson/");
    check wsClient->writeMessage(jsonVal);
    json data = check wsClient->readMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testOnMessageJsonBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson/");
    check wsClient->writeMessage(jsonVal.toString().toBytes());
    json data = check wsClient->readMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testOnMessageReadonlyJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReadonlyjson/");
    check wsClient->writeMessage(jsonVal);
    boolean data = check wsClient->readMessage();
    test:assertTrue(data);
}

@test:Config {}
public function testOnMessageReadonlyJsonBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReadonlyjson/");
    check wsClient->writeMessage(jsonVal.toString().toBytes());
    boolean data = check wsClient->readMessage();
    test:assertTrue(data);
}

@test:Config {}
public function testOnMessageIntDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onInt/");
    check wsClient->writeMessage(1);
    int data = check wsClient->readMessage();
    test:assertEquals(1, data);
}

@test:Config {}
public function testOnMessageReadonlyIntDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReadonlyInt/");
    check wsClient->writeMessage(1);
    boolean data = check wsClient->readMessage();
    test:assertTrue(data);
}

@test:Config {}
public function testOnMessageFloatDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onFloat/");
    check wsClient->writeMessage(1.0);
    float data = check wsClient->readMessage();
    test:assertEquals(1.0, data);
}

@test:Config {}
public function testOnMessageDecimalDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onDecimal/");
    decimal val = 3.0;
    check wsClient->writeMessage(val);
    decimal data = check wsClient->readMessage();
    test:assertEquals(val, data);
}

@test:Config {}
public function testOnMessageBooleanDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBoolean/");
    check wsClient->writeMessage("true");
    boolean data = check wsClient->readMessage();
    test:assertTrue(data);
}

@test:Config {}
public function testOnMessageXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onXml/");
    check wsClient->writeMessage(xmlVal);
    xml data = check wsClient->readMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testOnMessageXmlBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onXml/");
    check wsClient->writeMessage(xmlVal.toString().toBytes());
    xml data = check wsClient->readMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testOnMessageReadonlyXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReadonlyXml/");
    check wsClient->writeMessage(xmlVal);
    boolean data = check wsClient->readMessage();
    test:assertTrue(data);
}

@test:Config {}
public function testOnMessageRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onRecord/");
    byte[] rec = check recordVal.toJson().toString().toBytes();
    check wsClient->writeMessage(rec);
    Coord cord = check wsClient->readMessage();
    test:assertEquals(cord, recordVal);
}

@test:Config {}
public function testOnMessageReadonlyRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReadonlyRecord/");
    check wsClient->writeMessage(recordVal);
    boolean cord = check wsClient->readMessage();
    test:assertTrue(cord);
}

@test:Config {}
public function testOnMessageRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onRecordArr");
    check wsClient->writeMessage(recordArrVal);
    Coord[] cord = check wsClient->readMessage();
    test:assertEquals(cord, recordArrVal);
}

@test:Config {}
public function testOnMessageErrorRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onRecord");
    check wsClient->writeMessage(recordVal);
    Coord[]|Error cord = wsClient->readMessage();
     if cord is Coord[] {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(cord.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageErrorRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onXml");
    check wsClient->writeMessage(xmlVal);
    Coord|Error cord = wsClient->readMessage();
    if cord is Coord {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(cord.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageErrorXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson");
    check wsClient->writeMessage(jsonVal);
    xml|Error data = wsClient->readMessage();
    if data is xml {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith("data binding failed: error(\"failed to parse xml:"));
    }
}

@test:Config {}
public function testOnMessageErrorJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onXml");
    check wsClient->writeMessage(xmlVal);
    json|Error data = wsClient->readMessage();
    if data is json {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson");
    check wsClient->writeMessage(xmlVal);
    json|Error data = wsClient->readMessage();
    if data is json {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorJsonBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson");
    check wsClient->writeMessage(xmlVal.toString().toBytes());
    json|Error data = wsClient->readMessage();
    if data is json {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onXml");
    check wsClient->writeMessage(jsonVal);
    xml|Error data = wsClient->readMessage();
    if data is xml {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorRecordArrayDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onRecordArr");
    check wsClient->writeMessage(jsonVal);
    Coord|Error data = wsClient->readMessage();
    if data is Coord {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorRecordDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onRecord");
    check wsClient->writeMessage(recordArrVal);
    Coord|Error data = wsClient->readMessage();
    if data is Coord {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageClientIntErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson/");
    check wsClient->writeMessage(jsonVal);
    int|Error data = wsClient->readMessage();
    if data is int {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageClientFloatErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson/");
    check wsClient->writeMessage(jsonVal);
    float|Error data = wsClient->readMessage();
    if data is float {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageClientDecimalErrorDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJson/");
    check wsClient->writeMessage(jsonVal);
    decimal|Error data = wsClient->readMessage();
    if data is decimal {
        test:assertFail("Expected a binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorIntDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onInt");
    check wsClient->writeMessage("Hello");
    int|Error data = wsClient->readMessage();
    if data is int {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorFloatDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onFloat");
    check wsClient->writeMessage("Hello");
    float|Error data = wsClient->readMessage();
    if data is float {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageDispatchingErrorDecimalDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onDecimal");
    check wsClient->writeMessage("Hello");
    decimal|Error data = wsClient->readMessage();
    if data is decimal {
        test:assertFail("Expected a service dispatching binding error");
    } else {
        test:assertTrue(data.message().startsWith(errorMessage));
    }
}

@test:Config {}
public function testOnMessageReturnInt() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnInt");
    check wsClient->writeMessage(55);
    int data = check wsClient->readMessage();
    test:assertEquals(data, 55);
}

@test:Config {}
public function testOnMessageReturnFloat() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnFloat");
    check wsClient->writeMessage(55.01);
    float data = check wsClient->readMessage();
    test:assertEquals(data, 55.01);
}

@test:Config {}
public function testOnMessageReturnDecimal() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnDecimal");
    decimal val = 55.014;
    check wsClient->writeMessage(val);
    decimal data = check wsClient->readMessage();
    test:assertEquals(data, val);
}

@test:Config {}
public function testOnMessageReturnRecord() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnRecord");
    check wsClient->writeMessage(recordVal);
    Coord data = check wsClient->readMessage();
    test:assertEquals(data, recordVal);
}

@test:Config {}
public function testOnMessageReturnJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnJson/");
    check wsClient->writeMessage(jsonVal);
    json data = check wsClient->readMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testOnMessageReturnXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onReturnXml/");
    check wsClient->writeMessage(xmlVal);
    xml data = check wsClient->readMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testOnMessageJsonArrDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJsonArr/");
    check wsClient->writeMessage(jsonArr.toJsonString().toBytes());
    json[] data = check wsClient->readMessage();
    test:assertEquals(data, jsonArr);
}

@test:Config {}
public function testOnMessageByteArrDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJsonArr/");
    check wsClient->writeMessage(jsonArr.toJsonString().toBytes());
    byte[] data = check wsClient->readMessage();
    test:assertEquals(data, jsonArr.toJsonString().toBytes());
}

@test:Config {}
public function testOnMessageAnydataDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onJsonArr/");
    check wsClient->writeMessage(jsonArr);
    anydata data = check wsClient->readMessage();
    test:assertEquals(data, jsonArr);
}

@test:Config {}
public function testOnMessageTableDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onTable/");
    check wsClient->writeMessage(t);
    table<Employee> key(name) data = check wsClient->readMessage();
    test:assertEquals(data, t);
}

@test:Config {}
public function testOnMessageErrorTableDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onTable/");
    check wsClient->writeMessage("jsonArr");
    table<Employee> key(name)|Error data = wsClient->readMessage();
    if data is Error {
        test:assertTrue(data.message().startsWith(errorMessage));
    } else {
        test:assertFail("Expected a service dispatching binding error");
    }
}

@test:Config {}
public function testOnMessageStringDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onString/");
    check wsClient->writeMessage("Hello");
    string data = check wsClient->readMessage();
    test:assertEquals(data, "Hello");
}

@test:Config {}
public function testOnMessageBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeMessage("Hello".toBytes());
    byte[] data = check wsClient->readMessage();
    test:assertEquals(data, "Hello".toBytes());
}

@test:Config {}
public function testBinaryDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeBinaryMessage("Hello");
    string data = check wsClient->readBinaryMessage();
    test:assertEquals(data, "Hello");
}

@test:Config {}
public function testBinaryJsonDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeBinaryMessage(jsonVal);
    json data = check wsClient->readBinaryMessage();
    test:assertEquals(data, jsonVal);
}

@test:Config {}
public function testBinaryXmlDataBinding() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeBinaryMessage(xmlVal);
    xml data = check wsClient->readBinaryMessage();
    test:assertEquals(data, xmlVal);
}

@test:Config {}
public function testUnionWithStringDataBindingForText() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onText/");
    check wsClient->writeMessage(1);
    int|byte[]|string data = check wsClient->readMessage();
    test:assertEquals(data, "1");
}

@test:Config {}
public function testUnionWithoutStringDataBindingForText() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onText/");
    check wsClient->writeMessage(1);
    int|byte[] data = check wsClient->readMessage();
    test:assertEquals(data, 1);
}

@test:Config {}
public function testUnionWithByteArrDataBindingForBinary() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeMessage("1".toBytes());
    int|byte[]|string data = check wsClient->readMessage();
    test:assertEquals(data, "1".toBytes());
}

@test:Config {}
public function testUnionWithoutByteArrDataBindingForBinary() returns Error? {
    Client wsClient = check new("ws://localhost:22080/onBinary/");
    check wsClient->writeMessage("1".toBytes());
    int|string data = check wsClient->readMessage();
    test:assertEquals(data, 1);
}

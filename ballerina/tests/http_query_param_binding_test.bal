// Copyright (c) 2021 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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
import ballerina/http;

string l83Foo = "";
string l83Header = "";
string l84Xyz = "";
int l85Foo = 0;
boolean l86Bar = true;
float l87Xyz = 1.0;
decimal l88Cde = 1.0;
string l88Name = "";
boolean l88Bool = false;

// Enum types for testing enum query parameters
enum WsType {
    ORDER_TYPE,
    CARGO_TYPE
}

enum Status {
    ACTIVE,
    INACTIVE
}

// Global variables for enum tests
WsType capturedWsType = ORDER_TYPE;
Status capturedStatus = ACTIVE;
string capturedEnumId = "";
WsType? capturedOptionalType = ();

listener Listener l83 = new(21083);

service /onTextString on l83 {
   resource function get barz/[string xyz](@http:Header string upgrade, http:Request req, string foo, float? abc) returns Service|UpgradeError {
       l83Foo = foo;
       l83Header = upgrade;
       return new WsService83();
   }
}

listener Listener l84 = new(21084);

service /onTextString on l84 {
   resource function get [string xyz]() returns Service|UpgradeError {
       l84Xyz = xyz;
       return new WsService83();
   }
}

listener Listener l85 = new(21085);

service /onTextString on l85 {
   resource function get .(http:Request req, int foo, string? abc) returns Service|UpgradeError {
       l85Foo = foo;
       return new WsService83();
   }
}

listener Listener l86 = new(21086);

service /onTextString on l86 {
    resource function get .(http:Request req, boolean bar, decimal? abc) returns Service|UpgradeError {
        l86Bar = bar;
        return new WsService83();
    }
}

listener Listener l87 = new(21087);

service /onTextString on l87 {
    resource function get foo/[string bar](http:Request req, float xyz, string? abc, boolean? jkl) returns Service|UpgradeError {
        l87Xyz = xyz;
        return new WsService83();
    }
}

listener Listener l88 = new(21088);

service /onTextString on l88 {
    resource function get foo/[string bar]/bbe(http:Request req, decimal cde, string name, boolean bool, int? intVal) returns Service|UpgradeError {
        l88Cde = cde;
        l88Name = name;
        l88Bool = bool;
        return new WsService83();
    }
}

listener Listener l89 = new(21089);

service /onTextString on l89 {
    resource function get foo(string xyz) returns Service|UpgradeError {
        return new WsService83();
    }
}

listener Listener l90 = new(21090);

service /onTextString on l90 {
    resource function get foo(int xyz) returns Service|UpgradeError {
        return new WsService83();
    }
}

listener Listener l91 = new(21091);

service /onTextString on l91 {
    resource function get foo(boolean xyz) returns Service|UpgradeError {
        return new WsService83();
    }
}

listener Listener l92 = new(21092);

service /onTextString on l92 {
    resource function get foo(float xyz) returns Service|UpgradeError {
        return new WsService83();
    }
}

listener Listener l93 = new(21093);

service /onTextString on l93 {
    resource function get foo(decimal xyz) returns Service|UpgradeError {
        return new WsService83();
    }
}

// Enum query parameter services
listener Listener enumListener1 = new(21150);

service /ws on enumListener1 {
    resource function get .(string id, WsType 'type) returns Service|UpgradeError {
        capturedEnumId = id;
        capturedWsType = 'type;
        return new WsService83();
    }
}

listener Listener enumListener2 = new(21151);

service /enumtest/multi on enumListener2 {
    resource function get .(WsType wsType, Status status) returns Service|UpgradeError {
        capturedWsType = wsType;
        capturedStatus = status;
        return new WsService83();
    }
}

listener Listener enumListener3 = new(21152);

service /enumtest/optional on enumListener3 {
    resource function get .(string id, WsType? 'type) returns Service|UpgradeError {
        capturedEnumId = id;
        capturedOptionalType = 'type;
        return new WsService83();
    }
}

service class WsService83 {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) returns string? {
        return data;
   }
}

// Tests string support for writeTextMessage and onTextMessage
@test:Config {}
public function testStringQueryParamBinding() returns Error? {
    Client wsClient = check new("ws://localhost:21083/onTextString/barz/xyz?foo=WSO2");
    test:assertEquals(l83Foo, "WSO2");
    test:assertEquals(l83Header, "websocket");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testStringQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21083/onTextString/barz/xyz");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testQueryParamBindingWithoutParams() returns Error? {
    Client wsClient = check new("ws://localhost:21084/onTextString/xyz?foo=WSO2");
    test:assertEquals(l84Xyz, "xyz");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testIntQueryParamBinding() returns Error? {
    Client wsClient = check new("ws://localhost:21085/onTextString?foo=4");
    test:assertEquals(l85Foo, 4);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testIntQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21085/onTextString");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testBooleanQueryParamBinding() returns Error? {
    Client wsClient = check new("ws://localhost:21086/onTextString?bar=false");
    test:assertEquals(l86Bar, false);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testBooleanQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21086/onTextString");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testFloatQueryParamBinding() returns Error? {
    Client wsClient = check new("ws://localhost:21087/onTextString/foo/bar?xyz=1.2");
    test:assertEquals(l87Xyz, 1.2);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testFloatQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21087/onTextString/foo/bar");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testDecimalQueryParamBinding() returns Error? {
    Client wsClient = check new("ws://localhost:21088/onTextString/foo/bar/bbe?cde=4.5&name=simba&bool=true");
    decimal expected = 4.5;
    test:assertEquals(l88Cde, expected);
    test:assertEquals(l88Name, "simba");
    test:assertEquals(l88Bool, true);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testMandatoryStringQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21089/onTextString/foo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testMandatoryIntQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21090/onTextString/foo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testMandatoryBooleanQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21091/onTextString/foo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testMandatoryFloatQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21092/onTextString/foo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

@test:Config {}
public function testMandatoryDecimalQueryParamBindingError() returns Error? {
    Client|Error wsClient = new("ws://localhost:21093/onTextString/foo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 400 Bad Request");
    } else {
        test:assertFail("Expected an resource not found error");
    }
}

// Enum query parameter tests
@test:Config {}
public function testEnumQueryParamWithOrderType() returns Error? {
    Client wsClient = check new("ws://localhost:21150/ws?id=123&type=ORDER_TYPE");
    test:assertEquals(capturedEnumId, "123", "ID should match");
    test:assertEquals(capturedWsType, ORDER_TYPE, "Type should be ORDER_TYPE");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testEnumQueryParamWithCargoType() returns Error? {
    Client wsClient = check new("ws://localhost:21150/ws?id=456&type=CARGO_TYPE");
    test:assertEquals(capturedEnumId, "456", "ID should match");
    test:assertEquals(capturedWsType, CARGO_TYPE, "Type should be CARGO_TYPE");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testEnumQueryParamInvalidValue() returns Error? {
    Client|Error wsClient = new("ws://localhost:21150/ws?id=789&type=INVALID_TYPE");
    if wsClient is Error {
        test:assertTrue(wsClient.message().includes("Invalid handshake"), 
            "Should fail with invalid handshake error for invalid enum value");
    } else {
        test:assertFail("Expected an error for invalid enum value");
    }
}

@test:Config {}
public function testEnumQueryParamMissingValue() returns Error? {
    Client|Error wsClient = new("ws://localhost:21150/ws?id=789");
    if wsClient is Error {
        test:assertTrue(wsClient.message().includes("Invalid handshake") || 
                       wsClient.message().includes("Bad Request"), 
            "Should fail with error for missing mandatory enum parameter");
    } else {
        test:assertFail("Expected an error for missing mandatory enum parameter");
    }
}

@test:Config {}
public function testMultipleEnumQueryParams() returns Error? {
    Client wsClient = check new("ws://localhost:21151/enumtest/multi?wsType=ORDER_TYPE&status=ACTIVE");
    test:assertEquals(capturedWsType, ORDER_TYPE, "WsType should be ORDER_TYPE");
    test:assertEquals(capturedStatus, ACTIVE, "Status should be ACTIVE");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testMultipleEnumQueryParamsDifferentValues() returns Error? {
    Client wsClient = check new("ws://localhost:21151/enumtest/multi?wsType=CARGO_TYPE&status=INACTIVE");
    test:assertEquals(capturedWsType, CARGO_TYPE, "WsType should be CARGO_TYPE");
    test:assertEquals(capturedStatus, INACTIVE, "Status should be INACTIVE");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testOptionalEnumQueryParamWithValue() returns Error? {
    capturedOptionalType = (); // Reset
    Client wsClient = check new("ws://localhost:21152/enumtest/optional?id=opt1&type=ORDER_TYPE");
    test:assertEquals(capturedEnumId, "opt1", "ID should match");
    test:assertEquals(capturedOptionalType, ORDER_TYPE, "Optional type should be ORDER_TYPE");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testOptionalEnumQueryParamWithoutValue() returns Error? {
    capturedOptionalType = ORDER_TYPE; // Reset to non-nil value
    Client wsClient = check new("ws://localhost:21152/enumtest/optional?id=opt2");
    test:assertEquals(capturedEnumId, "opt2", "ID should match");
    test:assertEquals(capturedOptionalType, (), "Optional type should be nil when not provided");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}
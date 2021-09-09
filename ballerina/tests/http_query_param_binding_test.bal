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
    resource function get foo/[string bar]/bbe(http:Request req, decimal cde, string name, boolean bool) returns Service|UpgradeError {
        l88Cde = cde;
        l88Name = name;
        l88Bool = bool;
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
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
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
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
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
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
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
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found");
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

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
import ballerina/io;

final map<string> customHeader = {"someHeader1": "some-header-value"};
string header1 = "";
string? header2 = "";
string[] header3 = [];
string header4 = "";

listener Listener l79 = new(21079);

service /onTextString on l79 {
   resource function get .(http:Request req, @http:Header string upgrade) returns Service|UpgradeError {
       header1 = upgrade;
       return new WsService79();
   }
}

listener Listener l80 = new(21080);

service /onTextString on l80 {
   resource function get .(http:Request req, @http:Header string? foo) returns Service|UpgradeError {
       header2 = foo;
       return new WsService79();
   }
}

listener Listener l81 = new(21081);

service /onTextString on l81 {
    resource function get .(http:Request req, @http:Header string[] someHeader1) returns Service|UpgradeError {
        header3 = someHeader1;
        return new WsService79();
    }
}

listener Listener l82 = new(21082);

service /onTextString on l82 {
    resource function get .(http:Request req, @http:Header {name:"upgrade" } string foo) returns Service|UpgradeError {
        header4 = foo;
        io:println(foo);
        return new WsService79();
    }
}

service class WsService79 {
    *Service;
    remote isolated function onTextMessage(Caller caller, string data) returns string? {
        return data;
   }
}

// Tests string support for writeTextMessage and onTextMessage
@test:Config {}
public function testHeaderParams() returns Error? {
    Client wsClient = check new("ws://localhost:21079/onTextString/");
    test:assertEquals(header1, "websocket");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testHeaderParamsWithNillable() returns Error? {
    Client wsClient = check new("ws://localhost:21080/onTextString/");
    test:assertEquals(header2, ());
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testHeaderParamsWithArray() returns Error? {
    Client wsClient = check new("ws://localhost:21081/onTextString/", {customHeaders: customHeader});
    test:assertEquals(header3, ["some-header-value"]);
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testHeaderParamsWithName() returns Error? {
    Client wsClient = check new("ws://localhost:21082/onTextString/", {customHeaders: customHeader});
    test:assertEquals(header4, "websocket");
    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

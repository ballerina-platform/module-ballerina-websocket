// Copyright (c) 2020 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

import ballerina/runtime;
import ballerina/test;
import ballerina/io;
import ballerina/http;

string data = "";
string expectedMsg = "{\"name\":\"Riyafa\", \"age\":23}";

public type WebSocketPerson record {|
   string name;
   int age;
|};

service UpgradeService /onTextString on new Listener(21003) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new WsService1();
   }
}

service class WsService1 {
  *Service;
  remote isolated function onString(Caller caller, string data, boolean finalFrame) {
      checkpanic caller->writeString(data);
  }
}

service UpgradeService /onTextJSON on new Listener(21023) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new WsService2();
   }
}

service class WsService2 {
  *Service;
  remote isolated function onString(Caller caller, json data) {
      checkpanic caller->writeString(data);
  }
}

service UpgradeService /onTextXML on new Listener(21024) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new WsService3();
   }
}

service class WsService3 {
  *Service;
  remote isolated function onString(Caller caller, xml data) {
      checkpanic caller->writeString(data);
  }
}

service UpgradeService /onTextRecord on new Listener(21025) {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new WsService4();
   }
}

service class WsService4 {
  *Service;
  remote isolated function onString(Caller caller, WebSocketPerson data) {
       var personData = data.cloneWithType(json);
       if (personData is error) {
           panic personData;
       } else {
           var returnVal = caller->writeString(personData);
           if (returnVal is WebSocketError) {
               panic <error>returnVal;
           }
       }
   }
}

service UpgradeService /onTextByteArray on new Listener(21026) {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new WsService5();
    }
}

service class WsService5 {
  *Service;
  remote isolated function onString(Caller caller, byte[] data) {
       var returnVal = caller->writeString(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service class clientPushCallbackService {
    *CallbackService;
    remote function onString(AsyncClient wsEp, string text) {
        data = <@untainted>text;
    }

    remote isolated function onError(AsyncClient wsEp, error err) {
        io:println(err);
    }
}

// Tests string support for writeString and onString
@test:Config {}
public function testString() {
   AsyncClient wsClient = new("ws://localhost:21003/onTextString", new clientPushCallbackService());
   checkpanic wsClient->writeString("Hi");
   runtime:sleep(500);
   test:assertEquals(data, "Hi", msg = "Failed writeString");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests JSON support for writeString and onString
@test:Config {}
public function testJson() {
   AsyncClient wsClient = new("ws://localhost:21023/onTextJSON", new clientPushCallbackService());
   checkpanic wsClient->writeString("{\"name\":\"Riyafa\", \"age\":23}");
   runtime:sleep(500);
   test:assertEquals(data, expectedMsg, msg = "Failed writeString");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests XML support for writeString and onString
@test:Config {}
public function testXml() {
   AsyncClient wsClient = new ("ws://localhost:21024/onTextXML", new clientPushCallbackService());
   string msg = "<note><to>Tove</to></note>";
   var output = wsClient->writeString(msg);
   runtime:sleep(500);
   test:assertEquals(data, msg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests Record support for writeString and onString
@test:Config {}
public function testRecord() {
   AsyncClient wsClient = new ("ws://localhost:21025/onTextRecord", new clientPushCallbackService());
   var output = wsClient->writeString("{\"name\":\"Riyafa\", \"age\":23}");
   runtime:sleep(500);
   test:assertEquals(data, expectedMsg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests byte array support for writeString and onString
@test:Config {}
public function testByteArray() {
   AsyncClient wsClient = new ("ws://localhost:21026/onTextByteArray", new clientPushCallbackService());
   string msg = "Hello";
   var output = wsClient->writeString(msg);
   runtime:sleep(500);
   test:assertEquals(data, msg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

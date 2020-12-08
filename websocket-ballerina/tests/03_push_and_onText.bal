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
   remote function onUpgrade(http:Caller caller, http:Request req) returns Service {
       return new WsService1();
   }
}

service class WsService1 {
  *Service;
  remote function onText(Caller caller, string data, boolean finalFrame) {
      io:println("Dispatched to onText");
      checkpanic caller->pushText(data);
  }
}

service UpgradeService /onTextJSON on new Listener(21023) {
   remote function onUpgrade(http:Caller caller, http:Request req) returns Service {
       return new WsService2();
   }
}

service class WsService2 {
  *Service;
  remote function onText(Caller caller, json data) {
      io:println("Dispatched to onText");
      checkpanic caller->pushText(data);
  }
}

service UpgradeService /onTextXML on new Listener(21024) {
   remote function onUpgrade(http:Caller caller, http:Request req) returns Service {
       io:println("Dispatched to onTextXML upgrade resource");
       return new WsService3();
   }
}

service class WsService3 {
  *Service;
  remote function onText(Caller caller, xml data) {
      io:println("Dispatched to onTextXML");
      checkpanic caller->pushText(data);
  }
}

service UpgradeService /onTextRecord on new Listener(21025) {
    remote function onUpgrade(http:Caller caller, http:Request req) returns Service {
       io:println("Dispatched to onTextRecord upgrade resource");
       return new WsService4();
   }
}

service class WsService4 {
  *Service;
  remote function onText(Caller caller, WebSocketPerson data) {
       var personData = data.cloneWithType(json);
       if (personData is error) {
           panic personData;
       } else {
           var returnVal = caller->pushText(personData);
           if (returnVal is WebSocketError) {
               panic <error>returnVal;
           }
       }
   }
}

service UpgradeService /onTextByteArray on new Listener(21026) {
    remote function onUpgrade(http:Caller caller, http:Request req) returns Service {
        io:println("Dispatched to onTextByteArray upgrade resource");
       return new WsService5();
    }
}

service class WsService5 {
  *Service;
  remote function onText(Caller caller, byte[] data) {
       var returnVal = caller->pushText(data);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }
}

service object {} clientPushCallbackService = service object {
    remote function onText(WebSocketClient wsEp, string text) {
        io:println("Executing testString Ontext");
        io:println(text);
        data = <@untainted>text;
    }

    remote function onError(WebSocketClient wsEp, error err) {
        io:println(err);
    }
};

// Tests string support for pushText and onText
@test:Config {}
public function testString() {
   io:println("Executing testString");
   WebSocketClient wsClient = new("ws://localhost:21003/onTextString", {callbackService: clientPushCallbackService});
   checkpanic wsClient->pushText("Hi");
   runtime:sleep(500);
   test:assertEquals(data, "Hi", msg = "Failed pushtext");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
}

// Tests JSON support for pushText and onText
@test:Config {}
public function testJson() {
   WebSocketClient wsClient = new("ws://localhost:21023/onTextJSON",
       {callbackService: clientPushCallbackService});
   checkpanic wsClient->pushText("{\"name\":\"Riyafa\", \"age\":23}");
   runtime:sleep(500);
   test:assertEquals(data, expectedMsg, msg = "Failed pushtext");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

// Tests XML support for pushText and onText
@test:Config {}
public function testXml() {
   WebSocketClient wsClient = new ("ws://localhost:21024/onTextXML", {callbackService: clientPushCallbackService});
   string msg = "<note><to>Tove</to></note>";
   var output = wsClient->pushText(msg);
   runtime:sleep(500);
   test:assertEquals(data, msg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

// Tests Record support for pushText and onText
@test:Config {}
public function testRecord() {
   WebSocketClient wsClient = new ("ws://localhost:21025/onTextRecord",
       {callbackService: clientPushCallbackService});
   var output = wsClient->pushText("{\"name\":\"Riyafa\", \"age\":23}");
   runtime:sleep(500);
   test:assertEquals(data, expectedMsg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

// Tests byte array support for pushText and onText
@test:Config {}
public function testByteArray() {
   WebSocketClient wsClient = new ("ws://localhost:21026/onTextByteArray",
       {callbackService: clientPushCallbackService});
   string msg = "Hello";
   var output = wsClient->pushText(msg);
   runtime:sleep(500);
   test:assertEquals(data, msg, msg = "");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
   //if (result is WebSocketError) {
   //   io:println("Error occurred when closing connection", result);
   //}
}

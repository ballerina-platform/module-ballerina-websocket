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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/io;
import ballerina/lang.value;

string attr1 = "";
string removedAttr = "";
boolean isSecure = true;
string serviceSubProtocol = "";

listener Listener l62 = new(21009);

@ServiceConfig {
    subProtocols: ["json", "xml"]
}
service /onTestUtils on l62 {
   resource function get .() returns Service|UpgradeError {
       return new WsService62();
   }
}

service class WsService62 {
  *Service;
  remote function onTextMessage(Caller caller, string data) returns string? {
      io:println("server on text message");
      caller.setAttribute("test", "testAttr");
      caller.setAttribute("test2", "removedAttr");
      isSecure = caller.isSecure();
      value:Cloneable rmAttr = caller.removeAttribute("test2");
      if rmAttr is string {
          removedAttr = rmAttr;
      }
      value:Cloneable attr = caller.getAttribute("test");
      if attr is string {
          attr1 = attr;
      }
      string? protocol = caller.getNegotiatedSubProtocol();
      if protocol is string {
         serviceSubProtocol = protocol;
      }
  }

  remote isolated function onError(error err) returns Error? {
      io:println("server on error message");
  }
}

// Tests set/get attributes.
@test:Config {}
public function testAttributes() returns Error? {
   Client wsClient = check new("ws://localhost:21009/onTestUtils/", config = {
                                       subProtocols: ["xml"]
                                   });
   check wsClient->writeTextMessage("Hi");
   runtime:sleep(0.5);
   test:assertEquals(attr1, "testAttr");
   test:assertEquals(removedAttr, "removedAttr");
   test:assertEquals(isSecure, false);
   test:assertEquals(serviceSubProtocol, "xml");
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

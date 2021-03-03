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

listener Listener l46 = new(21316);

service /testCookieSync on l46 {
   resource function get .(http:Request req) returns Service|UpgradeError {
       http:Cookie[] cookies = req.getCookies();
       http:Cookie[] usernameCookie = cookies.filter(function
           (http:Cookie cookie) returns boolean {
           return cookie.name == "username";
       });
       http:Cookie cookie = usernameCookie[0];
       string cookieValue = cookie.toStringValue();
       map<string> customHeaders = {"Set-Cookie" : cookie.toStringValue()};
       return new WsService46(customHeaders);
   }
}

service class WsService46 {
  map<string> customHeaders;
  public function init(map<string> customHeaders) {
      self.customHeaders = customHeaders;
  }
  *Service;
  remote isolated function onTextMessage(Caller caller, string data) returns Error? {
      check caller->writeTextMessage(data);
  }
}

http:Cookie cookie = new ("username", "name");
http:Cookie[] httpCookies = [cookie];

ClientConfiguration clientConf = {
   cookies: httpCookies
};

// Tests string support for sending cookies from Async client
@test:Config {}
public function testSendCookieWithSyncClient() returns error? {
   Client wsClient = check new("ws://localhost:21316/testCookieSync/", config = clientConf);
   var resp = wsClient.getHttpResponse();
   if (resp is http:Response) {
      http:Cookie[] respCookies = resp.getCookies();
      http:Cookie respCookie = respCookies[0];
      test:assertEquals(respCookie.toStringValue(), "username=name");
   } else {
      test:assertFail("Cookie header is not present");
   }
   error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

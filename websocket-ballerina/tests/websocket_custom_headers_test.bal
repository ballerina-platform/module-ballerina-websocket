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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/http;

listener http:Listener hl = check new(21001);
listener Listener socketListener = new(<@untainted> hl);
string output = "";
string errorMsg = "";
string pathParam = "";
string queryParam = "";
int x = 0;
final map<string> customHeaders = {"X-some-header": "some-header-value"};

service /isOpen/abc on socketListener {
    resource function get barz/[string xyz]/abc/[string value](http:Request req)
               returns Service|UpgradeError  {
       pathParam = <@untainted> xyz;
       var qParam = req.getQueryParamValue("para1");
       if (qParam is string) {
          queryParam = <@untainted>qParam;
       }
       if (x < 1) {
          x = x + 1;
          return new MyWSService(customHeaders);
       } else {
          return new MyWSService2();
       }
    }
}

service class MyWSService {
  *Service;
  map<string> customHeaders;
  public function init(map<string> customHeaders) {
     self.customHeaders = customHeaders;
  }
  remote function onTextMessage(Caller caller, string text) {
      Error? err = caller->close(timeout = 0);
      output = <@untainted>("In service 1 onTextMessage isOpen " + caller.isOpen().toString());
  }
}

service class MyWSService2 {
  *Service;
  remote function onTextMessage(Caller caller, string text) {
      Error? err = caller->close(timeout = 0);
      output = <@untainted>("In service 2 onTextMessage isOpen " + caller.isOpen().toString());
  }
}

service /helloWorld on hl {
    resource function get hello(http:Caller caller, http:Request req) {
        var result = caller->respond("Hello World!");
    }
}

// Test isOpen when close is called
@test:Config {}
public function testIsOpenCloseCalled() returns error? {
    Client wsClient = check new("ws://localhost:21001/isOpen/abc;a=4;b=5/barz/xyz/abc/rre?para1=value1");
    check wsClient->writeTextMessage("Hi");
    runtime:sleep(0.5);

    test:assertEquals(output, "In service 1 onTextMessage isOpen false");
    test:assertEquals(pathParam, "xyz");
    test:assertEquals(queryParam, "value1");

    var resp = wsClient.getHttpResponse();
    if (resp is http:Response) {
       test:assertEquals(resp.getHeader("X-some-header"), "some-header-value");
    } else {
       test:assertFail("Couldn't find the expected values");
    }

    Client wsClient2 = check new("ws://localhost:21001/isOpen/abc/barz/tuv/abc/cav/");
    check wsClient2->writeTextMessage("Hi");
    runtime:sleep(0.5);
    test:assertEquals(output, "In service 2 onTextMessage isOpen false");
    test:assertEquals(pathParam, "tuv");
    error? err1 = wsClient2->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
    error? err2 = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

@test:Config {}
public function testPortSharingHttpService() returns error? {
    http:Client clientEndpoint = check new ("http://localhost:21001/helloWorld");
    http:Response response = check clientEndpoint->get("/hello");
    string payload = check response.getTextPayload();
    test:assertEquals(payload, "Hello World!");
}

// Test isOpen when a close frame is received
// Disable due to https://github.com/ballerina-platform/module-ballerina-http/issues/71#issuecomment-707017984
@test:Config {enable : false}
public function testIsOpenCloseFrameReceived() returns error? {
    Client wsClient = check new ("ws://localhost:21001");
    check wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 300);
    runtime:sleep(0.5);
    test:assertEquals(output, "In onClose isOpen true");
    error? result = wsClient->close(statusCode = 1000, timeout = 0);
}

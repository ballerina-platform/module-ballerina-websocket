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
import ballerina/http;

http:Listener hl = new(21001);
listener Listener socketListener = new(hl);
string output = "";
string errorMsg = "";
string pathParam = "";
string queryParam = "";
int x = 0;
final map<string> customHeaders = {"X-some-header": "some-header-value"};

service /isOpen/abc on socketListener {
    resource function onUpgrade barz/[string xyz]/abc/[string value](http:Request req)
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
  remote function onString(Caller caller, string text) {
      WebSocketError? err = caller->close(timeoutInSeconds = 0);
      output = <@untainted>("In service 1 onString isOpen " + caller.isOpen().toString());
  }
}

service class MyWSService2 {
  *Service;
  remote function onString(Caller caller, string text) {
      WebSocketError? err = caller->close(timeoutInSeconds = 0);
      output = <@untainted>("In service 2 onString isOpen " + caller.isOpen().toString());
  }
}

// Test isOpen when close is called
@test:Config {}
public function testIsOpenCloseCalled() {
    AsyncClient wsClient = new("ws://localhost:21001/isOpen/abc;a=4;b=5/barz/xyz/abc/rre?para1=value1");
    checkpanic wsClient->writeString("Hi");
    runtime:sleep(500);

    test:assertEquals(output, "In service 1 onString isOpen false");
    test:assertEquals(pathParam, "xyz");
    test:assertEquals(queryParam, "value1");

    var resp = wsClient.getHttpResponse();
    if (resp is http:Response) {
       test:assertEquals(resp.getHeader("X-some-header"), "some-header-value");
    } else {
       test:assertFail("Couldn't find the expected values");
    }

    AsyncClient wsClient2 = new("ws://localhost:21001/isOpen/abc/barz/tuv/abc/cav/");
    checkpanic wsClient2->writeString("Hi");
    runtime:sleep(500);
    test:assertEquals(output, "In service 2 onString isOpen false");
    test:assertEquals(pathParam, "tuv");
}

// Test isOpen when a close frame is received
// Disable due to https://github.com/ballerina-platform/module-ballerina-http/issues/71#issuecomment-707017984
@test:Config {enable : false}
public function testIsOpenCloseFrameReceived() {
    AsyncClient wsClient = new ("ws://localhost:21001");
    checkpanic wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 300);
    runtime:sleep(500);
    test:assertEquals(output, "In onClose isOpen true");
}

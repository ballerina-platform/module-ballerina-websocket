// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
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

string expextedValue = "";

service UpgradeService /pingpong/ws on new Listener(21011) {
    remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service?  {
       if (req.getHeader("X-some-header") == "some-header-value") {
           http:WebSocketCaller|http:WebSocketError wsServiceEp =
               caller->acceptWebSocketUpgrade({"X-some-header": "some-header-value"});
           return new wsService();
        // TODO: Need to check the behavior of acceptWebSocketUpgrade and align it to new design.
        //    http:WebSocketCaller|http:WebSocketError wsServiceEp =
        //        caller->acceptWebSocketUpgrade({"X-some-header": "some-header-value"});
       } else {
           checkpanic caller->cancelWebSocketUpgrade(401, "Unauthorized request. Please login");
       }
    }
}

service class wsService {
  *Service;
  remote isolated function onOpen(Caller caller) {
      checkpanic caller->pushText("some-header-value");
   }
}

service object {} customHeaderService = @WebSocketServiceConfig {} service object {
   remote function onText(WebSocketClient wsEp, string text) {
       expextedValue = <@untainted>text;
   }
};

// Tests when the client sends custom headers to the server.
@test:Config {}
public function testClientSentCustomHeader() {
   WebSocketClient wsClientEp = new ("ws://localhost:21011/pingpong/ws", {
           callbackService:
               customHeaderService,
           customHeaders: {"X-some-header": "some-header-value"}
       });
   runtime:sleep(500);
   test:assertEquals(expextedValue, "some-header-value");
   error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
   if (result is WebSocketError) {
      io:println("Error occurred when closing connection", result);
   }
}

// // Tests the client receiving custom headers from the server.
// @test:Config {}
// public function testClientReceivedCustomHeader() {
//    WebSocketClient wsClient = new ("ws://localhost:21011/pingpong/ws", {
//            callbackService:
//                customHeaderService,
//            customHeaders: {"X-some-header": "some-header-value"}
//        });
//    runtime:sleep(500);
//    var resp = wsClient.getHttpResponse();
//    if (resp is http:Response) {
//        test:assertEquals(resp.getHeader("X-some-header"), "some-header-value");
//    } else {
//        test:assertFail("Couldn't find the expected values");
//    }
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection");
//    if (result is WebSocketError) {
//       io:println("Error occurred when closing connection", result);
//    }
// }

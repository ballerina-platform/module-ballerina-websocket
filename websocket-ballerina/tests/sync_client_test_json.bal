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

import ballerina/test;
import ballerina/http;
import ballerina/io;
import ballerina/runtime;

string aggregatedjsonOutput = "";
listener Listener l9 = checkpanic new(21051);
service /onTextJson on l9 {
   resource function onUpgrade .(http:Caller caller) returns Service|UpgradeError {
       return new WsServiceSyncJson();
   }
}

service class WsServiceSyncJson {
  *Service;
  remote isolated function onString(Caller caller, json data) {
      checkpanic caller->writeString(data);
  }

  remote isolated function onClose(Caller caller, string data, boolean finalFrame) {
        checkpanic caller->writeString(data);
  }
}

@test:Config {}
public function testSyncClientJson() {
   SyncClient wsClient = new("ws://localhost:21051/onTextJson");
   @strand {
      thread:"any"
   }
   worker w1 {
      io:println("Reading message starting: sync json client");

      json resp1 = <json> checkpanic wsClient->readString(json);
      aggregatedjsonOutput = aggregatedjsonOutput + resp1.toString();
      io:println("1st response received at sync json client :" + resp1.toString());

      json resp2 = <json> checkpanic wsClient->readString(json);
      aggregatedjsonOutput = aggregatedjsonOutput + resp2.toString();
      io:println("2nd response received at sync json client :" + resp2.toString());

      json resp3 = <json> checkpanic wsClient->readString(json);
      aggregatedjsonOutput = aggregatedjsonOutput + resp3.toString();
      io:println("3rd response received at sync json client :" + resp3.toString());

      json resp4 = <json> checkpanic wsClient->readString(json);
      aggregatedjsonOutput = aggregatedjsonOutput + resp4.toString();
      io:println("4th response received at sync json client :" + resp4.toString());

      json resp5 = <json> checkpanic wsClient->readString(json);
      aggregatedjsonOutput = aggregatedjsonOutput + resp5.toString();
      io:println("final response received at sync json client :" + resp5.toString());
   }
   @strand {
      thread:"any"
   }
   worker w2 {
      io:println("Waiting till client starts reading json.");
      runtime:sleep(2000);
      var resp1 = wsClient->writeString("{\"name\":\"Brian\", \"age\":23}");
      runtime:sleep(2000);
      var resp2 = wsClient->writeString("{\"name\":\"John\", \"age\":24}");
      runtime:sleep(2000);
      var resp3 = wsClient->writeString("{\"name\":\"Mike\", \"age\":25}");
      var resp4 = wsClient->writeString("{\"name\":\"Bush\", \"age\":40}");
      var resp5 = wsClient->writeString("{\"name\":\"Kane\", \"age\":23}");
   }
   _ = wait {w1, w2};
   string msg = "{\"name\":\"Brian\",\"age\":23}{\"name\":\"John\",\"age\":24}{\"name\":\"Mike\",\"age\":25}" +
                "{\"name\":\"Bush\",\"age\":40}{\"name\":\"Kane\",\"age\":23}";
   test:assertEquals(aggregatedjsonOutput, msg, msg = "");
   runtime:sleep(2000);
}

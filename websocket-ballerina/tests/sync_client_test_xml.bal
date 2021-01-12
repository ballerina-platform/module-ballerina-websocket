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
import ballerina/io;
import ballerina/runtime;

string aggregatedOutput = "";
listener Listener l12 = checkpanic new(21050);
service /onTextXML on l12 {
   resource function onUpgrade .() returns Service|UpgradeError {
       return new WsServiceSyncXml();
   }
}

service class WsServiceSyncXml {
  *Service;
  remote isolated function onString(Caller caller, xml data) {
      checkpanic caller->writeString(data);
  }

  remote isolated function onClose(Caller caller, string data, boolean finalFrame) {
        checkpanic caller->writeString(data);
  }
}

@test:Config {}
public function testSyncClientXml() {
   SyncClient wsClient = new("ws://localhost:21050/onTextXML");
   @strand {
      thread:"any"
   }
   worker w1 {
      io:println("Reading message starting: sync xml client");

      xml resp1 = <xml> checkpanic wsClient->readString(xml);
      aggregatedOutput = aggregatedOutput + resp1.toString();
      io:println("1st response received at sync xml client :" + resp1.toString());

      xml resp2 = <xml> checkpanic wsClient->readString(xml);
      aggregatedOutput = aggregatedOutput + resp2.toString();
      io:println("2nd response received at sync xml client :" + resp2.toString());

      xml resp3 = <xml> checkpanic wsClient->readString(xml);
      aggregatedOutput = aggregatedOutput + resp3.toString();
      io:println("3rd response received at sync xml client :" + resp3.toString());

      xml resp4 = <xml> checkpanic wsClient->readString(xml);
      aggregatedOutput = aggregatedOutput + resp4.toString();
      io:println("4th response received at sync xml client :" + resp4.toString());

      runtime:sleep(3000);

      xml resp5 = <xml> checkpanic wsClient->readString(xml);
      aggregatedOutput = aggregatedOutput + resp5.toString();
      io:println("final response received at sync xml client :" + resp5.toString());
   }

   @strand {
       thread:"any"
   }
   worker w2 {
      io:println("Reading message starting: sync xml client");
      runtime:sleep(2000);
      var resp1 = wsClient->writeString("<note><to>move</to></note>");
      runtime:sleep(2000);
      var resp2 = wsClient->writeString("<note><to>Tove</to></note>");
      runtime:sleep(2000);
      var resp3 = wsClient->writeString("<note><to>Tove</to></note>");
      var resp4 = wsClient->writeString("<note><to>Tove</to></note>");
      var resp5 = wsClient->writeString("<note><to>Tove</to></note>");
   }
   _ = wait {w1, w2};
   string msg = "<note><to>move</to></note><note><to>Tove</to></note><note><to>Tove</to></note><note><to>Tove</to>"
                + "</note><note><to>Tove</to></note>";
   test:assertEquals(aggregatedOutput, msg, msg = "");
   runtime:sleep(2000);
}

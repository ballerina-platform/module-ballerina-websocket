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

import ballerina/io;
import ballerina/log;
import ballerina/http;

service UpgradeService /'error/ws on new Listener(21013) {
   remote isolated function onUpgrade(http:Caller caller, http:Request req) returns Service|WebSocketError {
       return new ErrorService();
   }
}

service class ErrorService {
   *Service;
   remote function onOpen(Caller ep) {
       log:print("connection open");
   }

   remote function onString(Caller ep, string text) {
       log:printError(string `text received: ${text}`);
       var returnVal = ep->writeString(text);
       if (returnVal is WebSocketError) {
           panic <error>returnVal;
       }
   }

   remote function onError(Caller ep, error err) {
       io:println(err.message());
   }

   remote function onClose(Caller ep, int statusCode, string reason) {
       log:printError(string `Connection closed with ${statusCode}, ${reason}`);
   }
}

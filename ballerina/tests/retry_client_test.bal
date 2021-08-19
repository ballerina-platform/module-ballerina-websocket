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
import ballerina/io;
import ballerina/jballerina.java;

string data = "";

// Tests string support for writeTextMessage and onTextMessage
@test:Config {}
public function testString() returns error? {
   Client? wsclient2 = ();
   @strand {
       thread:"any"
   }
   worker w1 returns error? {
       Client|Error wsClient = new("ws://localhost:21003/websocket", { readTimeout: 5, retryConfig: {maxCount: 10}});
       if wsClient is Error {
           io:println("***************************** ERROR ******************************");
           io:println(wsClient);
           io:println("***************************** ERROR ******************************");
       } else {
           wsclient2 = wsClient;
           check wsClient->writeTextMessage("Hi");
           data = check wsClient->readTextMessage();
           io:println("*****************************read data******************************");
           io:println(data);
           io:println("*****************************read data******************************");
           //data = check wsClient->readTextMessage();
           runtime:sleep(5);
           Error|string readData = wsClient->readTextMessage();
           if readData is Error {
               io:println("=============Error2===================");
               io:println(readData);
               io:println("=============Error2===================");
           } else {
               io:println("=============No Error===================");
               io:println(readData);
               io:println("=============No Error===================");
           }
        //    runtime:sleep(5);
        //    Error? writeData3 = wsClient->writeTextMessage("3rd message");
        //    if writeData3 is Error {
        //        io:println("=============writeData3 Error===================");
        //        io:println(writeData3);
        //        io:println("=============writeData3 Error===================");
        //    }
        //    Error|string readData2 = wsClient->readTextMessage();
        //    if readData2 is Error {
        //        io:println("=============Error3===================");
        //        io:println(readData2);
        //        io:println("=============Error3===================");
        //    } else {
        //        io:println("=============No Error3===================");
        //        io:println(readData2);
        //        io:println("=============No Error3===================");
        //    }
           test:assertEquals(data, "Hi", msg = "Failed writeTextMessage");
       }
   }
   @strand {
       thread:"any"
   }
   worker w2 returns error? {
      runtime:sleep(0.5);
      startRemoteServer();
      runtime:sleep(3);
      stopRemoteServer();
      runtime:sleep(3);
      startRemoteServer();
      runtime:sleep(3);
      //stopRemoteServer();
   }
   runtime:sleep(0.5);
   //error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeout = 0);
}

public function startRemoteServer() = @java:Method {
    name: "initiateServer",
    'class: "io.ballerina.stdlib.websocket.testutils.WebSocketRemoteServer"
} external;

public function stopRemoteServer() = @java:Method {
    name: "stop",
    'class: "io.ballerina.stdlib.websocket.testutils.WebSocketRemoteServer"
} external;

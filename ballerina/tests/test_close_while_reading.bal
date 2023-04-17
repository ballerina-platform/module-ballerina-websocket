// Copyright (c) 2023 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

import ballerina/io;
import ballerina/test;
import ballerina/lang.runtime;

listener Listener l6191 = new(6091);
service /textstring on l6191 {
    resource function get .() returns ParallelReadService {
        return new ParallelReadService();
    }
}

// Does not send any data to the client.
service class ParallelReadService {
    *Service;
    remote function onClose(Caller caller) returns error? {
        io:println(string `Connection ${caller.getConnectionId()} closed`);
    }
}

service /serverClose on l6191 {
    resource function get .() returns ServerCloseService {
        return new ServerCloseService();
    }
}

service class ServerCloseService {
    *Service;
    remote function onMessage(Caller caller, string text) returns error? {
        check caller->close();
    }
}

@test:Config {}
public function testParallelClosure() returns Error? {
    Client ws = check new ("ws://localhost:6091/textstring");
    json request = {'type: "start"};
    check ws->writeMessage(request);

    worker name returns error? {
        while true {
            json data = check ws->readMessage();
        } 
    }
    // wait for the worker to start and block in the read operation.
    runtime:sleep(2);
    error? err =  ws->close(); // close the connection during the communication
}

@test:Config {}
public function testParallelServerInitiatedClosure() returns Error? {
    Client ws = check new ("ws://localhost:6091/serverClose");

    worker name returns error? {
        while true {
            json data = check ws->readMessage();
        }
    }
    // wait for the worker to start and the server will close the connection from the server side.
    json request = {'type: "start"};
    runtime:sleep(2);
    check ws->writeMessage(request);
}

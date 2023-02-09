// Copyright (c) 2023 WSO2 LLC. (//www.wso2.org) All Rights Reserved.
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

import ballerina/io;
import ballerina/test;
// import ballerina/lang.runtime;

service on new Listener(6090) {
    resource function get .() returns StreamingService {
        return new StreamingService();
    }
}

service class StreamingService {
    *Service;

    remote function onMessage(Caller caller, json messge) returns error? {
        // worker name returns error? {
        //     stream<int> 'stream = new(new StreamGenerator());
        //     check from int value in 'stream do {
                // json response = {data: value};
                // check caller->writeMessage(messge);
        //     };
        // }
    }

    remote function onClose(Caller caller) returns error? {
        io:println(string `Connection ${caller.getConnectionId()} closed`);
    }
}

// class StreamGenerator {
//     private int value = 0;
//     public isolated function next() returns record {|int value;|}? {
//         self.value += 1;
//         runtime:sleep(2);
//         return {value: self.value};
//     }
// }

@test:Config {}
public function testReadMessageAfterConnectionClosure() returns Error? {
    Client wsClient = check new ("ws://localhost:6090");
    json request = {'type: "start"};
    check wsClient->writeMessage(request);
    check wsClient->close();
    io:println("connection closed");
    json|Error resp = wsClient->readMessage();
    if resp is Error {
        test:assertEquals(resp.message(), "Connection already closed");
    } else {
        test:assertFail("Expected a connection closure error");
    }
}

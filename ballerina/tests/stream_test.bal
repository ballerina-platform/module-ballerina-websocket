// Copyright (c) 2022 WSO2 LLC. (www.wso2.com) All Rights Reserved.
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

import ballerina/lang.runtime;
import ballerina/test;

public type ChatMessage record {|
    string name = "";
    string message = "";
|};

listener Listener streamLis = new (21402);

service /onStream on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamStringSvc();
    }
}

service class StreamStringSvc {
    *Service;

    remote function onMessage(Caller caller, json data) returns stream<string> {
        string[] greets = ["Hi Sam", "Hey Sam", "GM Sam"];
        return greets.toStream();
    }
}

service /onRecordStream on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamRecordSvc();
    }
}

service class StreamRecordSvc {
    *Service;

    remote function onMessage(Caller caller, json data) returns stream<ChatMessage> {
        ChatMessage mes1 = {name: "Sam", message: "Hi"};
        ChatMessage mes2 = {name: "Sam", message: "GM"};
        ChatMessage[] chatMsges = [mes1, mes2];
        return chatMsges.toStream();
    }
}

class EvenNumberGenerator {
    int i = 0;

    public isolated function next() returns record {|int value;|}|error? {
        self.i += 2;
        if self.i > 4 {
            return;
        }
        return {value: self.i};
    }
}

service /onIntStreamWithError on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamIntWithErrorSvc();
    }
}

service class StreamIntWithErrorSvc {
    *Service;

    remote function onMessage(Caller caller, json data) returns stream<int, error?> {
        EvenNumberGenerator evenGen = new ();
        stream<int, error?> evenNumberStream = new (evenGen);
        return evenNumberStream;
    }
}

class ErrorGenerator {
    int i = 0;

    public isolated function next() returns record {|int value;|}|error? {
        return error("panic from next");
    }
}

service /onErrorStream on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamErrorSvc();
    }
}

service class StreamErrorSvc {
    *Service;

    remote function onMessage(Caller caller, json data) returns stream<int, error?> {
        ErrorGenerator errGen = new ();
        stream<int, error?> errNumberStream = new (errGen);
        return errNumberStream;
    }
}

service /onJsonStream on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamJsonSvc();
    }
}

service class StreamJsonSvc {
    *Service;

    remote function onMessage(Caller caller, json data) returns stream<json, error?> {
        json[] jsonMsges = [{"x": 1, "y": 2}, {"x": 4, "y": 5}];
        return jsonMsges.toStream();
    }
}

service /onJsonStreamOnOpen on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new StreamJsonOpenSvc();
    }
}

service class StreamJsonOpenSvc {
    *Service;

    remote function onOpen(Caller caller) returns stream<json, error?> {
        json[] jsonMsges = [{"x": 1, "y": 2}, {"x": 4, "y": 5}];
        return jsonMsges.toStream();
    }
}

service /onConcurrentRequest on streamLis {
    resource function get .() returns Service|UpgradeError {
        return new ConcurrentRequestSvc();
    }
}

service class ConcurrentRequestSvc {
    *Service;

    remote function onOpen(Caller caller) returns stream<int, error?> {
        return [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].toStream().'map(function(int i) returns int {
            runtime:sleep(1);
            return i;
        });
    }

    remote function onMessage(Caller caller) returns int {
        return -1;
    }
}

@test:Config {}
public function testStreamString() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onStream/");
    string[] greets = ["Hi", "Hey", "GM"];
    check wsClient->writeMessage(greets);
    string data = check wsClient->readTextMessage();
    test:assertEquals(data, "Hi Sam");
    string data2 = check wsClient->readTextMessage();
    test:assertEquals(data2, "Hey Sam");
    string data3 = check wsClient->readTextMessage();
    test:assertEquals(data3, "GM Sam");
}

@test:Config {}
public function testRecord() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onRecordStream/");
    string[] greets = ["Hi", "Hey", "GM"];
    check wsClient->writeMessage(greets);
    ChatMessage data = check wsClient->readMessage();
    test:assertEquals(data, {name: "Sam", message: "Hi"});
    ChatMessage data2 = check wsClient->readMessage();
    test:assertEquals(data2, {name: "Sam", message: "GM"});
}

@test:Config {}
public function testIntWithError() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onIntStreamWithError/");
    string[] greets = ["Hi", "Hey", "GM"];
    check wsClient->writeMessage(greets);
    json data = check wsClient->readMessage();
    test:assertEquals(data, 2);
    json data2 = check wsClient->readMessage();
    test:assertEquals(data2, 4);
}

@test:Config {}
public function testError() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onErrorStream/");
    string[] greets = ["Hi", "Hey", "GM"];
    check wsClient->writeMessage(greets);
    string|error data = wsClient->readMessage();
    test:assertTrue(data is error);
    if data is error {
        test:assertEquals(data.message(), "streaming failed: panic from next: Status code: 1011");
    }
}

@test:Config {}
public function testStreamJson() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onJsonStream/");
    string[] greets = ["Hi", "Hey", "GM"];
    check wsClient->writeMessage(greets);
    json data = check wsClient->readMessage();
    test:assertEquals(data, {"x": 1, "y": 2});
    json data2 = check wsClient->readMessage();
    test:assertEquals(data2, {"x": 4, "y": 5});
}

@test:Config {}
public function testStreamJsonOnOpen() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onJsonStreamOnOpen/");
    json data = check wsClient->readMessage();
    test:assertEquals(data, {"x": 1, "y": 2});
    json data2 = check wsClient->readMessage();
    test:assertEquals(data2, {"x": 4, "y": 5});
}

@test:Config {}
public function testConcurrentRequestDuringStreamResponse() returns Error? {
    Client wsClient = check new ("ws://localhost:21402/onConcurrentRequest/");
    int data = check wsClient->readMessage();
    test:assertEquals(data, 1);
    check wsClient->writeMessage("Hello");
    while true {
        int res = check wsClient->readMessage();
        if res >= 10 {
            test:assertFail("Did not receive -1 as the response from the service");
        }
        if res == -1 {
            return;
        }
    }
}

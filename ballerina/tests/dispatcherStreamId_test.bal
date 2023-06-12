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

import ballerina/test;
import ballerina/io;

listener Listener dispatcherStreamIdLis = new(21600);

@ServiceConfig {dispatcherKey: "event", dispatcherStreamId: "id"}
service /ping on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new DispatcherStreamIdPingService();
    }
}

service class DispatcherStreamIdPingService {
    *Service;
    remote function onPing(Caller caller, json data) returns json {
        return {"event":"pong", "id": "3"};
    }
}

@ServiceConfig {dispatcherKey: "event",dispatcherStreamId: "correlationId"}
service /onMessage on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new DispatcherStreamIdOnMessages();
    }
}

service class DispatcherStreamIdOnMessages {
    *Service;
    remote function onMessages() returns json {
    
        return {"event":"responseMessage", "correlationId": "10000"};
    }

     remote function onRequest(Request data) returns UnSubscribe {
    
        return {"event":"unsubscribe", "correlationId": "3434"};
    }
}

@ServiceConfig {dispatcherKey: "type",dispatcherStreamId: "id"}
service /chat on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new DispatcherStreamIdOnStream();
    }
}

service class DispatcherStreamIdOnStream {
    *Service;
    remote function onSubscribe() returns stream<Nextmessage|Completemessage>|Errormessage {
        Nextmessage[] array=[{"type":"NextMessage", "id": "",payload: ()},
        {"type":"NextMessage", "id": "10000",payload: ()},{"type":"CompleteMessage", "id": "500",payload: ()}];
        return array.toStream();
    }

}

@ServiceConfig { dispatcherStreamId: "id"}
service /noDispatcherKey on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new NoDispatcherKeyService();
    }
}

service class NoDispatcherKeyService {
    *Service;
    remote function onPing(Caller caller, json data) returns json {
        return {"event":"pong", "id": "3"};
    }
}

@ServiceConfig {dispatcherKey: "event", dispatcherStreamId: "id"}
service /noDispatcherStreamId on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new NoDispatcherStreamIdInMessageService();
    }
}

service class NoDispatcherStreamIdInMessageService {
    *Service;
    remote function onPing(Caller caller, json data) returns json {
        return {"event":"pong"};
    }
}

@ServiceConfig {dispatcherKey: "event", dispatcherStreamId: "correlationId"}
service /noDispatcherStreamIdInResponse on dispatcherStreamIdLis {
    resource function get .() returns Service|Error {
        return new NoDispatcherStreamIdPresentInResponse();
    }
}

service class NoDispatcherStreamIdPresentInResponse {
    *Service;
    remote function onRequest(json data) returns Response {
        return {"event":"response"};
    }
    remote function onError(Caller caller, error err) returns Error? {
        check caller->writeMessage({"event": err.message()});
    }
}

@test:Config {}
public function testPingMessageForDispatcherStreamId() returns Error? {
    Client cl = check new("ws://localhost:21600/ping");
    check cl->writeMessage({"event": "ping","id": "4"});
    json resp = check cl->readMessage();
    test:assertEquals(resp, {"event": "pong", "id": "4"});
}

@test:Config {}
public function testMessagesForDispatcherStreamId() returns Error? {
    Client cl = check new("ws://localhost:21600/onMessage");
    check cl->writeMessage({"event": "messages","correlationId": "10000"});
    json messageResponse = check cl->readMessage();
    test:assertEquals(messageResponse, {"event": "responseMessage", "correlationId": "10000"});
    check cl->writeMessage({"event": "request","correlationId": "5KFfdnfsdnfsnfj7423ib8hbr3i4234"});
    json subscribeResponse = check cl->readMessage();
    test:assertEquals(subscribeResponse, {"event": "unsubscribe", "correlationId": "5KFfdnfsdnfsnfj7423ib8hbr3i4234"});

}

@test:Config {}
public function testSubscriptionForStream() returns Error? {
    Client cl = check new("ws://localhost:21600/chat");
    
    check cl->writeMessage({"type": "subscribe","id": "5KFfdnfsd98901"});
    json response1 = check cl->readMessage();
    io:println(response1);
    test:assertEquals(response1, {"type":"NextMessage", "id": "5KFfdnfsd98901",payload: ()});
    json response2 = check cl->readMessage();
     io:println(response2);
    test:assertEquals(response2, {"type":"NextMessage", "id": "5KFfdnfsd98901",payload: ()});
    json response3 = check cl->readMessage();
     io:println(response3);
    test:assertEquals(response3, {"type":"CompleteMessage", "id": "5KFfdnfsd98901",payload: ()});

}

@test:Config {}
public function testNoDispatcherKey() returns Error? {
    Client cl = check new("ws://localhost:21600/noDispatcherKey");
    check cl->writeMessage({"event": "ping","id": "4"});
    json|Error resp = cl->readMessage();
    if(resp is Error){
        test:assertEquals( resp.message(),"Encountered an unexpected condition: Status code: 1011");
    }

}

@test:Config {}
public function testNoDispatcherStreamIdInMessage() returns Error? {
    Client cl = check new("ws://localhost:21600/noDispatcherStreamId");
    check cl->writeMessage({"event": "ping"});
    json resp = check cl->readMessage();
    test:assertEquals( resp,{"event":"pong"});
    
}

@test:Config {}
public function testNoDispatcherStreamIdPresentInResponse() returns Error? {
    Client cl = check new("ws://localhost:21600/noDispatcherStreamIdInResponse");
    check cl->writeMessage({"event": "request","correlationId":""});
    json resp = check cl->readMessage();
    test:assertEquals( resp,{"event":"Error: Response type must contain dispatcherStreamId"});
    
}


public type Subscribe record{
    string 'type;
    string id;
};
public type Request record{
    string event;
    string correlationId;
};

public type Response record{|
    string event;
|};

public type UnSubscribe record{
    string event;
    string correlationId;
};

public type Nextmessage record {
    string id;
    string 'type;
    json payload;
};

public type Completemessage record {
    string id;
    string 'type;
};

public type Errormessage record {
    string id;
    string 'type;
    json payload;
};
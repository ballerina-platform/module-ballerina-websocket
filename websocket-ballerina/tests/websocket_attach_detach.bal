// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
//import ballerina/websocket;
import ballerina/io;

string msg = "message";
string attDettServerOutput = "";
string attDettExpectedData = "";
string attDettExpectedErr = "";

listener Listener attachDetachEp = new (21032);
@WebSocketServiceConfig {
    path: "/attach/detach"
}
service attachDetach on attachDetachEp {
    resource function onText(WebSocketCaller caller, string data, boolean finalFrame) returns error? {
        if (data == "attach") {
            var err = attachDetachEp.__attach(wsNoPath);
            handleError(err, caller);
            err = attachDetachEp.__attach(wsWithPath);
        } else if (data == "detach") {
            var err = attachDetachEp.__detach(wsNoPath);
            handleError(err, caller);
            err = attachDetachEp.__detach(wsWithPath);
        } else if (data == "client_attach") {
            var err = attachDetachEp.__attach(wsClientService);
            handleError(err, caller);
        }
    }
}

service wsWithPath = @WebSocketServiceConfig {path: "/hello"} service {
    resource function onText(WebSocketCaller conn, string text, boolean finalFrame) returns error? {
        check conn->pushText(text);
    }
};

service wsNoPath = @WebSocketServiceConfig {} service {

    resource function onText(WebSocketCaller conn, string text, boolean finalFrame) returns error? {
        check conn->pushText(text);
    }
};

service wsClientService = @WebSocketServiceConfig {} service {

    resource function onText(WebSocketClient conn, string text, boolean finalFrame) returns error? {
        check conn->pushText(text);
    }
};

function handleError(error? err, WebSocketCaller caller) {

    if (err is WebSocketError) {
        attDettServerOutput = <@untainted>err.message();
    }
}

service attachService = @WebSocketServiceConfig {} service {
    resource function onText(WebSocketClient caller, string text) {
        attDettExpectedData = <@untainted>text;
    }
    resource function onError(WebSocketClient caller, error err) {
        attDettExpectedErr = <@untainted>err.toString();
    }

};

// Try attaching a WebSocket Client service
@test:Config {}
public function attachClientService() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    io:println("Attaching the service test");
    checkpanic wsClientEp->pushText("client_attach");
    runtime:sleep(500);
    test:assertEquals(attDettServerOutput, "GenericError: Client service cannot be attached to the Listener");
}

// Detach the service first
@test:Config {}
public function detachFirst() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    checkpanic wsClientEp->pushText("detach");
    runtime:sleep(500);
    test:assertEquals(attDettServerOutput, "GenericError: Cannot detach service. Service has not been registered");
    error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
    if (result is WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Tests echoed text message from the attached servers
@test:Config {}
public function attachSuccess() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    checkpanic wsClientEp->pushText("attach");
    runtime:sleep(500);

    // send to the no path service
    WebSocketClient attachClient = new ("ws://localhost:21032", {callbackService: attachService});
    checkpanic attachClient->pushText(msg);
    runtime:sleep(500);
    test:assertEquals(attDettExpectedData, msg);

    // send to service with path
    msg = "path message";
    WebSocketClient pathClient = new ("ws://localhost:21032/hello", {callbackService: attachService});
    checkpanic attachClient->pushText(msg);
    runtime:sleep(500);
    test:assertEquals(attDettExpectedData, msg);
    error? result1 = wsClientEp->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 180);
    if (result1 is WebSocketError) {
       io:println("Error occurred when closing connection", result1);
    }
    error? result2 = attachClient->close(statusCode = 1000, reason = "Close the connection");
    if (result2 is WebSocketError) {
       io:println("Error occurred when closing connection", result2);
    }
    error? result3 = pathClient->close(statusCode = 1000, reason = "Close the connection");
    if (result3 is WebSocketError) {
       io:println("Error occurred when closing connection", result3);
    }
}

// Tests detach
@test:Config {}
public function detachSuccess() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    checkpanic wsClientEp->pushText("detach");
    runtime:sleep(500);
    test:assertEquals(attDettServerOutput, "GenericError: Cannot detach service. Service has not been registered");
    WebSocketClient attachClient = new ("ws://localhost:21032", {callbackService: attachService});
    runtime:sleep(500);
    test:assertEquals(attDettExpectedErr, "error(\"InvalidHandshakeError: Invalid handshake response getStatus: 404 Not Found\")");
    error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
    if (result is WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Attach twice to the service
@test:Config {}
public function attachTwice() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    checkpanic wsClientEp->pushText("attach");
    runtime:sleep(500);
    checkpanic wsClientEp->pushText("attach");
    runtime:sleep(500);
    test:assertEquals(attDettServerOutput, "GenericError: Two services have the same addressable URI");
    error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
    if (result is WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

// Detach from the service twice
@test:Config {}
public function detachTwice() {
    WebSocketClient wsClientEp = new ("ws://localhost:21032/attach/detach");
    checkpanic wsClientEp->pushText("detach");
    runtime:sleep(500);
    checkpanic wsClientEp->pushText("detach");
    runtime:sleep(500);
    test:assertEquals(attDettServerOutput, "GenericError: Cannot detach service. Service has not been registered");
    error? result = wsClientEp->close(statusCode = 1000, reason = "Close the connection");
    if (result is WebSocketError) {
       io:println("Error occurred when closing connection", result);
    }
}

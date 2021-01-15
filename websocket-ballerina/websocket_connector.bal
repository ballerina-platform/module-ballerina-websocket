// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;

# Represents a WebSocket connection in Ballerina. This includes all connection-oriented operations.
class WebSocketConnector {
    private boolean isReady = false;

    # Pushes text to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent.
    # + return  - An `error` if an error occurs when sending
    public isolated function writeString(string data) returns Error? {
        return externWriteString(self, data);
    }

    # Pushes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    public isolated function writeBytes(byte[] data) returns Error? {
        return externWriteBytes(self, data);
    }

    # Pings the connection. If an error occurs while sending the ping frame to the connection, that frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    public isolated function ping(byte[] data) returns Error? {
        return externPing(self, data);
    }

    # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
    # frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    public isolated function pong(byte[] data) returns Error? {
        return externPong(self, data);
    }

    # Calls when the endpoint is ready to receive messages. It can be called only once per endpoint. The
    # WebSocketListener can be called only in the `upgrade` or `onConnect` resources.
    #
    # + return - An `error` if an error occurs when sending
    public isolated function ready() returns Error? {
        return externReady(self);
    }

    # Reads text data from the websocket connection.
    #
    # + return  - The text message or an `error` if an error occurs when sending
    public isolated function readString() returns string|Error {
        return externReadString(self);
    }

    # Reads binary data from the websocket connection.
    #
    # + return  - The binary message or an `error` if an error occurs when sending
    public isolated function readBytes() returns byte[]|Error {
        return externReadBytes(self);
    }

    # Closes the connection.
    #
    # + statusCode - Status code for closing the connection
    # + reason - Reason for closing the connection
    # + timeoutInSecs - Time to wait for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value < 0 (e.g., -1), then the connection waits
    #                   until a close frame is received. If WebSocket frame is received from the remote endpoint
    #                   within the waiting period, the connection is terminated immediately.
    # + return - An `error` if an error occurs when sending
    public isolated function close(int? statusCode = 1000, string? reason = (), int timeoutInSecs = 60) returns Error? {
        if (statusCode is int) {
            if (statusCode <= 999 || statusCode >= 1004 && statusCode <= 1006 || statusCode >= 1012 &&
                statusCode <= 2999 || statusCode > 4999) {
                string errorMessage = "Failed to execute close. Invalid status code: " + statusCode.toString();
                return error WsConnectionClosureError(errorMessage);
            }
            return externClose(self, statusCode, reason is () ? "" : reason, timeoutInSecs);
        } else {
            return externClose(self, -1, "", timeoutInSecs);
        }
    }
}

isolated function externWriteString(WebSocketConnector wsConnector, string text) returns Error? =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
} external;

isolated function externWriteBytes(WebSocketConnector wsConnector, byte[] data) returns Error? =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector",
    name: "writeBytes"
} external;

isolated function externPing(WebSocketConnector wsConnector, byte[] data) returns Error? =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector",
    name: "ping"
} external;

isolated function externPong(WebSocketConnector wsConnector, byte[] data) returns Error? =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector",
    name: "pong"
} external;

isolated function externClose(WebSocketConnector wsConnector, int statusCode, string reason, int timeoutInSecs)
                     returns Error? =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.Close"
} external;

isolated function externReady(WebSocketConnector wsConnector) returns Error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.Ready",
    name: "ready"
} external;

isolated function externReadString(WebSocketConnector wsConnector) returns string|Error =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
} external;

isolated function externReadBytes(WebSocketConnector wsConnector) returns byte[]|Error =
@java:Method {
    'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
} external;

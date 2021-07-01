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

import ballerina/jballerina.java;

# Represents a WebSocket caller.
public isolated client class Caller {

    private boolean open = false;
    private final map<string|int> attributes = {};
    private boolean initializedByService = false;

    isolated function init() {
        // package private function to prevent object creation
    }

    # Pushes text messages to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent.
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function writeTextMessage(string data) returns Error? = @java:Method {
        'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Pushes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function writeBinaryMessage(byte[] data) returns Error? = @java:Method {
        'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Pings the connection. If an error occurs while sending the ping frame to the server, that frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function ping(byte[] data) returns Error? = @java:Method {
        'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
    # the frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function pong(byte[] data) returns Error? = @java:Method {
        'class: "org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Closes the connection.
    #
    # + statusCode - Status code for closing the connection
    # + reason - Reason for closing the connection
    # + timeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value < 0 (e.g., -1), then the connection waits
    #                   until a close frame is received. If the WebSocket frame is received from the remote endpoint
    #                   within the waiting period, the connection is terminated immediately.
    # + return - A `websocket:Error` if an error occurs when sending
    remote isolated function close(int? statusCode = 1000, string? reason = (),
        decimal timeout = 60) returns Error? {
        int code = 1000;
        if (statusCode is int) {
            if (statusCode <= 999 || statusCode >= 1004 && statusCode <= 1006 || statusCode >= 1012 &&
                statusCode <= 2999 || statusCode > 4999) {
                string errorMessage = "Failed to execute close. Invalid status code: " + statusCode.toString();
                return error ConnectionClosureError(errorMessage);
            }
            code = statusCode;
        }
        return self.externClose(code, reason is () ? "" : reason, timeout);
    }

    isolated function externClose(int statusCode, string reason, decimal timeoutInSecs) returns Error? = @java:Method {
        'class: "org.ballerinalang.net.websocket.actions.websocketconnector.Close"
    } external;

    # Sets a connection related attribute.
    #
    # + key - The key, which identifies the attribute
    # + value - The value of the attribute
    public isolated function setAttribute(string key, string|int value) {
        lock {
            self.attributes[key] = value;
        }
    }

    # Gets connection related attribute if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function getAttribute(string key) returns string|int? {
        lock {
            return self.attributes[key];
        }
    }

    # Removes connection related attribute if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function removeAttribute(string key) returns string|int? {
        lock {
            return self.attributes.remove(key);
        }
    }

    # Gives the connection id associated with this connection.
    #
    # + return - The unique ID associated with the connection
    public isolated function getConnectionId() returns string = @java:Method {
        'class: "org.ballerinalang.net.websocket.client.SyncInitEndpoint"
    } external;

    # Gives the subprotocol if any that is negotiated with the client.
    #
    # + return - The subprotocol if any negotiated with the client or `nil`
    public isolated function getNegotiatedSubProtocol() returns string? = @java:Method {
        'class: "org.ballerinalang.net.websocket.client.SyncInitEndpoint",
        name: "getNegotiatedSubProtocol"
    } external;

    # Gives the secured status of the connection.
    #
    # + return - `true` if the connection is secure
    public isolated function isSecure() returns boolean= @java:Method {
        'class: "org.ballerinalang.net.websocket.client.SyncInitEndpoint"
    } external;

    # Gives the open or closed status of the connection.
    #
    # + return - `true` if the connection is open
    public isolated function isOpen() returns boolean {
        lock {
            return self.open;
        }
    }
}

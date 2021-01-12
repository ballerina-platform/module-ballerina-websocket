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

import ballerina/java;
import ballerina/http;

# Represents a WebSocket synchronous client endpoint.
public client class SyncClient {

    private string id = "";
    private string? negotiatedSubProtocol = ();
    private boolean secure = false;
    private boolean open = false;
    private http:Response? response = ();
    private map<any> attributes = {};

    private WebSocketConnector conn = new;
    private string url = "";
    private WebSocketClientConfiguration config = {};
    private (service object {})? callbackService = ();

    # Initializes the synchronous client when called.
    #
    # + url - URL of the target service
    # + callbackService - The callback service of the client. Resources in this service gets called on the
    #                     receipt of ping, pong, close from the server
    # + config - The configurations to be used when initializing the client
    public isolated function init(string url, (service object {})? callbackService = (), WebSocketClientConfiguration? config = ()) {
        self.url = url;
        self.config = config ?: {};
        self.callbackService = callbackService ?: ();
        self.initEndpoint();
    }

    # Initializes the endpoint.
    public isolated function initEndpoint() {
        var retryConfig = self.config?.retryConfig;
        if (retryConfig is WebSocketRetryConfig) {
            return externSyncRetryInitEndpoint(self);
        } else {
            return externSyncWSInitEndpoint(self);
        }
    }

    # Writes text to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent. If it is a byte[], it is converted to a UTF-8 string for sending
    # + finalFrame - Set to `true` if this is a final frame of a (long) message
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeString(string|json|xml|boolean|int|float|byte|byte[] data,
    boolean finalFrame = true) returns Error? {
        return self.conn.writeString(data, finalFrame);
    }

    # Writes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + finalFrame - Set to `true` if this is a final frame of a (long) message
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeBytes(byte[] data, boolean finalFrame = true) returns Error? {
        return self.conn.writeBytes(data, finalFrame);
    }

    # Pings the connection. If an error occurs while sending the ping frame to the server, that frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function ping(byte[] data) returns Error? {
        return self.conn.ping(data);
    }

    # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
    # frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function pong(byte[] data) returns Error? {
        return self.conn.pong(data);
    }

    # Closes the connection.
    #
    # + statusCode - Status code for closing the connection
    # + reason - Reason for closing the connection
    # + timeoutInSeconds - Time to wait for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value is < 0 (e.g., -1), then the connection
    #                   waits until a close frame is received. If the WebSocket frame is received from the remote
    #                   endpoint within the waiting period, the connection is terminated immediately.
    # + return - An `error` if an error occurs while closing the WebSocket connection
    remote isolated function close(int? statusCode = 1000, string? reason = (),
        int timeoutInSeconds = 60) returns Error? {
        return self.conn.close(statusCode, reason, timeoutInSeconds);
    }

    # Sets a connection-related attribute.
    #
    # + key - The key, which identifies the attribute
    # + value - The value of the attribute
    public isolated function setAttribute(string key, any value) {
        self.attributes[key] = value;
    }

    # Gets connection-related attributes if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function getAttribute(string key) returns any {
        return self.attributes[key];
    }

    # Removes connection related attribute if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function removeAttribute(string key) returns any {
        return self.attributes.remove(key);
    }

    # Gives the connection id associated with this connection.
    #
    # + return - The unique ID associated with the connection
    public isolated function getConnectionId() returns string {
        return self.id;
    }

    # Gives the subprotocol if any that is negotiated with the client.
    #
    # + return - The subprotocol if any negotiated with the client or `nil`
    public isolated function getNegotiatedSubProtocol() returns string? {
        return self.negotiatedSubProtocol;
    }

    # Gives the secured status of the connection.
    #
    # + return - `true` if the connection is secure
    public isolated function isSecure() returns boolean {
        return self.secure;
    }

    # Gives the open or closed status of the connection.
    #
    # + return - `true` if the connection is open
    public isolated function isOpen() returns boolean {
        return self.open;
    }

    # Gives the HTTP response if any received for the client handshake request.
    #
    # + return - The HTTP response received from the client handshake request
    public isolated function getHttpResponse() returns http:Response? {
        return self.response;
    }

    # Reads the texts in a synchronous manner
    #
    # + return  - The text data sent by the server or an `error` if an error occurs when sending
    remote isolated function readString() returns string|Error {
        return self.conn.readString();
    }

    # Reads the binary data in a synchronous manner
    #
    # + return  - The binary data sent by the server or an `error` if an error occurs when sending
    remote isolated function readBytes() returns byte[]|Error {
        return self.conn.readBytes();
    }
}

isolated function externSyncWSInitEndpoint(SyncClient wsClient) = @java:Method {
    'class: "org.ballerinalang.net.websocket.client.InitEndpoint",
    name: "initEndpoint"
} external;

isolated function externSyncRetryInitEndpoint(SyncClient wsClient) = @java:Method {
    'class: "org.ballerinalang.net.websocket.client.RetryInitEndpoint",
    name: "initEndpoint"
} external;

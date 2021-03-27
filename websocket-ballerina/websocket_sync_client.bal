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

import ballerina/lang.array;
import ballerina/jballerina.java;
import ballerina/time;
import ballerina/http;

# Represents a WebSocket synchronous client endpoint.
public client class Client {

    private string id = "";
    private string? negotiatedSubProtocol = ();
    private boolean secure = false;
    private boolean open = false;
    private http:Response? response = ();
    private map<any> attributes = {};

    private WebSocketConnector conn = new;
    private string url = "";
    private ClientConfiguration config = {};
    private PingPongService? pingPongService = ();

    # Initializes the synchronous client when called.
    #
    # + url - URL of the target service
    # + config - The configurations to be used when initializing the client
    public isolated function init(string url, *ClientConfiguration config) returns Error? {
        self.url = url;
        addCookies(config);
        check initClientAuth(config);
        self.config = config;
        var pingPongHandler = config["pingPongHandler"];
        if (pingPongHandler is PingPongService) {
            self.pingPongService = pingPongHandler;
        }
        return self.initEndpoint();
    }

    public isolated function initEndpoint() returns Error? {
        return externSyncWSInitEndpoint(self);
    }

    # Writes text to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent.
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeTextMessage(string data) returns Error? {
        return self.conn.writeTextMessage(data);
    }

    # Writes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeBinaryMessage(byte[] data) returns Error? {
        return self.conn.writeBinaryMessage(data);
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
    # + timeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value is < 0 (e.g., -1), then the connection
    #                   waits until a close frame is received. If the WebSocket frame is received from the remote
    #                   endpoint within the waiting period, the connection is terminated immediately.
    # + return - An `error` if an error occurs while closing the WebSocket connection
    remote isolated function close(int? statusCode = 1000, string? reason = (), decimal timeout = 60) returns Error? {
        return self.conn.close(statusCode, reason, timeout);
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
    remote isolated function readTextMessage() returns string|Error {
        return self.conn.readTextMessage();
    }

    # Reads the binary data in a synchronous manner
    #
    # + return  - The binary data sent by the server or an `error` if an error occurs when sending
    remote isolated function readBinaryMessage() returns byte[]|Error {
        return self.conn.readBinaryMessage();
    }

    # Reads data from the WebSocket connection
    #
    # + return  - A `string` if a text message is received, `byte[]` if a binary message is received or a `websocket:Error`
    #             if an error occurs when receiving
    remote isolated function readMessage() returns string|byte[]|Error {
        return self.conn.readMessage();
    }
}

# Configurations for the WebSocket client.
# Following fields are inherited from the other configuration records in addition to the Client specific
# configs.
#
# |                                                                              |
# |:---------------------------------------------------------------------------- |
# | callbackService - Copied from CommonClientConfiguration             |
# | subProtocols - Copied from CommonClientConfiguration                |
# | customHeaders - Copied from CommonClientConfiguration               |
# | idleTimeout - Copied from CommonClientConfiguration                 |
# | secureSocket - Copied from CommonClientConfiguration                |
# | maxFrameSize - Copied from CommonClientConfiguration                |
# | webSocketCompressionEnabled - Copied from CommonClientConfiguration |
# | handShakeTimeout - Copied from CommonClientConfiguration            |
# | cookies - Copied from CommonClientConfiguration                     |
public type ClientConfiguration record {|
    *CommonClientConfiguration;
|};

# Common client configurations for WebSocket clients.
#
# + subProtocols - Negotiable sub protocols of the client
# + customHeaders - Custom headers, which should be sent to the server
# + readTimeout - Read timeout (in seconds) of the client. This is applicable only for the Sync client
# + secureSocket - SSL/TLS-related options
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes
#                  If this is not set, is negative, or is zero, the default frame size of 65536 will be used.
# + webSocketCompressionEnabled - Enable support for compression in the WebSocket
# + handShakeTimeout - Time (in seconds) that a connection waits to get the response of
#                               the webSocket handshake. If the timeout exceeds, then the connection is terminated with
#                               an error.If the value < 0, then the value sets to the default value(300).
# + cookies - An Array of `http:Cookie`
# + auth - Configurations related to client authentication
# + pingPongHandler - A service to handle ping/pong frames.
#                     Resources in this service gets called on the receipt of ping, pong from the server
public type CommonClientConfiguration record {|
    string[] subProtocols = [];
    map<string> customHeaders = {};
    decimal readTimeout = -1;
    ClientSecureSocket secureSocket?;
    int maxFrameSize = 65536;
    boolean webSocketCompressionEnabled = true;
    decimal handShakeTimeout = 300;
    http:Cookie[] cookies?;
    ClientAuthConfig auth?;
    PingPongService pingPongHandler?;
|};

# Configures the SSL/TLS options to be used for WebSocket client.
public type ClientSecureSocket record {|
    *http:ClientSecureSocket;
|};

# Adds cookies to the custom header.
#
# + config - Represents the cookies to be added
public isolated function addCookies(ClientConfiguration config) {
   string cookieHeader = "";
   var cookiesToAdd = config["cookies"];
   if (cookiesToAdd is http:Cookie[]) {
       http:Cookie[] sortedCookies = cookiesToAdd.sort(array:ASCENDING, isolated function(http:Cookie c) returns int {
           var cookiePath = c.path;
           int l = 0;
           if (cookiePath is string) {
               l = cookiePath.length();
           }
          return l;
       });
       foreach var cookie in sortedCookies {
           var cookieName = cookie.name;
           var cookieValue = cookie.value;
           if (cookieName is string && cookieValue is string) {
               cookieHeader = cookieHeader + cookieName + EQUALS + cookieValue + SEMICOLON + SPACE;
           }
           cookie.lastAccessedTime = time:utcNow();
       }
       if (cookieHeader != "") {
           cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 2);
           map<string> headers = config["customHeaders"];
           headers["Cookie"] = cookieHeader;
           config["customHeaders"] = headers;
       }
   }
}

const EQUALS = "=";
const SPACE = " ";
const SEMICOLON = ";";

isolated function externSyncWSInitEndpoint(Client wsClient) returns Error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.client.SyncInitEndpoint",
    name: "initEndpoint"
} external;

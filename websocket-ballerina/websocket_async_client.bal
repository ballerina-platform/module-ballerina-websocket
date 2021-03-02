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

import ballerina/jballerina.java;
import ballerina/lang.array;
import ballerina/time;
import ballerina/http;
import ballerina/mime;
import ballerina/lang.runtime;

# Represents a WebSocket client endpoint.
public client class AsyncClient {

    private string id = "";
    private string? negotiatedSubProtocol = ();
    private boolean secure = false;
    private boolean open = false;
    private http:Response? response = ();
    private map<any> attributes = {};

    private WebSocketConnector conn = new;
    private string url = "";
    private ClientConfiguration config = {};
    private Service? callbackService = ();
    private DynamicListener dynamicListener = new;

    # Initializes the client when called.
    #
    # + url - URL of the target service
    # + callbackService - The callback service of the client. Resources in this service gets called on the
    #                     receipt of messages from the server
    # + config - The configurations to be used when initializing the client
    public isolated function init(string url, Service? callbackService = (), ClientConfiguration? config = ())
                returns Error? {
        self.url = url;
        if (config is ClientConfiguration) {
           addCookies(config);
           check initClientAuth(config);
        }
        self.config = config ?: {};
        self.callbackService = callbackService ?: ();
        self.dynamicListener = new DynamicListener();
        runtime:registerListener(self.dynamicListener);
        return self.initEndpoint();
    }

    public isolated function initEndpoint() returns Error? {
        var retryConfig = self.config?.retryConfig;
        if (retryConfig is WebSocketRetryConfig) {
            return externRetryInitEndpoint(self);
        } else {
            return externWSInitEndpoint(self);
        }
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
    # + timeout - Time to wait for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value is < 0 (e.g., -1), then the connection
    #                   waits until a close frame is received. If the WebSocket frame is received from the remote
    #                   endpoint within the waiting period, the connection is terminated immediately.
    # + return - An `error` if an error occurs while closing the WebSocket connection
    remote isolated function close(int? statusCode = 1000, string? reason = (),
        int timeout = 60) returns Error? {
        Error? err = self.conn.close(statusCode, reason, timeout);
        runtime:deregisterListener(self.dynamicListener);
        return err;
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
}


public class DynamicListener {

   *runtime:DynamicListener;

   public isolated function init(){}

   public isolated function 'start() returns error? {}

   public isolated function gracefulStop() returns error? {}

   public isolated function immediateStop() returns error? {}
}

# Configurations for the WebSocket client.
# Following fields are inherited from the other configuration records in addition to the Client specific
# configs.
#
# |                                                                              |
# |:---------------------------------------------------------------------------- |
# | callbackService - Copied from CommonWebSocketClientConfiguration             |
# | subProtocols - Copied from CommonWebSocketClientConfiguration                |
# | customHeaders - Copied from CommonWebSocketClientConfiguration               |
# | idleTimeout - Copied from CommonWebSocketClientConfiguration        |
# | secureSocket - Copied from CommonWebSocketClientConfiguration                |
# | maxFrameSize - Copied from CommonWebSocketClientConfiguration                |
# | webSocketCompressionEnabled - Copied from CommonWebSocketClientConfiguration |
# | handShakeTimeout - Copied from CommonWebSocketClientConfiguration   |
# | cookies - Copied from CommonWebSocketClientConfiguration                     |
# + retryConfig - Retry related configurations
public type ClientConfiguration record {|
    *CommonWebSocketClientConfiguration;
    WebSocketRetryConfig retryConfig?;
|};

# Common client configurations for WebSocket clients.
#
# + subProtocols - Negotiable sub protocols of the client
# + customHeaders - Custom headers, which should be sent to the server
# + idleTimeout - Idle timeout of the client. Upon timeout, the `onIdleTimeout` resource (if defined)
#                          of the client service will be triggered
# + readTimeout - Read timeout of the client. This is applicable only for the Sync client
# + secureSocket - SSL/TLS-related options
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes
#                  If this is not set, is negative, or is zero, the default frame size of 65536 will be used.
# + webSocketCompressionEnabled - Enable support for compression in the WebSocket
# + handShakeTimeout - Time (in seconds) that a connection waits to get the response of
#                               the webSocket handshake. If the timeout exceeds, then the connection is terminated with
#                               an error.If the value < 0, then the value sets to the default value(300).
# + cookies - An Array of `http:Cookie`
# + auth - Configurations related to client authentication
public type CommonWebSocketClientConfiguration record {|
    string[] subProtocols = [];
    map<string> customHeaders = {};
    decimal idleTimeout = -1;
    decimal readTimeout = -1;
    http:ClientSecureSocket? secureSocket = ();
    int maxFrameSize = 65536;
    boolean webSocketCompressionEnabled = true;
    decimal handShakeTimeout = 300;
    http:Cookie[] cookies?;
    ClientAuthConfig auth?;
|};

# Adds cookies to the custom header.
#
# + config - Represents the cookies to be added
public isolated function addCookies(ClientConfiguration|WebSocketFailoverClientConfiguration config) {
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
           cookie.lastAccessedTime = time:currentTime();
       }
       if (cookieHeader != "") {
           cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 2);
           map<string> headers = config["customHeaders"];
           headers["Cookie"] = cookieHeader;
           config["customHeaders"] = headers;
       }
   }
}

# Retry configurations for WebSocket.
#
# + maxCount - The maximum number of retry attempts. If the count is zero, the client will retry indefinitely
# + intervalInMillis - The number of milliseconds to delay before attempting to reconnect
# + backOffFactor - The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems
#                persist
# + maxWaitIntervalInMillis - Maximum time of the retry interval in milliseconds
public type WebSocketRetryConfig record {|
    int maxCount = 0;
    int intervalInMillis = 1000;
    float backOffFactor = 1.0;
    int maxWaitIntervalInMillis = 30000;
|};

const EQUALS = "=";
const SPACE = " ";
const SEMICOLON = ";";
string dummy = mime:MULTIPART_MIXED;

isolated function externWSInitEndpoint(AsyncClient wsClient) returns Error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.client.InitEndpoint",
    name: "initEndpoint"
} external;

isolated function externRetryInitEndpoint(AsyncClient wsClient) returns Error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.client.RetryInitEndpoint",
    name: "initEndpoint"
} external;

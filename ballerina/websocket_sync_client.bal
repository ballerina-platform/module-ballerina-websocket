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
public isolated client class Client {

    private boolean open = false;
    private final map<string|int> attributes = {};

    private string url = "";
    private ClientConfiguration & readonly config;
    private final PingPongService? pingPongService;

    # Initializes the synchronous client when called.
    #
    # + url - URL of the target service
    # + config - The configurations to be used when initializing the client
    public isolated function init(string url, *ClientConfiguration config) returns Error? {
        self.url = url;
        addCookies(config);
        check initClientAuth(config);
        ClientInferredConfig inferredConfig = {
            subProtocols: config.subProtocols,
            customHeaders: config.customHeaders,
            readTimeout: config.readTimeout,
            writeTimeout: config.writeTimeout,
            secureSocket: config.secureSocket,
            maxFrameSize: config.maxFrameSize,
            webSocketCompressionEnabled: config.webSocketCompressionEnabled,
            handShakeTimeout: config.handShakeTimeout,
            retryConfig: config.retryConfig
        };
        self.config = inferredConfig.cloneReadOnly();
        var pingPongHandler = config["pingPongHandler"];
        if pingPongHandler is PingPongService {
            self.pingPongService = pingPongHandler;
        } else {
            self.pingPongService = ();
        }
        return self.initEndpoint();
    }

    public isolated function initEndpoint() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.client.SyncInitEndpoint",
        name: "initEndpoint"
    } external;

    # Writes text messages to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function writeTextMessage(anydata data) returns Error? {
        return self.externWriteTextMessage(getString(data));
    }

    # Writes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function writeBinaryMessage(byte[] data) returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Pings the connection. If an error occurs while sending the ping frame to the server, that frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function ping(byte[] data) returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
    # the frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - A `websocket:Error` if an error occurs when sending
    remote isolated function pong(byte[] data) returns Error? = @java:Method {
         'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector"
    } external;

    # Closes the connection.
    #
    # + statusCode - Status code for closing the connection
    # + reason - Reason for closing the connection
    # + timeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint before closing the
    # connection. If the timeout exceeds, then the connection is terminated even though a close frame
    # is not received from the remote endpoint. If the value is < 0 (e.g., -1), then the connection
    # waits until a close frame is received. If the WebSocket frame is received from the remote
    # endpoint within the waiting period, the connection is terminated immediately
    # + return - A `websocket:Error` if an error occurs while closing the WebSocket connection
    remote isolated function close(int? statusCode = 1000, string? reason = (), decimal timeout = 60) returns Error? {
        int code = 1000;
        if statusCode is int {
            if statusCode <= 999 || statusCode >= 1004 && statusCode <= 1006 || statusCode >= 1012 &&
                statusCode <= 2999 || statusCode > 4999 {
                string errorMessage = "Failed to execute close. Invalid status code: " + statusCode.toString();
                return error ConnectionClosureError(errorMessage);
            }
            code = statusCode;
        }
        return self.externClose(code, reason is () ? "" : reason, timeout);
    }

    # Sets a connection-related attribute.
    #
    # + key - The key, which identifies the attribute
    # + value - The value of the attribute
    public isolated function setAttribute(string key, string|int value) {
        lock {
            self.attributes[key] = value;
        }
    }

    # Gets connection-related attributes if any.
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
        'class: "io.ballerina.stdlib.websocket.WebSocketUtil"
    } external;

    # Gives the subprotocol if any that is negotiated with the client.
    #
    # + return - The subprotocol if any negotiated with the client or `nil`
    public isolated function getNegotiatedSubProtocol() returns string? {
        return self.externGetNegotiatedSubProtocol();
    }

    # Gives the secured status of the connection.
    #
    # + return - `true` if the connection is secure
    public isolated function isSecure() returns boolean = @java:Method {
        'class: "io.ballerina.stdlib.websocket.WebSocketUtil"
    } external;


    # Gives the open or closed status of the connection.
    #
    # + return - `true` if the connection is open
    public isolated function isOpen() returns boolean {
        lock {
            return self.open;
        }
    }

    # Gives the HTTP response if any received for the client handshake request.
    #
    # + return - The HTTP response received from the client handshake request
    public isolated function getHttpResponse() returns http:Response? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.client.SyncInitEndpoint"
    } external;

    # Reads text messages in a synchronous manner.
    #
    # + targetType - The payload type (sybtype of `anydata`), which is expected to be returned after data binding
    # + return  - The text data sent by the server or a `websocket:Error` if an error occurs when receiving
    remote isolated function readTextMessage(TargetType targetType = <>) returns targetType|Error = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketSyncConnector"
    } external;

    # Reads binary data in a synchronous manner.
    #
    # + return  - The binary data sent by the server or an `websocket:Error` if an error occurs when receiving
    remote isolated function readBinaryMessage() returns byte[]|Error = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketSyncConnector"
    } external;

    # Reads data from the WebSocket connection.
    #
    # + return - A `string` if a text message is received, `byte[]` if a binary message is received or a `websocket:Error`
    #            if an error occurs when receiving
    remote isolated function readMessage() returns string|byte[]|Error = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketSyncConnector"
    } external;

    isolated function externClose(int statusCode, string reason, decimal timeoutInSecs)
                         returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.Close"
    } external;

    isolated function externSyncWSInitEndpoint() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.client.SyncInitEndpoint",
        name: "initEndpoint"
    } external;

    isolated function externGetNegotiatedSubProtocol() returns string? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.WebSocketUtil",
        name: "getNegotiatedSubProtocol"
    } external;

    isolated function externWriteTextMessage(string data) returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector",
        name: "writeTextMessage"
    } external;
}

# Configurations for the WebSocket client.
# The following fields are inherited from the other configuration records in addition to the client-specific configs.                    |
public type ClientConfiguration record {|
    *CommonClientConfiguration;
|};

# Common client configurations for WebSocket clients.
#
# + subProtocols - Negotiable sub protocols of the client
# + customHeaders - Custom headers, which should be sent to the server
# + readTimeout - Read timeout (in seconds) of the client
# + writeTimeout - Write timeout (in seconds) of the client
# + secureSocket - SSL/TLS-related options
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes.
# If this is not set, is negative, or is zero, the default frame size of 65536 will be used
# + webSocketCompressionEnabled - Enable support for compression in the WebSocket
# + handShakeTimeout - Time (in seconds) that a connection waits to get the response of
# the WebSocket handshake. If the timeout exceeds, then the connection is terminated with
# an error. If the value < 0, then the value sets to the default value(300)
# + cookies - An Array of `http:Cookie`
# + auth - Configurations related to client authentication
# + pingPongHandler - A service to handle the ping/pong frames.
# Resources in this service gets called on the receipt of ping/pong frames from the server
# + retryConfig - Retry-related configurations
public type CommonClientConfiguration record {|
    string[] subProtocols = [];
    map<string> customHeaders = {};
    decimal readTimeout = -1;
    decimal writeTimeout = -1;
    ClientSecureSocket? secureSocket = ();
    int maxFrameSize = 65536;
    boolean webSocketCompressionEnabled = true;
    decimal handShakeTimeout = 300;
    http:Cookie[] cookies?;
    ClientAuthConfig auth?;
    PingPongService pingPongHandler?;
    WebSocketRetryConfig? retryConfig = ();
|};

# Configures the SSL/TLS options to be used for WebSocket client.
public type ClientSecureSocket record {|
    *http:ClientSecureSocket;
|};

# Retry configurations for WebSocket.
#
# + maxCount - The maximum number of retry attempts. If the count is zero, the client will retry indefinitely
# + interval - The number of seconds to delay before attempting to reconnect
# + backOffFactor - The rate of increase of the reconnect delay. Allows reconnect attempts to back off when problems
# persist
# + maxWaitInterval - Maximum time of the retry interval in seconds
public type WebSocketRetryConfig record {|
    int maxCount = 0;
    decimal interval = 1;
    float backOffFactor = 1.0;
    decimal maxWaitInterval = 30;
|};

type ClientInferredConfig record {|
    string[] subProtocols;
    map<string> customHeaders;
    decimal readTimeout;
    decimal writeTimeout;
    ClientSecureSocket? secureSocket;
    int maxFrameSize;
    boolean webSocketCompressionEnabled;
    decimal handShakeTimeout;
    WebSocketRetryConfig? retryConfig;
|};

# Adds cookies to the custom header.
#
# + config - Represents the cookies to be added
public isolated function addCookies(ClientConfiguration config) {
   string cookieHeader = "";
   var cookiesToAdd = config["cookies"];
   if cookiesToAdd is http:Cookie[] {
       http:Cookie[] sortedCookies = cookiesToAdd.sort(array:ASCENDING, isolated function(http:Cookie c) returns int {
           var cookiePath = c.path;
           int l = 0;
           if cookiePath is string {
               l = cookiePath.length();
           }
          return l;
       });
       foreach var cookie in sortedCookies {
           cookieHeader = cookieHeader + cookie.name + EQUALS + cookie.value + SEMICOLON + SPACE;
       }
       lock {
           updateLastAccessedTime(cookiesToAdd);
       }
       if cookieHeader != "" {
           cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 2);
           map<string> headers = config["customHeaders"];
           headers["Cookie"] = cookieHeader;
           config["customHeaders"] = headers;
       }
   }
}

isolated function updateLastAccessedTime(http:Cookie[] cookiesToAdd) {
    http:Cookie[] tempCookies = [];
    int endValue = cookiesToAdd.length();
    foreach var i in 0 ..< endValue {
        http:Cookie cookie = cookiesToAdd.pop();
        time:Utc lastAccessedTime = time:utcNow();
        tempCookies.push(getClone(cookie, cookie.createdTime, lastAccessedTime));
    }

    foreach var i in 0 ..< endValue {
        cookiesToAdd.push(tempCookies.pop());
    }
}

isolated function getClone(http:Cookie cookie, time:Utc createdTime, time:Utc lastAccessedTime) returns http:Cookie {
    http:CookieOptions options = {};
    if cookie.domain is string {
        options.domain = <string> cookie.domain;
    }
    if cookie.path is string {
        options.path = <string> cookie.path;
    }
    if cookie.expires is string {
        options.expires = <string> cookie.expires;
    }
    options.maxAge = cookie.maxAge;
    options.httpOnly = cookie.httpOnly;
    options.secure = cookie.secure;
    options.hostOnly = cookie.hostOnly;
    options.createdTime = createdTime;
    options.lastAccessedTime = lastAccessedTime;
    return new http:Cookie(cookie.name, cookie.value, options);
}

isolated function getString(anydata data) returns string {
    string text = "";
    if data is string {
        text = data;
    } else if data is xml {
        text = data.toString();
    } else {
        text = data.toJsonString();
    }
    return text;
}

const EQUALS = "=";
const SPACE = " ";
const SEMICOLON = ";";

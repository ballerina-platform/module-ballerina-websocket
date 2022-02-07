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
import ballerina/http;

# This is used for creating WebSocket server endpoints. A WebSocket server endpoint is capable of responding to
# remote callers. The `Listener` is responsible for initializing the endpoint using the provided configurations.
public class Listener {

    private int port = 0;
    private ListenerConfiguration config = {};
    private string instanceId;
    private http:Listener? httpListener = ();

    # Starts the registered service programmatically.
    #
    # + return - An `error` if an error occurred during the listener starting process
    public isolated function 'start() returns error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.serviceendpoint.Start",
         name: "start"
    } external;

    # Stops the service listener gracefully. Already-accepted requests will be served before connection closure.
    #
    # + return - An `error` if an error occurred during the listener stopping process
    public isolated function gracefulStop() returns error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.serviceendpoint.GracefulStop",
        name: "gracefulStop"
    } external;

    # Stops the service listener immediately. It is not implemented yet.
    #
    # + return - An `error` if an error occurred during the listener stop process
    public isolated function immediateStop() returns error? {
        error err = error("not implemented");
        return err;
    }

    # Attaches a service to the listener.
    #
    # + websocketService - The service that needs to be attached
    # + name - Name of the service
    # + return - An `error` if an error occurred during the service attachment process or else `()`
    public isolated function attach(UpgradeService websocketService, string[]|string? name = ()) returns error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.serviceendpoint.Register",
        name: "register"
    } external;


    # Detaches a WebSocket service from the listener. Note that detaching a WebSocket service would not affect
    # The functionality of the existing connections.
    #
    # + websocketService - The service to be detached
    # + return - An `error` if one occurred during detaching of a service or else `()`
    public isolated function detach(UpgradeService websocketService) returns error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.serviceendpoint.Detach",
        name: "detach"
    } external;

    # Gets invoked during the module initialization to initialize the listener.
    #
    # + port - Listening port of the WebSocket service listener
    # + config - Configurations for the WebSocket service listener
    public isolated function init(int|http:Listener 'listener, *ListenerConfiguration config) returns Error? {
        self.instanceId = uuid();
        self.config = config;
        if 'listener is http:Listener {
           self.httpListener = 'listener;
        } else {
           self.port = 'listener;
        }
        return self.externInitEndpoint();
    }

    isolated function externInitEndpoint() returns Error? = @java:Method {
        'class: "io.ballerina.stdlib.websocket.serviceendpoint.InitEndpoint",
        name: "initEndpoint"
    } external;
}

# Provides a set of configurations for HTTP service endpoints.
#
# + host - The host name/IP of the endpoint
# + http1Settings - Configurations related to HTTP/1.x protocol
# + secureSocket - The SSL configurations for the service endpoint. This needs to be configured in order to
# communicate through WSS
# + timeout - Period of time in seconds that a connection waits for a read/write operation in the
# initial upgrade request. Use value 0 to disable timeout
# + server - The server name which should appear as a response header
# + webSocketCompressionEnabled - Enable support for compression in WebSocket
# + requestLimits - Configurations associated with inbound request size limits
public type ListenerConfiguration record {|
    string host = "0.0.0.0";
    ListenerHttp1Settings http1Settings = {};
    ListenerSecureSocket secureSocket?;
    decimal timeout = 120;
    string? server = ();
    boolean webSocketCompressionEnabled = true;
    RequestLimitConfigs requestLimits = {};
|};

# Provides settings related to HTTP/1.x protocol.
public type ListenerHttp1Settings record {|
    *http:ListenerHttp1Settings;
|};

# Provides inbound request URI, total header and entity body size threshold configurations.
public type RequestLimitConfigs record {|
    *http:RequestLimitConfigs;
|};


# Configures the SSL/TLS options to be used for WebSocket service.
public type ListenerSecureSocket record {|
    *http:ListenerSecureSocket;
|};

# Returns a random UUID string.
#
# + return - The random string
isolated function uuid() returns string {
    var result = java:toString(nativeUuid());
    if result is string {
        return result;
    } else {
        panic error("Error occured when converting the UUID to string.");
    }
}

isolated function nativeUuid() returns handle = @java:Method {
    name: "randomUUID",
    'class: "java.util.UUID"
} external;

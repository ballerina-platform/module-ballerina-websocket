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

/////////////////////////////
/// Websocket Listener Endpoint ///
/////////////////////////////
# This is used for creating Websocket server endpoints. An Websocket server endpoint is capable of responding to
# remote callers. The `Listener` is responsible for initializing the endpoint using the provided configurations.
public class Listener {

    private int port = 0;
    private ListenerConfiguration config = {};
    private string instanceId;
    private http:Listener? httpListener = ();

    # Starts the registered service programmatically.
    #
    # + return - An `error` if an error occurred during the listener starting process
    public isolated function 'start() returns error? {
        return self.startEndpoint();
    }

    # Stops the service listener gracefully. Already-accepted requests will be served before connection closure.
    #
    # + return - An `error` if an error occurred during the listener stopping process
    public isolated function gracefulStop() returns error? {
        return self.gracefulStopS();
    }

    # Stops the service listener immediately. It is not implemented yet.
    #
    # + return - An `error` if an error occurred during the listener stop process
    public isolated function immediateStop() returns error? {
        error err = error("not implemented");
        return err;
    }

    # Attaches a service to the listener.
    #
    # + s - The service that needs to be attached
    # + name - Name of the service
    # + return - An `error` an error occurred during the service attachment process or else nil
    public isolated function attach(Service s, string[]|string? name = ()) returns error? {
        return self.register(s, name);
    }

    # Detaches a Http or WebSocket service from the listener. Note that detaching a WebSocket service would not affect
    # The functionality of the existing connections.
    #
    # + s - The service to be detached
    # + return - An `error` if one occurred during detaching of a service or else `()`
    public isolated function detach(Service s) returns error? {
        return self.detachS(s);
    }

    # Gets invoked during module initialization to initialize the listener.
    #
    # + port - Listening port of the websocket service listener
    # + config - Configurations for the websocket service listener
    public isolated function init(int|http:Listener 'listener, *ListenerConfiguration config) returns Error? {
        self.instanceId = uuid();
        self.config = config;
        if ('listener is http:Listener) {
           self.httpListener = 'listener;
        } else {
           self.port = 'listener;
        }
        return self.initEndpoint();
    }

    public isolated function initEndpoint() returns Error? {
        return externInitEndpoint(self);
    }

    # Gets invoked when attaching a service to the endpoint.
    #
    # + s - The service that needs to be attached
    # + name - Name of the service
    # + return - An `error` if an error occurred during the service attachment process or else nil
    isolated function register(Service s, string[]|string? name) returns error? {
        return externRegister(self, s, name);
    }

    # Starts the registered service.
    #
    # + return - An `error` if an error occurred during the listener start process
    isolated function startEndpoint() returns error? {
        return externStart(self);
    }

    # Stops the service listener gracefully.
    #
    # + return - An `error` if an error occurred during the listener stop process
    isolated function gracefulStopS() returns error? {
        return externGracefulStop(self);
    }

    # Disengage an attached service from the listener.
    #
    # + s - The service that needs to be detached
    # + return - An `error` if an error occurred during the service detachment process or else nil
    isolated function detachS(Service s) returns error? {
        return externDetach(self, s);
    }
}

isolated function externInitEndpoint(Listener listenerObj) returns Error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.serviceendpoint.InitEndpoint",
    name: "initEndpoint"
} external;

isolated function externRegister(Listener listenerObj, Service s, string[]|string? name) returns error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.serviceendpoint.Register",
    name: "register"
} external;

isolated function externStart(Listener listenerObj) returns error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.serviceendpoint.Start",
    name: "start"
} external;

isolated function externGracefulStop(Listener listenerObj) returns error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.serviceendpoint.GracefulStop",
    name: "gracefulStop"
} external;

isolated function externDetach(Listener listenerObj, Service s) returns error? = @java:Method {
    'class: "org.ballerinalang.net.websocket.serviceendpoint.Detach",
    name: "detach"
} external;

# Presents a read-only view of the remote address.
#
# + host - The remote host IP
# + port - The remote port
public type Remote record {|
    string host = "";
    int port = 0;
|};

# Presents a read-only view of the local address.
#
# + host - The local host name/IP
# + port - The local port
public type Local record {|
    string host = "";
    int port = 0;
|};

# Provides a set of configurations for HTTP service endpoints.
#
# + host - The host name/IP of the endpoint
# + http1Settings - Configurations related to HTTP/1.x protocol
# + secureSocket - The SSL configurations for the service endpoint. This needs to be configured in order to
#                  communicate through WSS.
# + timeout - Period of time in seconds that a connection waits for a read/write operation in the
#                     initial upgrade request. Use value 0 to disable timeout
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
    if (result is string) {
        return result;
    } else {
        panic error("Error occured when converting the UUID to string.");
    }
}

isolated function nativeUuid() returns handle = @java:Method {
    name: "randomUUID",
    'class: "java.util.UUID"
} external;

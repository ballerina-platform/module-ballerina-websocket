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

# Configurations for a WebSocket service.
#
# + subProtocols - Negotiable sub protocol by the service
# + idleTimeout - Idle timeout for the client connection. Upon timeout, `onIdleTimeout` resource (if defined)
# in the server service will be triggered. Note that this overrides the `timeout` config
# in the `websocket:Listener`, which is applicable only for the initial HTTP upgrade request
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes.
# If this is not set or is negative or zero, the default frame size, which is 65536 will be used
# + auth - Listener authentication configurations
# + validation - Enable/disable constraint validation
# + dispatcherKey - The key which is going to be used for dispatching to custom remote functions.
# + dispatcherStreamId - The identifier used to distinguish between requests and their corresponding responses in a multiplexing scenario.
# + connectionClosureTimeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint 
# before closing the connection. If the timeout exceeds, then the connection is terminated even though a close frame is
# not received from the remote endpoint. If the value is -1, then the connection waits until a close frame is
# received, and any other negative value results in an error. If the WebSocket frame is received from the remote endpoint
# within the waiting period, the connection is terminated immediately.
public type WSServiceConfig record {|
    string[] subProtocols = [];
    decimal idleTimeout = 0;
    int maxFrameSize = 65536;
    ListenerAuthConfig[] auth?;
    boolean validation = true;
    string dispatcherKey?;
    string dispatcherStreamId?;
    decimal connectionClosureTimeout = 60;
|};

# The annotation which is used to configure a WebSocket service.
public annotation WSServiceConfig ServiceConfig on service;

# Configurations used to define dispatching rules for remote functions.
#
# + value - The value which is going to be used for dispatching to custom remote functions.
public type WsDispatcherMapping record {|
    string value;
|};

# The annotation which is used to configure the dispatching rules for WebSocket remote functions.
public const annotation WsDispatcherMapping DispatcherMapping on function;

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

///////////////////////////
/// Service Annotations ///
///////////////////////////

# Configurations for a WebSocket service.
#
# + path - Path of the WebSocket service
# + subProtocols - Negotiable sub protocol by the service
# + idleTimeoutInSeconds - Idle timeout for the client connection. Upon timeout, `onIdleTimeout` resource (if defined)
#                          in the server service will be triggered. Note that this overrides the `timeoutInMillis` config
#                          in the `http:Listener`.
# + maxFrameSize - The maximum payload size of a WebSocket frame in bytes.
#                  If this is not set or is negative or zero, the default frame size will be used.
public type WSServiceConfig record {|
    string path = "";
    string[] subProtocols = [];
    int idleTimeoutInSeconds = 0;
    int maxFrameSize = 0;
|};

# The annotation which is used to configure a WebSocket service.
public annotation WSServiceConfig WebSocketServiceConfig on service;
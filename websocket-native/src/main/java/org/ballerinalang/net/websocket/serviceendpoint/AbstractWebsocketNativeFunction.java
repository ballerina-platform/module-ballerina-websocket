/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websocket.serviceendpoint;

import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.net.transport.contract.ServerConnector;
import org.ballerinalang.net.websocket.server.WebSocketServicesRegistry;

import static org.ballerinalang.net.websocket.WebSocketConstants.CONNECTOR_STARTED;
import static org.ballerinalang.net.websocket.WebSocketConstants.HTTP_SERVER_CONNECTOR;
import static org.ballerinalang.net.websocket.WebSocketConstants.WS_SERVICE_REGISTRY;

/**
 * Includes common functions to all the actions.
 */
public class AbstractWebsocketNativeFunction {
    protected static WebSocketServicesRegistry getWebSocketServicesRegistry(BObject serviceEndpoint) {
        return (WebSocketServicesRegistry) serviceEndpoint.getNativeData(WS_SERVICE_REGISTRY);
    }

    static boolean isConnectorStarted(BObject serviceEndpoint) {
        return serviceEndpoint.getNativeData(CONNECTOR_STARTED) != null && (Boolean) serviceEndpoint
                .getNativeData(CONNECTOR_STARTED);
    }

    protected static ServerConnector getServerConnector(BObject serviceEndpoint) {
        return (ServerConnector) serviceEndpoint.getNativeData(HTTP_SERVER_CONNECTOR);
    }

    static void resetRegistry(BObject serviceEndpoint) {
        WebSocketServicesRegistry webSocketServicesRegistry = new WebSocketServicesRegistry();
        serviceEndpoint.addNativeData(WS_SERVICE_REGISTRY, webSocketServicesRegistry);
    }
}

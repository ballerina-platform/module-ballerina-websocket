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

package io.ballerina.stdlib.websocket.serviceendpoint;

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import org.ballerinalang.net.transport.contract.ServerConnector;
import io.ballerina.stdlib.websocket.server.WebSocketServicesRegistry;

/**
 * Includes common functions to all the actions.
 */
public class AbstractWebsocketNativeFunction {
    protected static WebSocketServicesRegistry getWebSocketServicesRegistry(BObject serviceEndpoint) {
        return (WebSocketServicesRegistry) serviceEndpoint.getNativeData(WebSocketConstants.WS_SERVICE_REGISTRY);
    }

    static boolean isConnectorStarted(BObject serviceEndpoint) {
        return serviceEndpoint != null && serviceEndpoint.getNativeData(WebSocketConstants.CONNECTOR_STARTED) != null
                && (Boolean) serviceEndpoint.getNativeData(WebSocketConstants.CONNECTOR_STARTED);
    }

    protected static ServerConnector getServerConnector(BObject serviceEndpoint) {
        return (ServerConnector) serviceEndpoint.getNativeData(WebSocketConstants.HTTP_SERVER_CONNECTOR);
    }

    static void resetRegistry(BObject serviceEndpoint) {
        WebSocketServicesRegistry webSocketServicesRegistry = new WebSocketServicesRegistry();
        serviceEndpoint.addNativeData(WebSocketConstants.WS_SERVICE_REGISTRY, webSocketServicesRegistry);
    }
}

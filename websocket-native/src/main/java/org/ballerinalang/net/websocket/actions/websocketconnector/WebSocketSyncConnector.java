/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.net.websocket.actions.websocketconnector;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.client.listener.SyncClientConnectorListener;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ballerinalang.net.websocket.WebSocketUtil.findTimeoutInSeconds;

/**
 * Utilities related to websocket synchronous client connector read actions.
 */
public class WebSocketSyncConnector {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSyncConnector.class);

    public static Object externReadTextMessage(Environment env, BObject wsConnection) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }

    private static void readContentFromConnection(BObject wsConnection, Future callback) throws IllegalAccessException {
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        SyncClientConnectorListener connectorListener = (SyncClientConnectorListener) wsConnection
                .getNativeData(WebSocketConstants.CLIENT_LISTENER);
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        long readTimeoutInSeconds = findTimeoutInSeconds(
                connectionInfo.getWebSocketEndpoint().getMapValue(WebSocketConstants.CLIENT_ENDPOINT_CONFIG),
                WebSocketConstants.ANNOTATION_ATTR_READ_IDLE_TIMEOUT, 0);
        connectionInfo.getWebSocketConnection().addReadIdleStateHandler(readTimeoutInSeconds);
        connectorListener.setCallback(callback);
        connectionInfo.getWebSocketConnection().readNextFrame();
    }

    public static Object externReadBinaryMessage(Environment env, BObject wsConnection) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }

    public static Object externReadMessage(Environment env, BObject wsConnection) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }
}

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
package io.ballerina.stdlib.websocket.actions.websocketconnector;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.client.listener.SyncClientConnectorListener;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities related to websocket synchronous client connector read actions.
 */
public class WebSocketSyncConnector {

    public static Object readTextMessage(Environment env, BObject wsConnection) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }

    private static void readContentFromConnection(BObject wsConnection, Future callback, BTypedesc... targetType)
            throws IllegalAccessException {
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        connectionInfo.addCallback(callback);
        if (!connectionInfo.getWebSocketConnection().isOpen()) {
            callback.complete(WebSocketUtil.createWebsocketError("Connection already closed",
                            WebSocketConstants.ErrorCode.ConnectionClosureError));
            return;
        }
        SyncClientConnectorListener connectorListener = (SyncClientConnectorListener) wsConnection
                .getNativeData(WebSocketConstants.CLIENT_LISTENER);
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        long readTimeoutInSeconds = WebSocketUtil.findTimeoutInSeconds(
                connectionInfo.getWebSocketEndpoint().getMapValue(WebSocketConstants.CLIENT_ENDPOINT_CONFIG),
                WebSocketConstants.ANNOTATION_ATTR_READ_IDLE_TIMEOUT, 0);
        connectionInfo.getWebSocketConnection().addReadIdleStateHandler(readTimeoutInSeconds);
        connectorListener.setCallback(callback);
        if (targetType.length > 0) {
            connectorListener.setTargetType(targetType[0]);
        }
        connectorListener.setFutureCompleted(new AtomicBoolean(false));
        connectionInfo.getWebSocketConnection().readNextFrame();
    }

    public static Object readBinaryMessage(Environment env, BObject wsConnection) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }

    public static Object readMessage(Environment env, BObject wsConnection, BTypedesc targetType) {
        final Future callback = env.markAsync();
        try {
            readContentFromConnection(wsConnection, callback, targetType);
        } catch (IllegalAccessException e) {
            return WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError);
        }
        return null;
    }

    private WebSocketSyncConnector() {}
}

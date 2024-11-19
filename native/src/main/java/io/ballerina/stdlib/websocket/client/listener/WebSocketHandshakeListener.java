/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websocket.client.listener;

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.websocket.ClientHandshakeListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.message.HttpCarbonResponse;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The `WebSocketHandshakeListener` implements the `{@link ClientHandshakeListener}` interface directly.
 *
 * @since 1.2.0
 */
public class WebSocketHandshakeListener implements ClientHandshakeListener {

    private final WebSocketService wsService;
    private final SyncClientConnectorListener connectorListener;
    private final BObject webSocketClient;
    private WebSocketConnectionInfo connectionInfo;
    private final CompletableFuture<Object> balFuture;
    AtomicBoolean callbackCompleted;

    public WebSocketHandshakeListener(BObject webSocketClient, WebSocketService wsService,
                                      SyncClientConnectorListener connectorListener, CompletableFuture<Object> future,
                                      AtomicBoolean callbackCompleted) {
        this.webSocketClient = webSocketClient;
        this.wsService = wsService;
        this.connectorListener = connectorListener;
        this.balFuture = future;
        this.callbackCompleted = callbackCompleted;
    }

    @Override
    public void onSuccess(WebSocketConnection webSocketConnection, HttpCarbonResponse carbonResponse) {
        webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(carbonResponse));
        WebSocketUtil.populatWebSocketEndpoint(webSocketConnection, webSocketClient);
        setWebSocketOpenConnectionInfo(webSocketConnection, webSocketClient, wsService);
        connectorListener.setConnectionInfo(connectionInfo);
        webSocketConnection.removeReadIdleStateHandler();
        if (!callbackCompleted.get()) {
            balFuture.complete(null);
            callbackCompleted.set(true);
        }
        WebSocketObservabilityUtil.observeConnection(connectionInfo);
    }

    @Override
    public void onError(Throwable t, HttpCarbonResponse response) {
        if (response != null) {
            webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(response));
        }
        setWebSocketOpenConnectionInfo(null, webSocketClient, wsService);
        if (!callbackCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(t));
            callbackCompleted.set(true);
        }
    }

    private void setWebSocketOpenConnectionInfo(WebSocketConnection webSocketConnection,
            BObject webSocketClient, WebSocketService wsService) {
        this.connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketClient);
        webSocketClient.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO, connectionInfo);
    }
}

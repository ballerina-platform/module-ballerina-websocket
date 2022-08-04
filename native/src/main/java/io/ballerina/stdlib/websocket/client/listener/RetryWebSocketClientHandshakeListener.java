/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerina.stdlib.websocket.client.listener;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.websocket.ClientHandshakeListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.message.HttpCarbonResponse;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.client.RetryContext;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The retry handshake listener for the client.
 *
 */
public class RetryWebSocketClientHandshakeListener implements ClientHandshakeListener {

    private final WebSocketService wsService;
    private final SyncClientConnectorListener connectorListener;
    private final BObject webSocketClient;
    private WebSocketConnectionInfo connectionInfo;
    private Future balFuture;
    private RetryContext retryConfig;
    AtomicBoolean callbackCompleted;
    private static final Logger logger = LoggerFactory.getLogger(RetryWebSocketClientHandshakeListener.class);

    public RetryWebSocketClientHandshakeListener(BObject webSocketClient, WebSocketService wsService,
                                                 SyncClientConnectorListener connectorListener, Future future,
                                                 RetryContext retryConfig, AtomicBoolean callbackCompleted) {
        this.webSocketClient = webSocketClient;
        this.wsService = wsService;
        this.connectorListener = connectorListener;
        this.balFuture = future;
        this.retryConfig = retryConfig;
        this.callbackCompleted = callbackCompleted;
    }

    @Override
    public void onSuccess(WebSocketConnection webSocketConnection, HttpCarbonResponse carbonResponse) {
        webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(carbonResponse));
        WebSocketUtil.populatWebSocketEndpoint(webSocketConnection, webSocketClient);
        setWebSocketOpenConnectionInfo(webSocketConnection, webSocketClient, wsService);
        connectorListener.setConnectionInfo(connectionInfo);
        if (retryConfig.isFirstConnectionMadeSuccessfully()) {
            webSocketConnection.readNextFrame();
        } else {
            if (!callbackCompleted.get()) {
                balFuture.complete(null);
                callbackCompleted.set(true);
            }
        }
        WebSocketObservabilityUtil.observeConnection(connectionInfo);
        WebSocketUtil.adjustContextOnSuccess(retryConfig);
    }

    @Override
    public void onError(Throwable throwable, HttpCarbonResponse response) {
        if (response != null) {
            webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(response));
        }
        setWebSocketOpenConnectionInfo(null, webSocketClient, wsService);
        if (throwable instanceof IOException && WebSocketUtil.reconnect(connectionInfo, balFuture, callbackCompleted)) {
            return;
        }
        if (!callbackCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(throwable));
            callbackCompleted.set(true);
        }
    }

    private void setWebSocketOpenConnectionInfo(WebSocketConnection webSocketConnection,
                                                BObject webSocketClient, WebSocketService wsService) {
        this.connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketClient);
        webSocketClient.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO, connectionInfo);
    }
}

/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.server;

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.transport.contract.websocket.ServerHandshakeListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketResourceDispatcher;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ServerHandshakeListener that dispatches the onOpen resource onSuccess.
 */
class UpgradeListener implements ServerHandshakeListener {
    private static final Logger logger = LoggerFactory.getLogger(UpgradeListener.class);

    private final WebSocketServerService wsService;
    private final Object dispatchingService;
    private final WebSocketConnectionManager connectionManager;

    UpgradeListener(WebSocketServerService wsService, WebSocketConnectionManager connectionManager,
            Object dispatchingService) {
        this.wsService = wsService;
        this.connectionManager = connectionManager;
        this.dispatchingService = dispatchingService;
    }

    @Override
    public void onSuccess(WebSocketConnection webSocketConnection) {
        BObject webSocketCaller = WebSocketUtil.createAndPopulateWebSocketCaller(webSocketConnection, wsService,
                connectionManager);
        wsService.addWsService(webSocketConnection.getChannelId(), dispatchingService);
        WebSocketResourceDispatcher.dispatchOnOpen(webSocketConnection, webSocketCaller, wsService);
    }

    @Override
    public void onError(Throwable throwable) {
        String msg = "Unable to complete WebSocket handshake: ";
        logger.error(msg, throwable);
        throw WebSocketUtil.getWebSocketError("", throwable, WebSocketConstants.ErrorCode.Error.
                errorCode(), null);
    }
}

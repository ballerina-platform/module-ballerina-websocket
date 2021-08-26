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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketBinaryMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketCloseMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnectorListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketControlMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketHandshaker;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketTextMessage;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketResourceDispatcher;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * SyncClientConnectorListener implements {@link WebSocketConnectorListener} interface directly.
 *
 */
public class SyncClientConnectorListener implements WebSocketConnectorListener {

    private WebSocketConnectionInfo connectionInfo = null;
    private Future callback;
    private AtomicBoolean futureCompleted;
    private static final Logger logger = LoggerFactory.getLogger(SyncClientConnectorListener.class);

    public void setConnectionInfo(WebSocketConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void onHandshake(WebSocketHandshaker webSocketHandshaker) {}

    @Override
    public void onMessage(WebSocketTextMessage webSocketTextMessage) {
        try {
            WebSocketConnectionInfo.StringAggregator stringAggregator = connectionInfo
                    .createIfNullAndGetStringAggregator();
            boolean finalFragment = webSocketTextMessage.isFinalFragment();
            if (finalFragment) {
                stringAggregator.appendAggregateString(webSocketTextMessage.getText());
                BString txtMsg = StringUtils.fromString(stringAggregator.getAggregateString());
                stringAggregator.resetAggregateString();
                if (!futureCompleted.get()) {
                    callback.complete(txtMsg);
                    futureCompleted.set(true);
                }
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            } else {
                stringAggregator.appendAggregateString(webSocketTextMessage.getText());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException e) {
            callback.complete(WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            futureCompleted.set(true);
        }
    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {
        System.out.println("-------------on binary message----------");
        try {
            WebSocketConnectionInfo.ByteArrAggregator byteArrAggregator = connectionInfo
                    .createIfNullAndGetByteArrAggregator();
            boolean finalFragment = webSocketBinaryMessage.isFinalFragment();
            if (finalFragment) {
                byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
                byte[] binMsg = byteArrAggregator.getAggregateByteArr();
                byteArrAggregator.resetAggregateByteArr();
                callback.complete(ValueCreator.createArrayValue(binMsg));
                futureCompleted.set(true);
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            } else {
                byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException | IOException e) {
            callback.complete(WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            futureCompleted.set(true);
        }
    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {
        WebSocketResourceDispatcher.dispatchOnPingOnPong(connectionInfo, webSocketControlMessage, false);
    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {
        try {
            if (callback != null && !futureCompleted.get()) {
            int closeCode = webSocketCloseMessage.getCloseCode();
            String closeReason = webSocketCloseMessage.getCloseReason() == null ||
                    webSocketCloseMessage.getCloseReason().equals("") ?
                    "Connection closed: Status code: " + closeCode :
                    webSocketCloseMessage.getCloseReason() + ": Status code: " + closeCode;
            System.out.println("####################### on close message SyncClientConnectorListener ###############");
            if (WebSocketUtil.hasRetryConfig(connectionInfo.getWebSocketEndpoint())) {
                if (closeCode == WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE &&
                        WebSocketUtil.reconnect(connectionInfo, callback)) {
                    return;
                } else {
                    if (closeCode != WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE) {
                        logger.debug(WebSocketConstants.LOG_MESSAGE, "Reconnect attempt not made because of " +
                                "close initiated by the server: ", connectionInfo.getWebSocketEndpoint()
                                .getStringValue(WebSocketConstants.CLIENT_URL_CONFIG));
                    }
                }
            }
            if (!futureCompleted.get()) {
                callback.complete(WebSocketUtil
                        .createWebsocketError(closeReason, WebSocketConstants.ErrorCode.ConnectionClosureError));
                futureCompleted.set(true);
            }
            WebSocketConnection wsConnection = connectionInfo.getWebSocketConnection();
            wsConnection.removeReadIdleStateHandler();
            WebSocketResourceDispatcher.finishConnectionClosureIfOpen(wsConnection, closeCode, connectionInfo);
            }
        } catch (IllegalAccessException e) {
            callback.complete(WebSocketUtil.createWebsocketError("Connection already closed",
                    WebSocketConstants.ErrorCode.ConnectionClosureError));
        }
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {
        try {
            if (callback != null && !futureCompleted.get()) {
                BObject webSocketClient = connectionInfo.getWebSocketEndpoint();
                System.out.println("####################### on error message SyncClientConnectorListener ###############");
                if (WebSocketUtil.hasRetryConfig(webSocketClient) && throwable instanceof IOException &&
                        WebSocketUtil.reconnect(connectionInfo, callback)){
                    return;
                }
                callback.complete(WebSocketUtil
                        .createWebsocketError(throwable.getMessage(), WebSocketConstants.ErrorCode.Error));
                futureCompleted.set(true);
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            }
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        }
    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage controlMessage) {
        try {
            if (!futureCompleted.get()) {
                callback.complete(WebSocketUtil.createWebsocketError(
                        "Read timed out", WebSocketConstants.ErrorCode.ReadTimedOutError));
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
                futureCompleted.set(true);
            }
        } catch (IllegalAccessException e) {
            // Ignore as it is not possible have an Illegal access
        }
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection) {
        WebSocketObservabilityUtil.observeClose(connectionInfo);
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
        } catch (IllegalAccessException e) {
            // Ignore as at this point connection closure error is returned to the user
        }
    }

    public void setCallback(Future callback) {
        this.callback = callback;
    }

    public void setFutureCompleted(AtomicBoolean futureCompleted) {
        this.futureCompleted = futureCompleted;
    }
}

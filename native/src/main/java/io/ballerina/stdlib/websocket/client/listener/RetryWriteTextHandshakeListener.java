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

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.websocket.ClientHandshakeListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.message.HttpCarbonResponse;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector;
import io.ballerina.stdlib.websocket.client.RetryContext;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.release;

/**
 * A class for handling the handshake of writing text messages after retrying.
 */
public class RetryWriteTextHandshakeListener implements ClientHandshakeListener {
    private final String message;
    private final BObject clientEndpoint;
    private final SyncClientConnectorListener connectorListener;
    private WebSocketConnectionInfo connectionInfo;
    private final CompletableFuture<Object> balFuture;
    private AtomicBoolean textCallbackCompleted;
    private static final Logger logger = LoggerFactory.getLogger(RetryWriteTextHandshakeListener.class);

    public RetryWriteTextHandshakeListener(String message, BObject clientEndpoint,
                                           SyncClientConnectorListener connectorListener,
                                           CompletableFuture<Object> balFuture, AtomicBoolean textCallbackCompleted) {
        this.message = message;
        this.clientEndpoint = clientEndpoint;
        this.connectorListener = connectorListener;
        this.balFuture = balFuture;
        this.textCallbackCompleted = textCallbackCompleted;
    }

    @Override
    public void onSuccess(WebSocketConnection webSocketConnection, HttpCarbonResponse httpCarbonResponse) {
        clientEndpoint.addNativeData(WebSocketConstants.HTTP_RESPONSE,
                HttpUtil.createResponseStruct(httpCarbonResponse));
        WebSocketUtil.populatWebSocketEndpoint(webSocketConnection, clientEndpoint);
        setWebSocketOpenConnectionInfo(webSocketConnection, clientEndpoint,
                (WebSocketService) clientEndpoint.getNativeData(WebSocketConstants.CALL_BACK_SERVICE));
        connectorListener.setConnectionInfo(connectionInfo);
        webSocketConnection.removeReadIdleStateHandler();
        WebSocketObservabilityUtil.observeConnection(connectionInfo);
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) clientEndpoint
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        ByteBuf byteBuf = null;
        ByteBuf lastSlice = null;
        try {
            WebSocketConnector.setWriteTimeoutHandler(clientEndpoint, balFuture, textCallbackCompleted, connectionInfo);
            byteBuf = WebSocketConnector.fromText(message);
            int noBytes = byteBuf.readableBytes();
            int index = 0;
            final int size = (int) connectionInfo.getWebSocketEndpoint()
                    .getNativeData(WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE);
            while (index < noBytes - size) {
                ByteBuf slice = null;
                try {
                    slice = byteBuf.retainedSlice(index, size);
                    String chunk = slice.toString(CharsetUtil.UTF_8);
                    ChannelFuture future = connectionInfo.getWebSocketConnection().pushText(chunk, false);
                    promiseCombiner.add(future);
                    index += size;
                } finally {
                    release(slice);
                }
            }
            lastSlice = byteBuf.retainedSlice(index, noBytes - index);
            String chunk = lastSlice.toString(CharsetUtil.UTF_8);
            ChannelFuture future = connectionInfo.getWebSocketConnection().pushText(chunk, true);
            promiseCombiner.add(future);
            promiseCombiner.finish(connectionInfo.getWebSocketConnection().getChannel().newPromise()
                    .addListener((ChannelFutureListener) channelFuture -> {
                        WebSocketConnector.removeWriteTimeoutHandler(clientEndpoint, connectionInfo);
                        if (channelFuture.isSuccess()) {
                            WebSocketUtil.handleWebSocketCallback(balFuture, channelFuture, logger, connectionInfo,
                                    textCallbackCompleted);
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, connectionInfo);
                            WebSocketUtil.adjustContextOnSuccess((RetryContext) clientEndpoint
                                    .getNativeData(WebSocketConstants.RETRY_CONFIG.toString()));
                        } else {
                            if (!textCallbackCompleted.get()) {
                                WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, future.cause(),
                                        textCallbackCompleted);
                            }
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(clientEndpoint),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e, textCallbackCompleted);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
    }

    @Override
    public void onError(Throwable throwable, HttpCarbonResponse httpCarbonResponse) {
        setWebSocketOpenConnectionInfo(null, clientEndpoint,
                (WebSocketService) clientEndpoint.getNativeData(WebSocketConstants.CALL_BACK_SERVICE));
        if (throwable instanceof IOException && WebSocketUtil.reconnectForWrite(connectionInfo, balFuture,
                textCallbackCompleted, message, null)) {
            return;
        }
        if (!textCallbackCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(throwable));
            textCallbackCompleted.set(true);
        }
    }

    private void setWebSocketOpenConnectionInfo(WebSocketConnection webSocketConnection,
                                                BObject webSocketClient, WebSocketService wsService) {
        this.connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketClient);
        webSocketClient.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO, connectionInfo);
    }
}

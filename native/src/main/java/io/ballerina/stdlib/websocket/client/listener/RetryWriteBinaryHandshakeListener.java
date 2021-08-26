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
import io.ballerina.runtime.api.values.BArray;
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
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.release;

/**
 * A class for handling the handshake of writing binary messages after retrying.
 */
public class RetryWriteBinaryHandshakeListener implements ClientHandshakeListener {

    private final BArray message;
    private final BObject clientEndpoint;
    private final SyncClientConnectorListener connectorListener;
    private WebSocketConnectionInfo connectionInfo;
    private final Future balFuture;
    AtomicBoolean binaryCallbackCompleted;
    private static final Logger logger = LoggerFactory.getLogger(RetryWriteBinaryHandshakeListener.class);

    public RetryWriteBinaryHandshakeListener(BArray message, BObject clientEndpoint,
                                           SyncClientConnectorListener connectorListener, Future balFuture,
                                           AtomicBoolean textCallbackCompleted) {
        this.message = message;
        this.clientEndpoint = clientEndpoint;
        this.connectorListener = connectorListener;
        this.balFuture = balFuture;
        this.binaryCallbackCompleted = textCallbackCompleted;
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
            WebSocketConnector.setWriteTimeoutHandler(clientEndpoint, balFuture, binaryCallbackCompleted,
                    connectionInfo);
            byteBuf = WebSocketConnector.fromByteArray(ByteBuffer.wrap(message.getBytes()));
            int noBytes = byteBuf.readableBytes();
            int index = 0;
            final int size = (int) connectionInfo.getWebSocketEndpoint()
                    .getNativeData(WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE);
            while (index < noBytes - size) {
                ByteBuf slice = null;
                try {
                    slice = byteBuf.retainedSlice(index, size);
                    byte[] chunk = WebSocketConnector.getByteChunk(size, slice);
                    ChannelFuture future = connectionInfo.getWebSocketConnection()
                            .pushBinary(ByteBuffer.wrap(chunk), false);
                    promiseCombiner.add(future);
                    index += size;
                } finally {
                    release(slice);
                }
            }
            lastSlice = byteBuf.retainedSlice(index, noBytes - index);
            byte[] finalChunk = WebSocketConnector.getByteChunk(noBytes - index, lastSlice);
            ChannelFuture webSocketChannelFuture = connectionInfo.getWebSocketConnection()
                    .pushBinary(ByteBuffer.wrap(finalChunk), true);
            promiseCombiner.add(webSocketChannelFuture);
            promiseCombiner.finish(connectionInfo.getWebSocketConnection().getChannel().newPromise()
                    .addListener((ChannelFutureListener) future -> {
                        WebSocketConnector.removeWriteTimeoutHandler(clientEndpoint, connectionInfo);
                        if (webSocketChannelFuture.isSuccess()) {
                            WebSocketUtil.handleWebSocketCallback(balFuture, webSocketChannelFuture, logger,
                                    connectionInfo, binaryCallbackCompleted);
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, connectionInfo);
                            adjustContextOnSuccess((RetryContext) clientEndpoint
                                    .getNativeData(WebSocketConstants.RETRY_CONFIG.toString()));
                        } else {
                            if (!binaryCallbackCompleted.get()) {
                                WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, future.cause(),
                                        binaryCallbackCompleted);
                            }
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            logger.error("Error occurred when pushing binary data", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(clientEndpoint),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY, e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e, binaryCallbackCompleted);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
    }

    @Override
    public void onError(Throwable throwable, HttpCarbonResponse httpCarbonResponse) {
        if (httpCarbonResponse != null) {
            clientEndpoint.addNativeData(WebSocketConstants.HTTP_RESPONSE,
                    HttpUtil.createResponseStruct(httpCarbonResponse));
        }
        setWebSocketOpenConnectionInfo(null, clientEndpoint,
                (WebSocketService) clientEndpoint.getNativeData(WebSocketConstants.CALL_BACK_SERVICE));
        if (throwable instanceof IOException && WebSocketUtil.reconnectForWrite(connectionInfo, balFuture,
                binaryCallbackCompleted, null, message)) {
            return;
        }
        if (!binaryCallbackCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(throwable));
            binaryCallbackCompleted.set(true);
        }
    }

    private void setWebSocketOpenConnectionInfo(WebSocketConnection webSocketConnection,
                                                BObject webSocketClient, WebSocketService wsService) {
        this.connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketClient);
        webSocketClient.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO, connectionInfo);
    }

    private void adjustContextOnSuccess(RetryContext retryConfig) {
        retryConfig.setFirstConnectionMadeSuccessfully();
        retryConfig.setReconnectAttempts(0);
    }
}

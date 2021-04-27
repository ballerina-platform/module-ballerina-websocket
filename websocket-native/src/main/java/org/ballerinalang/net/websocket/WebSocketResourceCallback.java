/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.net.websocket;

import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import org.ballerinalang.net.transport.contract.websocket.WebSocketConnection;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityConstants;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.ballerinalang.net.websocket.WebSocketConstants.INITIALIZED_BY_SERVICE;
import static org.ballerinalang.net.websocket.WebSocketResourceDispatcher.dispatchOnError;
import static org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector.fromByteArray;
import static org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector.fromText;
import static org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector.getByteChunk;
import static org.ballerinalang.net.websocket.actions.websocketconnector.WebSocketConnector.release;
import static org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil.observeError;

/**
 * Callback impl for web socket.
 */
public class WebSocketResourceCallback implements Callback {

    private final WebSocketConnection webSocketConnection;
    private final WebSocketConnectionInfo connectionInfo;
    private final String resource;
    private static final Logger log = LoggerFactory.getLogger(WebSocketResourceCallback.class);

    WebSocketResourceCallback(WebSocketConnectionInfo webSocketConnectionInfo, String resource)
            throws IllegalAccessException {
        this.connectionInfo = webSocketConnectionInfo;
        this.webSocketConnection = connectionInfo.getWebSocketConnection();
        this.resource = resource;
    }

    @Override
    public void notifySuccess(Object result) {
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        if (result instanceof BArray && resource.equals(WebSocketConstants.RESOURCE_NAME_ON_PING)) {
            sendPing((BArray) result, promiseCombiner);
        } else if (result instanceof BString) {
            sendTextMessage((BString) result, promiseCombiner);
        } else if (result instanceof BArray) {
            sendBinaryMessage((BArray) result, promiseCombiner);
        } else {
            webSocketConnection.readNextFrame();
        }
    }

    private void sendPing(BArray result, PromiseCombiner promiseCombiner) {
        try {
            ChannelFuture webSocketChannelFuture = connectionInfo.getWebSocketConnection()
                    .ping(ByteBuffer.wrap(result.getBytes()));
            promiseCombiner.add(webSocketChannelFuture);
            promiseCombiner.finish(connectionInfo.getWebSocketConnection().getChannel().newPromise()
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            webSocketConnection.readNextFrame();
                        } else {
                            dispatchOnError(connectionInfo, future.cause(),
                                    connectionInfo.getWebSocketEndpoint().get(INITIALIZED_BY_SERVICE).equals(true));
                        }
                    }));
        } catch (Exception e) {
            log.error("Error occurred when pinging", e);
            dispatchOnError(connectionInfo, e,
                    connectionInfo.getWebSocketEndpoint().get(INITIALIZED_BY_SERVICE).equals(true));
        }
    }

    private void sendBinaryMessage(BArray result, PromiseCombiner promiseCombiner) {
        ByteBuf byteBuf = null;
        ByteBuf lastSlice = null;
        try {
            byteBuf = fromByteArray(ByteBuffer.wrap(result.getBytes()));
            int noBytes = byteBuf.readableBytes();
            int index = 0;
            final int size = (int) connectionInfo.getWebSocketEndpoint()
                    .getNativeData(WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE);
            while (index < noBytes - size) {
                ByteBuf slice = null;
                try {
                    slice = byteBuf.retainedSlice(index, size);
                    byte[] chunk = getByteChunk(size, slice);
                    ChannelFuture future = connectionInfo.getWebSocketConnection()
                            .pushBinary(ByteBuffer.wrap(chunk), false);
                    promiseCombiner.add(future);
                    index += size;
                } finally {
                    release(slice);
                }
            }
            lastSlice = byteBuf.retainedSlice(index, noBytes - index);
            byte[] finalChunk = getByteChunk(noBytes - index, lastSlice);
            ChannelFuture webSocketChannelFuture = connectionInfo.getWebSocketConnection()
                    .pushBinary(ByteBuffer.wrap(finalChunk), true);
            promiseCombiner.add(webSocketChannelFuture);
            promiseCombiner.finish(connectionInfo.getWebSocketConnection().getChannel().newPromise()
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY, connectionInfo);
                            webSocketConnection.readNextFrame();
                        } else {
                            dispatchOnError(connectionInfo, future.cause(), true);
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            log.error("Error occurred when pushing binary data", e);
            dispatchOnError(connectionInfo, e, true);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
    }

    private void sendTextMessage(BString result, PromiseCombiner promiseCombiner) {
        ByteBuf byteBuf = null;
        ByteBuf lastSlice = null;
        try {
            byteBuf = fromText(result.getValue());
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
                        if (channelFuture.isSuccess()) {
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, connectionInfo);
                            webSocketConnection.readNextFrame();
                        } else {
                            dispatchOnError(connectionInfo, future.cause(), true);
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            log.error("Error occurred when pushing text data", e);
            dispatchOnError(connectionInfo, e, true);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
    }

    @Override
    public void notifyFailure(BError error) {
        error.printStackTrace();
        WebSocketUtil.closeDuringUnexpectedCondition(webSocketConnection);
        observeError(connectionInfo, WebSocketObservabilityConstants.ERROR_TYPE_RESOURCE_INVOCATION, resource,
                error.getMessage());
    }
}

/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityConstants;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Utilities related to websocket connector actions.
 */
public class WebSocketConnector {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConnector.class);

    public static Object externWriteTextMessage(Environment env, BObject wsConnection, BString text) {
        Future balFuture = env.markAsync();
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil
                .observeResourceInvocation(env, connectionInfo, WebSocketConstants.WRITE_TEXT_MESSAGE);
        ByteBuf byteBuf = null;
        ByteBuf lastSlice = null;
        try {
            byteBuf = fromText(text.getValue());
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
                            WebSocketUtil.handleWebSocketCallback(balFuture, channelFuture, log, connectionInfo);
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, connectionInfo);
                        } else {
                            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, future.cause());
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            log.error("Error occurred when pushing text data", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT, e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
        return null;
    }

    private static void release(ByteBuf byteBuf) {
        if (byteBuf != null) {
            byteBuf.release();
        }
    }

    private static ByteBuf fromText(String text) {
        if (text == null || text.isEmpty()) {
            return Unpooled.EMPTY_BUFFER;
        } else {
            return Unpooled.copiedBuffer(text, CharsetUtil.UTF_8);
        }
    }

    private static ByteBuf fromByteArray(ByteBuffer buffer) {
        return Unpooled.wrappedBuffer(buffer);
    }

    public static Object writeBinaryMessage(Environment env, BObject wsConnection, BArray binaryData) {
        Future balFuture = env.markAsync();
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil
                .observeResourceInvocation(env, connectionInfo, WebSocketConstants.WRITE_BINARY_MESSAGE);
        ByteBuf byteBuf = null;
        ByteBuf lastSlice = null;
        try {
            byteBuf = fromByteArray(ByteBuffer.wrap(binaryData.getBytes()));
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
                            WebSocketUtil.handleWebSocketCallback(balFuture, future, log, connectionInfo);
                            WebSocketObservabilityUtil
                                    .observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY, connectionInfo);
                        } else {
                            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, future.cause());
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            log.error("Error occurred when pushing binary data", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY, e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
        return null;
    }

    private static byte[] getByteChunk(int size, ByteBuf slice) {
        byte[] chunk = new byte[size];
        slice.getBytes(0, chunk);
        return chunk;
    }

    public static Object ping(Environment env, BObject wsConnection, BArray binaryData) {
        Future balFuture = env.markAsync();
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil.observeResourceInvocation(env, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_PING);
        try {
            ChannelFuture future = connectionInfo.getWebSocketConnection().ping(ByteBuffer.wrap(binaryData.getBytes()));
            WebSocketUtil.handleWebSocketCallback(balFuture, future, log, connectionInfo);
            WebSocketObservabilityUtil.observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_PING,
                    connectionInfo);
        } catch (Exception e) {
            log.error("Error occurred when pinging", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_PING,
                    e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        }
        return null;
    }

    public static Object pong(Environment env, BObject wsConnection, BArray binaryData) {
        Future balFuture = env.markAsync();
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil.observeResourceInvocation(env, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_PONG);
        try {
            ChannelFuture future = connectionInfo.getWebSocketConnection().pong(ByteBuffer.wrap(binaryData.getBytes()));
            WebSocketUtil.handleWebSocketCallback(balFuture, future, log, connectionInfo);
            WebSocketObservabilityUtil.observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_PONG,
                    connectionInfo);
        } catch (Exception e) {
            log.error("Error occurred when ponging", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_PONG,
                    e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        }
        return null;
    }
}

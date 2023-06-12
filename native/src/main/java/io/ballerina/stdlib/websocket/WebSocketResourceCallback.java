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
package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BStream;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
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

import java.nio.ByteBuffer;

import static io.ballerina.stdlib.websocket.WebSocketConstants.STREAMING_NEXT_FUNCTION;
import static io.ballerina.stdlib.websocket.WebSocketResourceDispatcher.dispatchOnError;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.fromByteArray;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.fromText;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.getByteChunk;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.release;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil.observeError;

/**
 * Callback impl for web socket.
 */
public class WebSocketResourceCallback implements Callback {

    private final WebSocketConnection webSocketConnection;
    private final WebSocketConnectionInfo connectionInfo;
    private final String resource;
    private final Runtime runtime;
    private static final Logger log = LoggerFactory.getLogger(WebSocketResourceCallback.class);
    private final String dispatcherStreamId;
    private final String dispatcherStreamIdValue;

    WebSocketResourceCallback(WebSocketConnectionInfo webSocketConnectionInfo, String resource, Runtime runtime,
                              String dispatcherStreamId, String dispatcherStreamIdValue)
            throws IllegalAccessException {
        this.runtime = runtime;
        this.connectionInfo = webSocketConnectionInfo;
        this.webSocketConnection = connectionInfo.getWebSocketConnection();
        this.resource = resource;
        this.dispatcherStreamId = dispatcherStreamId;
        this.dispatcherStreamIdValue = dispatcherStreamIdValue;
    }

    @Override
    public void notifySuccess(Object result) {
        PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
        if (result instanceof BError) {
            ((BError) result).printStackTrace();
        } else if (result instanceof BArray && resource.equals(WebSocketConstants.RESOURCE_NAME_ON_PING)) {
            sendPong((BArray) result, promiseCombiner);
        } else if (result instanceof BString) {
            sendTextMessage((BString) result, promiseCombiner);
        } else if (result instanceof BArray) {
            sendBinaryMessage((BArray) result, promiseCombiner);
        } else if (result instanceof BStream) {
            BObject bObject = ((BStream) result).getIteratorObj();
            ReturnStreamUnitCallBack returnStreamUnitCallBack;
            if (dispatcherStreamId != null && dispatcherStreamIdValue != null) {
                returnStreamUnitCallBack = new ReturnStreamUnitCallBack(bObject, runtime,
                        connectionInfo, webSocketConnection, dispatcherStreamId, dispatcherStreamIdValue);
            } else {
                returnStreamUnitCallBack = new ReturnStreamUnitCallBack(bObject, runtime,
                        connectionInfo, webSocketConnection, null, null);
            }
            runtime.invokeMethodAsyncConcurrently(bObject, STREAMING_NEXT_FUNCTION, null,
                    null, returnStreamUnitCallBack, null, PredefinedTypes.TYPE_NULL);
        } else if (result == null) {
            webSocketConnection.readNextFrame();
        } else if (!resource.equals(WebSocketConstants.RESOURCE_NAME_ON_PONG) &&
                !resource.equals(WebSocketConstants.RESOURCE_NAME_ON_CLOSE) &&
                !resource.equals(WebSocketConstants.RESOURCE_NAME_ON_ERROR) &&
                !resource.equals(WebSocketConstants.RESOURCE_NAME_ON_IDLE_TIMEOUT)) {
            if (dispatcherStreamId != null && dispatcherStreamIdValue != null && result != null &&
                    result instanceof BMap) {
                BMap output = (BMap) result;
                boolean dispatchingStreamIdPresent = output
                            .containsKey(StringUtils.fromString(dispatcherStreamId));
                if (dispatchingStreamIdPresent) {
                        output.put(StringUtils.fromString(dispatcherStreamId),
                                StringUtils.fromString(dispatcherStreamIdValue));
                        sendTextMessage(StringUtils.fromString(output.toString()), promiseCombiner);
                } else {
                    dispatchOnError(connectionInfo, new Throwable("Response type must contain dispatcherStreamId"),
                            true);
                }

            } else {
                sendTextMessage(StringUtils.fromString(result.toString()), promiseCombiner);
            }
        } else {
            log.error("invalid return type");
        }
    }

    private void sendPong(BArray result, PromiseCombiner promiseCombiner) {
        try {
            ChannelFuture webSocketChannelFuture = connectionInfo.getWebSocketConnection()
                    .pong(ByteBuffer.wrap(result.getBytes()));
            promiseCombiner.add(webSocketChannelFuture);
            promiseCombiner.finish(connectionInfo.getWebSocketConnection().getChannel().newPromise()
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            webSocketConnection.readNextFrame();
                        } else {
                            dispatchOnError(connectionInfo, future.cause(), connectionInfo.getWebSocketEndpoint()
                                    .get(WebSocketConstants.INITIALIZED_BY_SERVICE).equals(true));
                        }
                    }));
        } catch (Exception e) {
            log.error("Error occurred when pinging", e);
            dispatchOnError(connectionInfo, e,
                    connectionInfo.getWebSocketEndpoint().get(WebSocketConstants.INITIALIZED_BY_SERVICE).equals(true));
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
        System.exit(1);
    }
}

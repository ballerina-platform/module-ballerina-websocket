/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
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

import static io.ballerina.stdlib.websocket.WebSocketConstants.STREAMING_NEXT_FUNCTION;
import static io.ballerina.stdlib.websocket.WebSocketResourceDispatcher.dispatchOnError;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.fromText;
import static io.ballerina.stdlib.websocket.actions.websocketconnector.WebSocketConnector.release;

/**
 * Call back class registered for returning streams.
 */
public class ReturnStreamUnitCallBack implements Callback {

    private final Runtime runtime;
    private final BObject bObject;
    private final WebSocketConnectionInfo connectionInfo;
    private final WebSocketConnection webSocketConnection;

    ReturnStreamUnitCallBack(BObject bObject, Runtime runtime, WebSocketConnectionInfo connectionInfo,
                             WebSocketConnection webSocketConnection) {
        this.bObject = bObject;
        this.runtime = runtime;
        this.connectionInfo = connectionInfo;
        this.webSocketConnection = webSocketConnection;
    }

    @Override
    public void notifySuccess(Object response) {
        if (response != null) {
            PromiseCombiner promiseCombiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);
            String content;
            if (response instanceof BError) {
                content = ((BError) response).getMessage();
                webSocketConnection.terminateConnection(1011,
                        String.format("streaming failed: %s", content));
            } else {
                content = ((BMap) response).get(StringUtils.fromString("value")).toString();
                sendTextMessageStream(StringUtils.fromString(content), promiseCombiner);
                runtime.invokeMethodAsyncConcurrently(bObject, STREAMING_NEXT_FUNCTION, null,
                        null, this, null, PredefinedTypes.TYPE_NULL);
            }
        } else {
            webSocketConnection.readNextFrame();
        }
    }

    @Override
    public void notifyFailure(BError bError) {
        bError.printStackTrace();
        WebSocketUtil.closeDuringUnexpectedCondition(webSocketConnection);
    }

    private void sendTextMessageStream(BString result, PromiseCombiner promiseCombiner) {
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
                        } else {
                            dispatchOnError(connectionInfo, future.cause(), true);
                        }
                    }));
        } catch (IllegalAccessException | IllegalStateException e) {
            dispatchOnError(connectionInfo, e, true);
        } finally {
            release(byteBuf);
            release(lastSlice);
        }
    }
}

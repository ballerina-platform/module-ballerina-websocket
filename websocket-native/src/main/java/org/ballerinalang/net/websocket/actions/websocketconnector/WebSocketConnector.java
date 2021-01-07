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
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.netty.channel.ChannelFuture;
import org.ballerinalang.net.transport.contract.websocket.WebSocketTextMessage;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityConstants;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

import static org.ballerinalang.net.websocket.WebsocketErrorType.READING_INBOUND_TEXT_FAILED;

/**
 * Utilities related to websocket connector actions.
 */
public class WebSocketConnector {
    private static final Logger log = LoggerFactory.getLogger(WebSocketConnector.class);

    public static Object externWriteString(Environment env, BObject wsConnection, BString text, boolean finalFrame) {
        Future balFuture = env.markAsync();
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil.observeResourceInvocation(env, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_PUSH_TEXT);
        try {
            ChannelFuture future = connectionInfo.getWebSocketConnection().pushText(text.getValue(), finalFrame);
            WebSocketUtil.handleWebSocketCallback(balFuture, future, log, connectionInfo);
            WebSocketObservabilityUtil.observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT,
                    connectionInfo);
        } catch (Exception e) {
            log.error("Error occurred when pushing text data", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT,
                    e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        }
        return null;
    }

    public static Object writeBytes(Environment env, BObject wsConnection, BArray binaryData,
            boolean finalFrame) {
        Future balFuture = env.markAsync();
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        WebSocketObservabilityUtil.observeResourceInvocation(env, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_PUSH_BINARY);
        try {
            ChannelFuture webSocketChannelFuture = connectionInfo.getWebSocketConnection().pushBinary(
                    ByteBuffer.wrap(binaryData.getBytes()), finalFrame);
            WebSocketUtil.handleWebSocketCallback(balFuture, webSocketChannelFuture, log, connectionInfo);
            WebSocketObservabilityUtil.observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY,
                    connectionInfo);
        } catch (Exception e) {
            log.error("Error occurred when pushing binary data", e);
            WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                    WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                    WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY,
                    e.getMessage());
            WebSocketUtil.setCallbackFunctionBehaviour(connectionInfo, balFuture, e);
        }
        return null;
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

    public static Object externReadString(Environment env, BObject wsConnection) {
        WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
        BlockingQueue<WebSocketTextMessage> messages = connectionInfo.getMsgQueue();
        try {
            String msg = messages.take().getText();
            return StringUtils.fromString(msg);
        } catch (InterruptedException e) {
            return WebSocketUtil.createWebsocketError(e.getMessage(), READING_INBOUND_TEXT_FAILED);
        }
    }
}

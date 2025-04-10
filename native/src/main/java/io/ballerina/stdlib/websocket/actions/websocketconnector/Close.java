/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.actions.websocketconnector;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.websocket.ModuleUtils;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import io.ballerina.stdlib.websocket.server.WebSocketServerService;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@code Get} is the GET action implementation of the HTTP Connector.
 */
public class Close {
    private static final Logger log = LoggerFactory.getLogger(Close.class);

    public static Object externClose(Environment env, BObject wsConnection, long statusCode, BString reason,
                                     Object bTimeoutInSecs) {
        return env.yieldAndRun(() -> {
            CompletableFuture<Object> balFuture = new CompletableFuture<>();
            WebSocketConnectionInfo connectionInfo = (WebSocketConnectionInfo) wsConnection
                    .getNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO);
            WebSocketObservabilityUtil.observeResourceInvocation(env, connectionInfo,
                    WebSocketConstants.RESOURCE_NAME_CLOSE);
            try {
                int timeoutInSecs = getConnectionClosureTimeout(bTimeoutInSecs, connectionInfo);
                CountDownLatch countDownLatch = new CountDownLatch(1);
                List<BError> errors = new ArrayList<>(1);
                ChannelFuture closeFuture = initiateConnectionClosure(errors, (int) statusCode, reason.getValue(),
                        connectionInfo, countDownLatch);
                connectionInfo.getWebSocketConnection().readNextFrame();
                waitForTimeout(errors, timeoutInSecs, countDownLatch, connectionInfo);
                closeFuture.channel().close().addListener(future -> {
                    WebSocketUtil.setListenerOpenField(connectionInfo);
                    if (errors.isEmpty()) {
                        balFuture.complete(null);
                    } else {
                        balFuture.complete(errors.getLast());
                    }
                });
                WebSocketObservabilityUtil.observeSend(WebSocketObservabilityConstants.MESSAGE_TYPE_CLOSE,
                        connectionInfo);
            } catch (Exception e) {
                log.error("Error occurred when closing the connection", e);
                WebSocketObservabilityUtil.observeError(WebSocketObservabilityUtil.getConnectionInfo(wsConnection),
                        WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_SENT,
                        WebSocketObservabilityConstants.MESSAGE_TYPE_CLOSE,
                        e.getMessage());
                balFuture.complete(WebSocketUtil.createErrorByType(e));
            }
            return ModuleUtils.getResult(balFuture);
        });
    }

    public static int getConnectionClosureTimeout(Object bTimeoutInSecs, WebSocketConnectionInfo connectionInfo) {
        try {
            int timeoutInSecs = 0;
            if (bTimeoutInSecs instanceof BDecimal) {
                timeoutInSecs = Integer.parseInt(bTimeoutInSecs.toString());
            } else if (connectionInfo.getService() instanceof WebSocketServerService webSocketServerService) {
                timeoutInSecs = webSocketServerService.getConnectionClosureTimeout();
            }
            return timeoutInSecs;
        } catch (Exception e) {
            throw new RuntimeException("Invalid timeout value: " + bTimeoutInSecs, e);
        }
    }

    public static ChannelFuture initiateConnectionClosure(List<BError> errors, int statusCode, String reason,
            WebSocketConnectionInfo connectionInfo, CountDownLatch latch) throws IllegalAccessException {
        WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
        ChannelFuture closeFuture;
        closeFuture = webSocketConnection.initiateConnectionClosure(statusCode, reason);
        return closeFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                addError(cause.getMessage(), errors);
                WebSocketObservabilityUtil
                        .observeError(connectionInfo, WebSocketObservabilityConstants.ERROR_TYPE_CLOSE,
                                cause.getMessage());
            }
            latch.countDown();
        });
    }

    public static void waitForTimeout(List<BError> errors, int timeoutInSecs,
            CountDownLatch latch, WebSocketConnectionInfo connectionInfo) {
        try {
            if (timeoutInSecs < 0) {
                latch.await();
            } else {
                boolean countDownReached = latch.await(timeoutInSecs, TimeUnit.SECONDS);
                if (!countDownReached) {
                    String errMsg = String.format(
                            "Could not receive a WebSocket close frame from remote endpoint within %d seconds",
                            timeoutInSecs);
                    addError(errMsg, errors);
                    WebSocketObservabilityUtil.observeError(connectionInfo,
                            WebSocketObservabilityConstants.ERROR_TYPE_CLOSE, errMsg);
                }
            }
        } catch (InterruptedException err) {
            String errMsg = "Connection interrupted while closing the connection";
            addError(errMsg, errors);
            WebSocketObservabilityUtil.observeError(connectionInfo,
                    WebSocketObservabilityConstants.ERROR_TYPE_CLOSE, errMsg);
            Thread.currentThread().interrupt();
        }
    }

    private static void addError(String errMsg, List<BError> errors) {
        errors.add(WebSocketUtil.getWebSocketError(
                errMsg, null, WebSocketConstants.ErrorCode.ConnectionClosureError.errorCode(), null));
    }

    private Close() {
    }
}

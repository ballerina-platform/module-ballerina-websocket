/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.net.websocket;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.ballerinalang.net.transport.contract.websocket.ClientHandshakeFuture;
import org.ballerinalang.net.transport.contract.websocket.WebSocketClientConnector;
import org.ballerinalang.net.transport.contract.websocket.WebSocketConnection;
import org.ballerinalang.net.websocket.client.listener.ClientHandshakeListener;
import org.ballerinalang.net.websocket.client.listener.ExtendedConnectorListener;
import org.ballerinalang.net.websocket.client.listener.ExtendedHandshakeListener;
import org.ballerinalang.net.websocket.client.listener.WebSocketHandshakeListener;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;
import org.ballerinalang.net.websocket.server.WebSocketConnectionManager;
import org.ballerinalang.net.websocket.server.WebSocketServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import static org.ballerinalang.net.websocket.WebSocketConstants.INITIALIZED_BY_SERVICE;
import static org.ballerinalang.net.websocket.WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE;
import static org.ballerinalang.net.websocket.WebSocketConstants.SYNC_CLIENT;
import static org.ballerinalang.net.websocket.WebSocketConstants.WEBSOCKET_ASYNC_CLIENT;

/**
 * Utility class for WebSocket.
 */
public class WebSocketUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketUtil.class);
    private static final BString CLIENT_ENDPOINT_CONFIG = StringUtils.fromString("config");
    private static final BString HANDSHAKE_TIME_OUT = StringUtils.fromString("handShakeTimeout");
    private static final String WEBSOCKET_FAILOVER_CLIENT_NAME = WebSocketConstants.PACKAGE_WEBSOCKET +
            WebSocketConstants.SEPARATOR + WebSocketConstants.FAILOVER_WEBSOCKET_CLIENT;
    public static final String ERROR_MESSAGE = "Error occurred: ";
    public static final String LOG_MESSAGE = "{} {}";

    public static BObject createAndPopulateWebSocketCaller(WebSocketConnection webSocketConnection,
            WebSocketServerService wsService,
            WebSocketConnectionManager connectionManager) {
        BObject webSocketCaller = ValueCreator
                .createObjectValue(ModuleUtils.getWebsocketModule(), WebSocketConstants.WEBSOCKET_CALLER,
                        StringUtils.fromString(""), null, null);
        BObject webSocketConnector = ValueCreator
                .createObjectValue(ModuleUtils.getWebsocketModule(), WebSocketConstants.WEBSOCKET_CONNECTOR);
        webSocketCaller.addNativeData(NATIVE_DATA_MAX_FRAME_SIZE, wsService.getMaxFrameSize());

        webSocketCaller.set(WebSocketConstants.LISTENER_CONNECTOR_FIELD, webSocketConnector);
        populateWebSocketEndpoint(webSocketConnection, webSocketCaller);
        webSocketCaller.set(INITIALIZED_BY_SERVICE, true);
        WebSocketConnectionInfo connectionInfo =
                new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketCaller);
        connectionManager.addConnection(webSocketConnection.getChannelId(), connectionInfo);
        webSocketConnector.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO,
                connectionInfo);
        //Observe new connection
        WebSocketObservabilityUtil.observeConnection(
                connectionManager.getConnectionInfo(webSocketConnection.getChannelId()));

        return webSocketCaller;
    }

    public static void populateWebSocketEndpoint(WebSocketConnection webSocketConnection, BObject webSocketClient) {
        webSocketClient.set(WebSocketConstants.LISTENER_ID_FIELD,
                StringUtils.fromString(webSocketConnection.getChannelId()));
        webSocketClient.set(WebSocketConstants.LISTENER_NEGOTIATED_SUBPROTOCOLS_FIELD,
                StringUtils.fromString(webSocketConnection.getNegotiatedSubProtocol()));
        webSocketClient.set(WebSocketConstants.LISTENER_IS_SECURE_FIELD, webSocketConnection.isSecure());
        webSocketClient.set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, webSocketConnection.isOpen());
    }

    public static void handleWebSocketCallback(Future balFuture,
            ChannelFuture webSocketChannelFuture, Logger log,
            WebSocketConnectionInfo connectionInfo) {
        webSocketChannelFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                log.error(ERROR_MESSAGE, cause);
                setCallbackFunctionBehaviour(connectionInfo, balFuture, cause);
            } else {
                // This is needed because since the same strand is used in all actions if an action is called before
                // this one it will cause this action to return the return value of the previous action.
                balFuture.complete(null);
            }
        });
    }

    public static void handlePingWebSocketCallback(Future balFuture,
            ChannelFuture webSocketChannelFuture, Logger log,
            WebSocketConnectionInfo connectionInfo) {
        webSocketChannelFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                log.error(ERROR_MESSAGE, cause);
                setCallbackFunctionBehaviour(connectionInfo, balFuture, cause);
            } else {
                balFuture.complete(null);
                if (connectionInfo.getWebSocketEndpoint().getType().getName().equals(SYNC_CLIENT)) {
                    connectionInfo.getWebSocketConnection().readNextFrame();
                }
            }
        });
    }

    public static void setCallbackFunctionBehaviour(WebSocketConnectionInfo connectionInfo, Future balFuture,
            Throwable error) {
        balFuture.complete(WebSocketUtil.createErrorByType(error));
    }

    public static void readFirstFrame(WebSocketConnection webSocketConnection, BObject wsConnector) {
        webSocketConnection.readNextFrame();
        wsConnector.set(WebSocketConstants.CONNECTOR_IS_READY_FIELD, true);
    }

    /**
     * Closes the connection with the unexpected failure status code.
     *
     * @param webSocketConnection - the webSocket connection to be closed.
     */
    public static void closeDuringUnexpectedCondition(WebSocketConnection webSocketConnection) {
        webSocketConnection.terminateConnection(1011, "Unexpected condition");
    }

    public static void setListenerOpenField(WebSocketConnectionInfo connectionInfo) throws IllegalAccessException {
        connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD,
                connectionInfo.getWebSocketConnection().isOpen());
    }

    public static int findMaxFrameSize(BMap<BString, Object> configs) {
        long size = configs.getIntValue(WebSocketConstants.ANNOTATION_ATTR_MAX_FRAME_SIZE);
        if (size <= 0) {
            return WebSocketConstants.DEFAULT_MAX_FRAME_SIZE;
        }
        try {
            return Math.toIntExact(size);
        } catch (ArithmeticException e) {
            logger.warn("The value set for maxFrameSize needs to be less than " + Integer.MAX_VALUE +
                    ". The maxFrameSize value is set to " + Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }

    }

    public static int findTimeoutInSeconds(BMap<BString, Object> config, BString key, int defaultValue) {
        try {
            int timeout = (int) ((BDecimal) config.get(key)).floatValue();
            if (timeout < 0) {
                return defaultValue;
            }
            return timeout;
        } catch (ArithmeticException e) {
            logger.warn("The value set for {} needs to be less than {} .The {} value is set to {} ", key,
                    Integer.MAX_VALUE, key, Integer.MAX_VALUE);
            return Integer.MAX_VALUE;
        }
    }

    public static String[] findNegotiableSubProtocols(BMap<BString, Object> configs) {
        return configs.getArrayValue(WebSocketConstants.ANNOTATION_ATTR_SUB_PROTOCOLS).getStringArray();
    }

    static String getErrorMessage(Throwable err) {
        if (err.getMessage() == null) {
            return "Unexpected error occurred";
        }
        return err.getMessage();
    }

    /**
     * Creates the appropriate ballerina errors using for the given throwable.
     *
     * @param throwable - the throwable to be represented in Ballerina.
     * @return the relevant WebSocketError with proper error code.
     */
    public static BError createErrorByType(Throwable throwable) {
        if (throwable instanceof WebSocketException) {
            return ((WebSocketException) throwable).getWsError();
        }
        String errorCode = WebSocketConstants.ErrorCode.Error.errorCode();
        BError cause = null;
        String message = getErrorMessage(throwable);
        if (throwable instanceof CorruptedWebSocketFrameException) {
            WebSocketCloseStatus status = ((CorruptedWebSocketFrameException) throwable).closeStatus();
            if (status == WebSocketCloseStatus.MESSAGE_TOO_BIG) {
                errorCode = WebSocketConstants.ErrorCode.PayloadTooLargeError.errorCode();
            } else {
                errorCode = WebSocketConstants.ErrorCode.ProtocolError.errorCode();
            }
        } else if (throwable instanceof SSLException) {
            cause = createErrorCause(throwable.getMessage(), WebSocketConstants.ErrorCode.SslError.errorCode(),
                    ModuleUtils.getWebsocketModule());
            message = "SSL/TLS Error";
        } else if (throwable instanceof IllegalStateException) {
            if (throwable.getMessage().contains("frame continuation")) {
                errorCode = WebSocketConstants.ErrorCode.InvalidContinuationFrameError.errorCode();
            } else if (throwable.getMessage().toLowerCase(Locale.ENGLISH).contains("close frame")) {
                errorCode = WebSocketConstants.ErrorCode.ConnectionClosureError.errorCode();
            }
        } else if (throwable instanceof IllegalAccessException &&
                throwable.getMessage().equals(WebSocketConstants.WEBSOCKET_CONNECTION_FAILURE)) {
            errorCode = WebSocketConstants.ErrorCode.ConnectionError.errorCode();
            if (throwable.getMessage() == null) {
                message = WebSocketConstants.WEBSOCKET_CONNECTION_FAILURE;
            }
        } else if (throwable instanceof TooLongFrameException) {
            errorCode = WebSocketConstants.ErrorCode.PayloadTooLargeError.errorCode();
        } else if (throwable instanceof CodecException) {
            errorCode = WebSocketConstants.ErrorCode.ProtocolError.errorCode();
        } else if (throwable instanceof WebSocketHandshakeException) {
            errorCode = WebSocketConstants.ErrorCode.InvalidHandshakeError.errorCode();
        } else if (throwable instanceof IOException) {
            errorCode = WebSocketConstants.ErrorCode.ConnectionError.errorCode();
            String errMessage = throwable.getMessage() != null ? throwable.getMessage() : "Connection Error";
            cause = createErrorCause(errMessage, WebSocketConstants.ErrorCode.Error.
                    errorCode(), ModuleUtils.getWebsocketModule());
            message = "IO Error";
        }
        return getWebSocketError(message, null, errorCode, cause);
    }

    private static BError createErrorCause(String message, String errorIdName, Module packageName) {
        return ErrorCreator.createError(packageName, errorIdName, StringUtils.fromString(message), null, null);
    }

    /**
     * Establishes connection with the endpoint.
     *
     * @param clientConnector -  the webSocket client connector
     * @param webSocketClient - the WebSocket client
     * @param wsService - the WebSocket service
     */
    public static void establishWebSocketConnection(WebSocketClientConnector clientConnector,
            BObject webSocketClient, WebSocketService wsService) {
        // Async client has to start reading the frames once connected. Hence if the client is Async
        // we set the readyOnConnect to true.
        boolean readyOnConnect = webSocketClient.getType().getName().equals(WEBSOCKET_ASYNC_CLIENT);
        ClientHandshakeFuture handshakeFuture = clientConnector.connect();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        setListenersToHandshakeFuture(handshakeFuture, webSocketClient, wsService, countDownLatch, readyOnConnect);
        // Sets the countDown latch for every handshake
        waitForHandshake(webSocketClient, countDownLatch, wsService);
    }

    /**
     * Sets listeners to the handshake future.
     *
     * @param handshakeFuture - the handshake future
     * @param webSocketClient - the WebSocket client
     * @param wsService - the WebSocket service
     */
    private static void setListenersToHandshakeFuture(ClientHandshakeFuture handshakeFuture,
            BObject webSocketClient, WebSocketService wsService, CountDownLatch countDownLatch,
            boolean readyOnConnect) {
        ExtendedConnectorListener connectorListener = (ExtendedConnectorListener) webSocketClient
                .getNativeData(WebSocketConstants.CLIENT_LISTENER);
        handshakeFuture.setWebSocketConnectorListener(connectorListener);
        ExtendedHandshakeListener webSocketHandshakeListener = new WebSocketHandshakeListener(webSocketClient,
                wsService, connectorListener, countDownLatch, readyOnConnect);
        handshakeFuture.setClientHandshakeListener(new ClientHandshakeListener(webSocketHandshakeListener));
    }

    private static void waitForHandshake(BObject webSocketClient, CountDownLatch countDownLatch,
            WebSocketService wsService) {
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        long timeout = WebSocketUtil.findTimeoutInSeconds((BMap<BString, Object>) webSocketClient.getMapValue(
                CLIENT_ENDPOINT_CONFIG), HANDSHAKE_TIME_OUT, 300);
        try {
            if (!countDownLatch.await(timeout, TimeUnit.SECONDS)) {
                countDownLatch.countDown();
                throw getWebSocketError("Waiting for WebSocket handshake has not been successful", null,
                        WebSocketConstants.ErrorCode.InvalidHandshakeError.errorCode(), WebSocketUtil
                                .createErrorCause("Connection timeout",
                                        WebSocketConstants.ErrorCode.HandshakeTimedOut.errorCode(),
                                        ModuleUtils.getWebsocketModule()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw WebSocketUtil.getWebSocketError(ERROR_MESSAGE + e.getMessage(), null,
                    WebSocketConstants.ErrorCode.Error.errorCode(), null);
        }
    }

    public static Map<String, String> getCustomHeaders(BMap<BString, Object> headers) {
        Map<String, String> customHeaders = new HashMap<>();
        headers.entrySet().forEach(
                entry -> customHeaders.put(entry.getKey().getValue(), headers.get(entry.getKey()).toString())
        );
        return customHeaders;
    }

    /**
     * Waits until call the countDown().
     *
     * @param countDownLatch - a countdown latch
     */
    public static void waitForHandshake(CountDownLatch countDownLatch) {
        try {
            // Waits to call countDown()
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw getWebSocketError(ERROR_MESSAGE + e.getMessage(), null,
                    WebSocketConstants.ErrorCode.Error.errorCode(), null);
        }
    }

    /**
     * Validate and create the webSocket service.
     *
     * @param callbackService - a client endpoint config
     * @param runtime - ballerina runtime
     * @return webSocketService
     */
    public static WebSocketService validateAndCreateWebSocketService(Runtime runtime, BObject callbackService) {
        if (callbackService != null) {
            Type param = (callbackService).getType().getMethods()[0].getParameterTypes()[0];
            if (param == null || !(WebSocketConstants.WEBSOCKET_CLIENT_NAME.equals(param.toString()) ||
                    WEBSOCKET_FAILOVER_CLIENT_NAME.equals(param.toString()))) {
                throw WebSocketUtil.getWebSocketError("The callback service should be a WebSocket Client Service",
                        null, WebSocketConstants.ErrorCode.Error.errorCode(), null);
            }
            return new WebSocketService(callbackService, runtime);
        } else {
            return new WebSocketService(runtime);
        }
    }

    /**
     * Counts the initialized `countDownLatch`.
     *
     * @param webSocketClient - the WebSocket client
     */
    public static void countDownForHandshake(BObject webSocketClient) {
        if (webSocketClient.getNativeData(WebSocketConstants.COUNT_DOWN_LATCH) != null) {
            ((CountDownLatch) webSocketClient.getNativeData(WebSocketConstants.COUNT_DOWN_LATCH)).countDown();
            webSocketClient.addNativeData(WebSocketConstants.COUNT_DOWN_LATCH, null);
        }
    }

    public static BError getWebSocketError(String msg, Throwable throwable, String errorCode,
            BError cause) {
        WebSocketException exception;
        String message = errorCode + ": " + msg;
        if (throwable != null) {
            exception = new WebSocketException(throwable, errorCode);
        } else if (cause != null) {
            exception = new WebSocketException(message, cause, errorCode);
        } else {
            exception = new WebSocketException(message, errorCode);
        }
        return exception.getWsError();
    }

    public static void setNotifyFailure(String msg, Future balFuture) {
        balFuture.complete(getWebSocketError(msg, null,
                WebSocketConstants.ErrorCode.InvalidHandshakeError.errorCode(), null));
    }

    public static BError createWebsocketError(String message, WebSocketConstants.ErrorCode errorType) {
        return ErrorCreator
                .createError(ModuleUtils.getWebsocketModule(), errorType.errorCode(), StringUtils.fromString(message),
                        null, null);
    }

    private WebSocketUtil() {
    }
}

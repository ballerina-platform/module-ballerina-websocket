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

package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.websocket.ClientHandshakeFuture;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketClientConnector;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.message.HttpCarbonMessage;
import io.ballerina.stdlib.websocket.client.RetryContext;
import io.ballerina.stdlib.websocket.client.listener.RetryWebSocketClientHandshakeListener;
import io.ballerina.stdlib.websocket.client.listener.RetryWriteBinaryHandshakeListener;
import io.ballerina.stdlib.websocket.client.listener.RetryWriteTextHandshakeListener;
import io.ballerina.stdlib.websocket.client.listener.SyncClientConnectorListener;
import io.ballerina.stdlib.websocket.client.listener.WebSocketHandshakeListener;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionManager;
import io.ballerina.stdlib.websocket.server.WebSocketServerService;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.websocketx.CorruptedWebSocketFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;

import static io.ballerina.stdlib.http.api.HttpErrorType.HEADER_NOT_FOUND_ERROR;
import static io.ballerina.stdlib.websocket.WebSocketConstants.CLIENT_ENDPOINT_CONFIG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.INITIALIZED_BY_SERVICE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.SYNC_CLIENT;

/**
 * Utility class for WebSocket.
 */
public class WebSocketUtil {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketUtil.class);
    private static final String WEBSOCKET_FAILOVER_CLIENT_NAME = WebSocketConstants.PACKAGE_WEBSOCKET +
            WebSocketConstants.SEPARATOR + WebSocketConstants.FAILOVER_WEBSOCKET_CLIENT;
    public static final String ERROR_MESSAGE = "Error occurred: ";

    public static BObject createAndPopulateWebSocketCaller(WebSocketConnection webSocketConnection,
            WebSocketServerService wsService,
            WebSocketConnectionManager connectionManager) {
        BObject webSocketCaller = ValueCreator
                .createObjectValue(ModuleUtils.getWebsocketModule(), WebSocketConstants.WEBSOCKET_CALLER,
                        StringUtils.fromString(""), null, null);
        webSocketCaller.addNativeData(NATIVE_DATA_MAX_FRAME_SIZE, wsService.getMaxFrameSize());

        populatWebSocketEndpoint(webSocketConnection, webSocketCaller);
        webSocketCaller.set(INITIALIZED_BY_SERVICE, true);
        WebSocketConnectionInfo connectionInfo =
                new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketCaller);
        connectionManager.addConnection(webSocketConnection.getChannelId(), connectionInfo);
        webSocketCaller.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO,
                connectionInfo);
        //Observe new connection
        WebSocketObservabilityUtil.observeConnection(
                connectionManager.getConnectionInfo(webSocketConnection.getChannelId()));

        return webSocketCaller;
    }

    public static void populatWebSocketEndpoint(WebSocketConnection webSocketConnection,
            BObject webSocketClient) {
        webSocketClient.addNativeData(WebSocketConstants.CONNECTION_ID_FIELD, webSocketConnection.getChannelId());
        webSocketClient.addNativeData(WebSocketConstants.NEGOTIATED_SUBPROTOCOL,
                webSocketConnection.getNegotiatedSubProtocol());
        webSocketClient.addNativeData(WebSocketConstants.IS_SECURE, webSocketConnection.isSecure());
        webSocketClient.set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, webSocketConnection.isOpen());
    }

    public static void handleWebSocketCallback(CompletableFuture<Object> balFuture,
                                               ChannelFuture webSocketChannelFuture, Logger log,
                                               WebSocketConnectionInfo connectionInfo, AtomicBoolean futureCompleted) {
        webSocketChannelFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                log.error(ERROR_MESSAGE, cause);
                setCallbackFunctionBehaviour(connectionInfo, balFuture, cause, futureCompleted);
            } else {
                // This is needed because since the same strand is used in all actions if an action is called before
                // this one it will cause this action to return the return value of the previous action.
                if (!futureCompleted.get()) {
                    balFuture.complete(null);
                    futureCompleted.set(true);
                }
            }
        });
    }

    public static void handlePingWebSocketCallback(CompletableFuture<Object> balFuture,
                        ChannelFuture webSocketChannelFuture, Logger log,
                        WebSocketConnectionInfo connectionInfo, AtomicBoolean pingCallbackCompleted) {
        webSocketChannelFuture.addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                log.error(ERROR_MESSAGE, cause);
                setCallbackFunctionBehaviour(connectionInfo, balFuture, cause, pingCallbackCompleted);
            } else {
                if (!pingCallbackCompleted.get()) {
                    balFuture.complete(null);
                    pingCallbackCompleted.set(true);
                }
                if (TypeUtils.getType(connectionInfo.getWebSocketEndpoint()).getName().equals(SYNC_CLIENT)) {
                    connectionInfo.getWebSocketConnection().readNextFrame();
                }
            }
        });
    }

    public static void setCallbackFunctionBehaviour(WebSocketConnectionInfo connectionInfo,
                                                    CompletableFuture<Object> balFuture,
            Throwable error, AtomicBoolean futureCompleted) {
        if (!futureCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(error));
            futureCompleted.set(true);
        }
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

    public static int findTimeoutInSeconds(BMap<BString, Object> config, BString key) {
        try {
            return (int) ((BDecimal) config.get(key)).floatValue();
        } catch (ArithmeticException e) {
            logger.warn("The value set for {} needs to be less than {} .The {} value is set to {} ", key,
                    Integer.MAX_VALUE, key, Integer.MAX_VALUE);
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

    public static BString getBString(byte[] byteArray) {
        return StringUtils.fromString(
                new String(byteArray, StandardCharsets.UTF_8));
    }

    public static boolean hasStringType(Type targetType) {
        if (targetType instanceof UnionType) {
            List<Type> memberTypes = ((UnionType) targetType).getMemberTypes();
            return memberTypes.stream().anyMatch(member -> member.getTag() ==
                    TypeTags.STRING_TAG | member.getTag() == TypeTags.FINITE_TYPE_TAG);
        }
        return false;
    }

    public static boolean hasByteArrayType(Type targetType) {
        List<Type> memberTypes = ((UnionType) targetType).getMemberTypes();
        return memberTypes.stream().anyMatch(member -> member.getTag() == TypeTags.ARRAY_TAG
                && member.toString().equals(WebSocketConstants.BYTE_ARRAY));
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
                errorCode = WebSocketConstants.ErrorCode.CorruptedFrameError.errorCode();
            }
        } else if (throwable instanceof SSLException) {
            cause = createErrorCause(throwable.getMessage(), WebSocketConstants.ErrorCode.SslError.errorCode(),
                    ModuleUtils.getWebsocketModule());
        } else if (throwable instanceof IllegalStateException) {
            if (throwable.getMessage().toLowerCase(Locale.ENGLISH).contains("close frame")) {
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
            errorCode = WebSocketConstants.ErrorCode.CorruptedFrameError.errorCode();
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

    public static Map<String, String> getCustomHeaders(BMap<BString, Object> headers) {
        Map<String, String> customHeaders = new HashMap<>();
        headers.entrySet().forEach(
                entry -> customHeaders.put(entry.getKey().getValue(), headers.get(entry.getKey()).toString())
        );
        return customHeaders;
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
            ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(callbackService));
            Type param = objectType.getMethods()[0].getParameters()[0].type;
            if (param == null || !(WebSocketConstants.WEBSOCKET_CLIENT_NAME.equals(param.toString()) ||
                    WEBSOCKET_FAILOVER_CLIENT_NAME.equals(param.toString()))) {
                throw WebSocketUtil.getWebSocketError("The callback service should be a PingPongService",
                        null, WebSocketConstants.ErrorCode.Error.errorCode(), null);
            }
            return new WebSocketService(callbackService, runtime);
        } else {
            return new WebSocketService(runtime);
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

    public static BError createWebsocketError(String message, WebSocketConstants.ErrorCode errorType) {
        return ErrorCreator
                .createError(ModuleUtils.getWebsocketModule(), errorType.errorCode(), StringUtils.fromString(message),
                        null, null);
    }

    public static BError createWebsocketErrorWithCause(String message, WebSocketConstants.ErrorCode errorType,
                                                       BError cause) {
        return ErrorCreator.createError(ModuleUtils.getWebsocketModule(), errorType.errorCode(),
                StringUtils.fromString(message), cause, null);
    }

    /**
     * Reconnect when the WebSocket connection is lost while reading or initial handshake.
     *
     * @param connectionInfo - Information about the connection.
     * @param balFuture - Ballerina future to be completed.
     * @param futureCompleted - Value to check whether the future has already completed.
     * @return If attempts reconnection, then return true.
     */
    public static boolean reconnect(WebSocketConnectionInfo connectionInfo, CompletableFuture<Object> balFuture,
                                    AtomicBoolean futureCompleted) {
        BObject webSocketClient = connectionInfo.getWebSocketEndpoint();
        RetryContext retryConnectorConfig = (RetryContext) webSocketClient.getNativeData(WebSocketConstants.
                RETRY_CONFIG.toString());
        int interval = retryConnectorConfig.getInterval();
        int maxInterval = retryConnectorConfig.getMaxInterval();
        int maxAttempts = retryConnectorConfig.getMaxAttempts();
        int noOfReconnectAttempts = retryConnectorConfig.getReconnectAttempts();
        double backOfFactor = retryConnectorConfig.getBackOfFactor();
        WebSocketService wsService = connectionInfo.getService();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        if (noOfReconnectAttempts < maxAttempts || maxAttempts == 0) {
            retryConnectorConfig.setReconnectAttempts(noOfReconnectAttempts + 1);
            String time = formatter.format(date.getTime());
            logger.debug(WebSocketConstants.LOG_MESSAGE, time, "reconnecting...");
            createDelay(calculateWaitingTime(interval, maxInterval, backOfFactor, noOfReconnectAttempts));
            establishWebSocketConnection(webSocketClient, wsService, balFuture, futureCompleted);
            return true;
        }
        logger.debug(WebSocketConstants.LOG_MESSAGE, "Maximum retry attempts but couldn't connect to the server: ",
                webSocketClient.getStringValue(WebSocketConstants.CLIENT_URL_CONFIG));
        return false;
    }

    /**
     * Reconnect when the WebSocket connection is lost while writing to the connection.
     *
     * @param connectionInfo - Information about the connection.
     * @param balFuture - Ballerina future to be completed.
     * @param futureCompleted - Value to check whether the future has already completed.
     * @param txtMessage - The text message that needs to be sent after a successful retry.
     * @param binMessage - The binary message that needs to be sent after a successful retry.
     * @return If attempts reconnection, then return true.
     */
    public static boolean reconnectForWrite(WebSocketConnectionInfo connectionInfo, CompletableFuture<Object> balFuture,
                                            AtomicBoolean futureCompleted, String txtMessage, BArray binMessage) {
        BObject webSocketClient = connectionInfo.getWebSocketEndpoint();
        RetryContext retryConnectorConfig = (RetryContext) webSocketClient.getNativeData(WebSocketConstants.
                RETRY_CONFIG.toString());
        int interval = retryConnectorConfig.getInterval();
        int maxInterval = retryConnectorConfig.getMaxInterval();
        int maxAttempts = retryConnectorConfig.getMaxAttempts();
        int noOfReconnectAttempts = retryConnectorConfig.getReconnectAttempts();
        double backOfFactor = retryConnectorConfig.getBackOfFactor();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        if (noOfReconnectAttempts < maxAttempts || maxAttempts == 0) {
            retryConnectorConfig.setReconnectAttempts(noOfReconnectAttempts + 1);
            String time = formatter.format(date.getTime());
            logger.debug(WebSocketConstants.LOG_MESSAGE, time, "reconnecting...");
            createDelay(calculateWaitingTime(interval, maxInterval, backOfFactor, noOfReconnectAttempts));
            establishWebSocketConnectionForWrite(webSocketClient, balFuture, futureCompleted, txtMessage, binMessage);
            return true;
        }
        logger.debug(WebSocketConstants.LOG_MESSAGE, "Maximum retry attempts but couldn't connect to the server: ",
                webSocketClient.getStringValue(WebSocketConstants.CLIENT_URL_CONFIG));
        return false;
    }

    /**
     * Establish connection with the endpoint. This is used for write operations.
     *
     * @param webSocketClient - The WebSocket client.
     * @param balFuture - Ballerina future to be completed.
     * @param futureCompleted - Value to check whether the future has already completed.
     * @param txtMessage - The text message that needs to be sent after a successful retry.
     * @param binMessage - The binary message that needs to be sent after a successful retry.
     */
    public static void establishWebSocketConnectionForWrite(BObject webSocketClient,
                                                            CompletableFuture<Object> balFuture,
                                                            AtomicBoolean futureCompleted, String txtMessage,
                                                            BArray binMessage) {
        SyncClientConnectorListener clientConnectorListener = (SyncClientConnectorListener) webSocketClient.
                getNativeData(WebSocketConstants.CLIENT_LISTENER);
        WebSocketClientConnector clientConnector = (WebSocketClientConnector) webSocketClient.
                getNativeData(WebSocketConstants.CLIENT_CONNECTOR);
        ClientHandshakeFuture handshakeFuture = clientConnector.connect();
        handshakeFuture.setWebSocketConnectorListener(clientConnectorListener);
        if (WebSocketUtil.hasRetryConfig(webSocketClient)) {
            if (txtMessage != null) {
                handshakeFuture.setClientHandshakeListener(new RetryWriteTextHandshakeListener(txtMessage,
                        webSocketClient, clientConnectorListener, balFuture, futureCompleted));
            } else {
                handshakeFuture.setClientHandshakeListener(new RetryWriteBinaryHandshakeListener(binMessage,
                        webSocketClient, clientConnectorListener, balFuture, futureCompleted));
            }
        }
    }

    /**
     * Establish connection with the endpoint. This is used for read and initial handshake.
     *  @param webSocketClient - The WebSocket client.
     * @param wsService - the WebSocket service.
     * @param balFuture - Ballerina future to be completed.
     * @param callbackCompleted - Value to check whether the future has already completed.
     */
    public static void establishWebSocketConnection(BObject webSocketClient, WebSocketService wsService,
                                                    CompletableFuture<Object> balFuture,
                                                    AtomicBoolean callbackCompleted) {
        SyncClientConnectorListener clientConnectorListener = (SyncClientConnectorListener) webSocketClient.
                getNativeData(WebSocketConstants.CLIENT_LISTENER);
        WebSocketClientConnector clientConnector = (WebSocketClientConnector) webSocketClient.
                getNativeData(WebSocketConstants.CLIENT_CONNECTOR);
        ClientHandshakeFuture handshakeFuture = clientConnector.connect();
        handshakeFuture.setWebSocketConnectorListener(clientConnectorListener);
        if (WebSocketUtil.hasRetryConfig(webSocketClient)) {
            handshakeFuture.setClientHandshakeListener(new RetryWebSocketClientHandshakeListener(webSocketClient,
                    wsService, clientConnectorListener, balFuture,
                    (RetryContext) webSocketClient.getNativeData(WebSocketConstants.RETRY_CONFIG.toString()),
                    callbackCompleted));
        } else {
            handshakeFuture.setClientHandshakeListener(new WebSocketHandshakeListener(webSocketClient, wsService,
                    clientConnectorListener, balFuture, callbackCompleted));
        }
    }

    public static boolean hasRetryConfig(BObject webSocketClient) {
        return webSocketClient.getMapValue(CLIENT_ENDPOINT_CONFIG).getMapValue(WebSocketConstants.RETRY_CONFIG) != null;
    }

    /**
     * Set the time to wait before attempting to reconnect.
     *
     * @param interval - Interval to wait before trying to reconnect.
     */
    private static void createDelay(int interval) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            if (!countDownLatch.await(interval, TimeUnit.SECONDS)) {
                countDownLatch.countDown();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebSocketException("WebSocketConstants.ERROR_MESSAGE", e.getMessage());
        }
    }

    /**
     * Calculate the waiting time.
     *
     * @param interval - Interval to wait before trying to reconnect.
     * @param maxInterval - Maximum interval to wait before trying to reconnect.
     * @param backOfFactor - The rate of increase of to reconnect delay.
     * @param reconnectAttempts - The number of reconnecting attempts.
     * @return The time to wait before attempting to reconnect.
     */
    private static int calculateWaitingTime(int interval, int maxInterval, double backOfFactor,
                                            int reconnectAttempts) {
        interval = (int) (interval * Math.pow(backOfFactor, reconnectAttempts));
        if (interval > maxInterval) {
            interval = maxInterval;
        }
        return interval;
    }

    public static void adjustContextOnSuccess(RetryContext retryConfig) {
        retryConfig.setFirstConnectionMadeSuccessfully();
        retryConfig.setReconnectAttempts(0);
    }

    public static Object getAuthorizationHeader(Environment env) {
        HttpCarbonMessage inboundMessage = (HttpCarbonMessage) env.getStrandLocal(HttpConstants.INBOUND_MESSAGE);
        String authorizationHeader = inboundMessage.getHeader(HttpHeaderNames.AUTHORIZATION.toString());
        if (authorizationHeader == null) {
            return HttpUtil.createHttpError("HTTP header does not exist", HEADER_NOT_FOUND_ERROR);
        }
        return StringUtils.fromString(authorizationHeader);
    }

    public static BString getConnectionId(Environment env, BObject wsSyncClient) {
        return StringUtils.fromString((String) wsSyncClient.getNativeData(WebSocketConstants.CONNECTION_ID_FIELD));
    }

    public static Boolean isSecure(Environment env, BObject wsSyncClient) {
        return (Boolean) wsSyncClient.getNativeData(WebSocketConstants.IS_SECURE);
    }

    public static Object getNegotiatedSubProtocol(Environment env, BObject wsSyncClient) {
        return StringUtils.fromString((String) wsSyncClient.getNativeData(WebSocketConstants.NEGOTIATED_SUBPROTOCOL));
    }

    private WebSocketUtil() {
    }
}

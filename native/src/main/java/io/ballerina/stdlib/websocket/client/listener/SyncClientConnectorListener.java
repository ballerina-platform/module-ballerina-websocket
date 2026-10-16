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

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.ValueUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.constraint.Constraints;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketBinaryMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketCloseMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnectorListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketControlMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketHandshaker;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketTextMessage;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketResourceDispatcher;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.ballerina.runtime.api.types.TypeTags.BYTE_TAG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.ANNOTATION_ATTR_VALIDATION_ENABLED;
import static io.ballerina.stdlib.websocket.WebSocketUtil.getBString;

/**
 * SyncClientConnectorListener implements {@link WebSocketConnectorListener} interface directly.
 *
 */
public class SyncClientConnectorListener implements WebSocketConnectorListener {

    private WebSocketConnectionInfo connectionInfo = null;
    private volatile ReadState readState = NO_READ;
    private volatile boolean closing = false;
    private static final Logger logger = LoggerFactory.getLogger(SyncClientConnectorListener.class);
    private static final ReadState NO_READ = new ReadState(null, null, new AtomicBoolean(true));

    private record ReadState(CompletableFuture<Object> callback, BTypedesc targetType, AtomicBoolean completed) {
    }

    public void setConnectionInfo(WebSocketConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void onHandshake(WebSocketHandshaker webSocketHandshaker) {}

    @Override
    public void onMessage(WebSocketTextMessage webSocketTextMessage) {
        ReadState read = this.readState;
        Type targetType = null;
        if (read.targetType() != null) {
            targetType = TypeUtils.getReferredType(read.targetType().getDescribingType());
        }
        try {
            WebSocketConnectionInfo.StringAggregator stringAggregator = connectionInfo
                    .createIfNullAndGetStringAggregator();
            boolean finalFragment = webSocketTextMessage.isFinalFragment();
            if (finalFragment) {
                stringAggregator.appendAggregateString(webSocketTextMessage.getText());
                Object message;
                int typeTag = targetType == null ? TypeTags.STRING_TAG : targetType.getTag();
                switch (typeTag) {
                    case TypeTags.STRING_TAG:
                        message = StringUtils.fromString(stringAggregator.getAggregateString());
                        break;
                    case TypeTags.XML_TAG:
                        message = XmlUtils.parse(stringAggregator.getAggregateString());
                        break;
                    case TypeTags.RECORD_TYPE_TAG:
                        message = cloneWithType(targetType, JsonUtils.parse(
                                stringAggregator.getAggregateString()));
                        break;
                    case TypeTags.UNION_TAG:
                        if (WebSocketUtil.hasStringType(targetType)) {
                            message = cloneWithType(targetType,
                                    StringUtils.fromString(stringAggregator.getAggregateString()));
                            break;
                        }
                        // fall through
                    default:
                        message = FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(
                                        stringAggregator.getAggregateString()),
                                ValueCreator.createTypedescValue(targetType));
                        break;
                }
                stringAggregator.resetAggregateString();
                completeRead(read, toReadResult(read, message));
                requestNextFrameIfClosing();
            } else {
                stringAggregator.appendAggregateString(webSocketTextMessage.getText());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException | BError e) {
            if (e instanceof BError) {
                completeRead(read, WebSocketUtil
                        .createWebsocketError(String.format("data binding failed: %s", e),
                                WebSocketConstants.ErrorCode.Error));
            } else {
                completeRead(read, WebSocketUtil
                        .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            }
            requestNextFrameIfClosing();
        }
    }

    private Object toReadResult(ReadState read, Object message) {
        if (message instanceof BError) {
            return WebSocketUtil.createWebsocketError(String.format("data binding failed: %s", message),
                    WebSocketConstants.ErrorCode.Error);
        }
        BTypedesc readTargetType = read.targetType();
        if (readTargetType == null) {
            return message;
        }
        boolean validationEnabled = connectionInfo.getWebSocketEndpoint()
                .getMapValue(WebSocketConstants.CLIENT_ENDPOINT_CONFIG)
                .getBooleanValue(ANNOTATION_ATTR_VALIDATION_ENABLED);
        if (!validationEnabled) {
            return message;
        }
        Object validationResult = Constraints.validate(message, readTargetType);
        if (validationResult instanceof BError) {
            return WebSocketUtil.createWebsocketErrorWithCause(
                    String.format("data validation failed: %s", validationResult),
                    WebSocketConstants.ErrorCode.PayloadValidationError, (BError) validationResult);
        }
        return message;
    }

    // Claims the read, cleans up its per-read state, and only then completes the future: completing wakes the
    // reader, whose next readMessage() replaces the callback and re-adds the idle handler.
    private boolean completeRead(ReadState read, Object result) {
        if (read.callback() == null || !read.completed().compareAndSet(false, true)) {
            return false;
        }
        try {
            connectionInfo.getCallbacks().remove(read.callback());
            connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        } finally {
            read.callback().complete(result);
        }
        return true;
    }

    // close() requests a single frame for its echo, so keep draining past buffered data frames while closing.
    private void requestNextFrameIfClosing() {
        if (!closing) {
            return;
        }
        try {
            connectionInfo.getWebSocketConnection().readNextFrame();
        } catch (IllegalAccessException e) {
            // The connection is already gone; there is nothing left to drain.
        }
    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {
        ReadState read = this.readState;
        Type targetType = null;
        if (read.targetType() != null) {
            targetType = TypeUtils.getReferredType(read.targetType().getDescribingType());
        }
        try {
            WebSocketConnectionInfo.ByteArrAggregator byteArrAggregator = connectionInfo
                    .createIfNullAndGetByteArrAggregator();
            boolean finalFragment = webSocketBinaryMessage.isFinalFragment();
            if (finalFragment) {
                byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
                byte[] binMsg = byteArrAggregator.getAggregateByteArr();
                byteArrAggregator.resetAggregateByteArr();
                Object message;
                int typeTag = targetType == null || targetType.toString().equals(WebSocketConstants.BYTE_ARRAY) ?
                        BYTE_TAG : targetType.getTag();
                switch (typeTag) {
                    case BYTE_TAG:
                        message = ValueCreator.createArrayValue(binMsg);
                        break;
                    case TypeTags.STRING_TAG:
                        message = getBString(binMsg);
                        break;
                    case TypeTags.XML_TAG:
                        message = XmlUtils.parse(getBString(binMsg));;
                        break;
                    case TypeTags.RECORD_TYPE_TAG:
                        message = cloneWithType(targetType, JsonUtils.parse(getBString(binMsg)));
                        break;
                    case TypeTags.UNION_TAG:
                        if (WebSocketUtil.hasByteArrayType(targetType)) {
                            message = cloneWithType(targetType, ValueCreator.createArrayValue(binMsg));
                            break;
                        }
                        // fall through
                    default:
                        message = FromJsonStringWithType.fromJsonStringWithType(getBString(binMsg),
                                ValueCreator.createTypedescValue(targetType));
                        break;
                }
                completeRead(read, toReadResult(read, message));
                requestNextFrameIfClosing();
            } else {
                byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException | IOException | BError e) {
            if (e instanceof BError) {
                completeRead(read, WebSocketUtil
                        .createWebsocketError(String.format("data binding failed: %s", e),
                                WebSocketConstants.ErrorCode.Error));
            } else {
                completeRead(read, WebSocketUtil
                        .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            }
            requestNextFrameIfClosing();
        }
    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {
        WebSocketResourceDispatcher.dispatchOnPingOnPong(connectionInfo, webSocketControlMessage, false);
    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {
        ReadState read = this.readState;
        if (read.callback() == null || read.completed().get()) {
            return;
        }
        int closeCode = webSocketCloseMessage.getCloseCode();
        String closeReason = webSocketCloseMessage.getCloseReason() == null ||
                webSocketCloseMessage.getCloseReason().equals("") ?
                String.format("%s %s %d", WebSocketConstants.CONNECTION_CLOSED,
                        WebSocketConstants.STATUS_CODE, closeCode) :
                String.format("%s: %s %d", webSocketCloseMessage.getCloseReason(),
                        WebSocketConstants.STATUS_CODE, closeCode);
        if (WebSocketUtil.hasRetryConfig(connectionInfo.getWebSocketEndpoint())) {
            if (closeCode == WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE &&
                    WebSocketUtil.reconnect(connectionInfo, read.callback(), read.completed())) {
                return;
            } else {
                if (closeCode != WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE) {
                    logger.debug(WebSocketConstants.LOG_MESSAGE, "Reconnect attempt not made because of " +
                            "close initiated by the server: ", connectionInfo.getWebSocketEndpoint()
                            .getStringValue(WebSocketConstants.CLIENT_URL_CONFIG));
                }
            }
        }
        completeRead(read, WebSocketUtil.createWebsocketError(closeReason,
                WebSocketConstants.ErrorCode.ConnectionClosureError));
        try {
            WebSocketResourceDispatcher.finishConnectionClosureIfOpen(connectionInfo.getWebSocketConnection(),
                    closeCode, connectionInfo);
            if (connectionInfo.getCallbacks().size() > 0) {
                connectionInfo.getCallbacks().forEach(callback -> {
                    callback.complete(WebSocketUtil.createWebsocketError(WebSocketConstants.CONNECTION_CLOSED,
                            WebSocketConstants.ErrorCode.ConnectionClosureError));
                });
            }
        } catch (IllegalAccessException e) {
            // The connection is already gone and the read has been completed above.
        }
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {
        ReadState read = this.readState;
        if (read.callback() == null || read.completed().get()) {
            return;
        }
        BObject webSocketClient = connectionInfo.getWebSocketEndpoint();
        if (WebSocketUtil.hasRetryConfig(webSocketClient) && throwable instanceof IOException &&
                WebSocketUtil.reconnect(connectionInfo, read.callback(), read.completed())) {
            return;
        }
        completeRead(read, WebSocketUtil.createWebsocketError(throwable.getMessage(),
                WebSocketConstants.ErrorCode.Error));
    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage controlMessage) {
        completeRead(this.readState, WebSocketUtil
                .createWebsocketError("Read timed out", WebSocketConstants.ErrorCode.ReadTimedOutError));
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection) {
        WebSocketObservabilityUtil.observeClose(connectionInfo);
        if (connectionInfo.getCallbacks().size() > 0) {
            connectionInfo.getCallbacks().forEach(callback -> {
                callback.complete(WebSocketUtil.createWebsocketError(WebSocketConstants.CONNECTION_CLOSED,
                                WebSocketConstants.ErrorCode.ConnectionClosureError));
            });
        }
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
        } catch (IllegalAccessException e) {
            // Ignore as at this point connection closure error is returned to the user
        }
    }

    public void startRead(CompletableFuture<Object> callback, BTypedesc targetType) {
        this.readState = new ReadState(callback, targetType, new AtomicBoolean(false));
    }

    public void markClosing() {
        this.closing = true;
    }

    private Object cloneWithType(Type targetType, Object value) {
        try {
            return ValueUtils.convert(value, targetType);
        } catch (BError e) {
            return e;
        }
    }
}

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
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
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
import org.ballerinalang.langlib.value.CloneWithType;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.ballerina.runtime.api.TypeTags.BYTE_TAG;
import static io.ballerina.stdlib.websocket.WebSocketUtil.getBString;

/**
 * SyncClientConnectorListener implements {@link WebSocketConnectorListener} interface directly.
 *
 */
public class SyncClientConnectorListener implements WebSocketConnectorListener {

    private WebSocketConnectionInfo connectionInfo = null;
    private Future callback;
    private Type targetType;
    private AtomicBoolean futureCompleted;
    private static final Logger logger = LoggerFactory.getLogger(SyncClientConnectorListener.class);

    public void setConnectionInfo(WebSocketConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public void onHandshake(WebSocketHandshaker webSocketHandshaker) {}

    @Override
    public void onMessage(WebSocketTextMessage webSocketTextMessage) {
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
                        message = CloneWithType.convert(targetType, JsonUtils.parse(
                                stringAggregator.getAggregateString()));
                        break;
                    case TypeTags.UNION_TAG:
                        if (WebSocketUtil.hasStringType(targetType)) {
                            message = CloneWithType.convert(targetType,
                                    StringUtils.fromString(stringAggregator.getAggregateString()));
                        } else {
                            message = FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(
                                            stringAggregator.getAggregateString()),
                                    ValueCreator.createTypedescValue(targetType));
                        }
                        break;
                    default:
                        message = FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(
                                        stringAggregator.getAggregateString()),
                                ValueCreator.createTypedescValue(targetType));
                        break;
                }
                stringAggregator.resetAggregateString();
                if (!futureCompleted.get()) {
                    if (message instanceof BError) {
                        callback.complete(WebSocketUtil
                                .createWebsocketError(String.format("data binding failed: %s", message),
                                        WebSocketConstants.ErrorCode.Error));
                    } else {
                        callback.complete(message);
                    }
                    futureCompleted.set(true);
                }
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            } else {
                stringAggregator.appendAggregateString(webSocketTextMessage.getText());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException | BError e) {
            if (e instanceof BError) {
                callback.complete(WebSocketUtil
                        .createWebsocketError(String.format("data binding failed: %s", e),
                                WebSocketConstants.ErrorCode.Error));
            } else {
                callback.complete(WebSocketUtil
                        .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            }
            futureCompleted.set(true);
        }
    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {
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
                        TypeTags.BYTE_TAG : targetType.getTag();
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
                        message = CloneWithType.convert(targetType, JsonUtils.parse(getBString(binMsg)));
                        break;
                    case TypeTags.UNION_TAG:
                        if (WebSocketUtil.hasByteArrayType(targetType)) {
                            message = CloneWithType.convert(targetType, ValueCreator.createArrayValue(binMsg));
                        } else {
                            message = FromJsonStringWithType.fromJsonStringWithType(getBString(binMsg),
                                    ValueCreator.createTypedescValue(targetType));
                        }
                        break;
                    default:
                        message = FromJsonStringWithType.fromJsonStringWithType(getBString(binMsg),
                                ValueCreator.createTypedescValue(targetType));
                        break;
                }
                callback.complete(message);
                futureCompleted.set(true);
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            } else {
                byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        } catch (IllegalAccessException | IOException e) {
            callback.complete(WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.ConnectionClosureError));
            futureCompleted.set(true);
        }
    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {
        WebSocketResourceDispatcher.dispatchOnPingOnPong(connectionInfo, webSocketControlMessage, false);
    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {
        try {
            if (callback != null && !futureCompleted.get()) {
            int closeCode = webSocketCloseMessage.getCloseCode();
            String closeReason = webSocketCloseMessage.getCloseReason() == null ||
                    webSocketCloseMessage.getCloseReason().equals("") ?
                    "Connection closed: Status code: " + closeCode :
                    webSocketCloseMessage.getCloseReason() + ": Status code: " + closeCode;
            if (WebSocketUtil.hasRetryConfig(connectionInfo.getWebSocketEndpoint())) {
                if (closeCode == WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE &&
                        WebSocketUtil.reconnect(connectionInfo, callback)) {
                    return;
                } else {
                    if (closeCode != WebSocketConstants.STATUS_CODE_ABNORMAL_CLOSURE) {
                        logger.debug(WebSocketConstants.LOG_MESSAGE, "Reconnect attempt not made because of " +
                                "close initiated by the server: ", connectionInfo.getWebSocketEndpoint()
                                .getStringValue(WebSocketConstants.CLIENT_URL_CONFIG));
                    }
                }
            }
            if (!futureCompleted.get()) {
                callback.complete(WebSocketUtil
                        .createWebsocketError(closeReason, WebSocketConstants.ErrorCode.ConnectionClosureError));
                futureCompleted.set(true);
            }
            WebSocketConnection wsConnection = connectionInfo.getWebSocketConnection();
            wsConnection.removeReadIdleStateHandler();
            WebSocketResourceDispatcher.finishConnectionClosureIfOpen(wsConnection, closeCode, connectionInfo);
            }
        } catch (IllegalAccessException e) {
            callback.complete(WebSocketUtil.createWebsocketError("Connection already closed",
                    WebSocketConstants.ErrorCode.ConnectionClosureError));
        }
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {
        try {
            if (callback != null && !futureCompleted.get()) {
                BObject webSocketClient = connectionInfo.getWebSocketEndpoint();
                if (WebSocketUtil.hasRetryConfig(webSocketClient) && throwable instanceof IOException &&
                        WebSocketUtil.reconnect(connectionInfo, callback)) {
                    return;
                }
                callback.complete(WebSocketUtil
                        .createWebsocketError(throwable.getMessage(), WebSocketConstants.ErrorCode.Error));
                futureCompleted.set(true);
                connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
            }
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        }
    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage controlMessage) {
        try {
            callback.complete(WebSocketUtil
                    .createWebsocketError("Read timed out", WebSocketConstants.ErrorCode.ReadTimedOutError));
            connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
        } catch (IllegalAccessException e) {
            // Ignore as it is not possible have an Illegal access
        }
    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection) {
        WebSocketObservabilityUtil.observeClose(connectionInfo);
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
        } catch (IllegalAccessException e) {
            // Ignore as at this point connection closure error is returned to the user
        }
    }

    public void setCallback(Future callback) {
        this.callback = callback;
    }

    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    public void setFutureCompleted(AtomicBoolean futureCompleted) {
        this.futureCompleted = futureCompleted;
    }
}

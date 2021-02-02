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
package org.ballerinalang.net.websocket.client.listener;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.net.transport.contract.websocket.WebSocketBinaryMessage;
import org.ballerinalang.net.transport.contract.websocket.WebSocketCloseMessage;
import org.ballerinalang.net.transport.contract.websocket.WebSocketConnection;
import org.ballerinalang.net.transport.contract.websocket.WebSocketControlMessage;
import org.ballerinalang.net.transport.contract.websocket.WebSocketHandshaker;
import org.ballerinalang.net.transport.contract.websocket.WebSocketTextMessage;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketResourceDispatcher;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.observability.WebSocketObservabilityUtil;
import org.ballerinalang.net.websocket.server.WebSocketConnectionInfo;

import java.io.IOException;

/**
 * SyncClientConnectorListener implements {@link ExtendedConnectorListener} interface directly.
 *
 */
public class SyncClientConnectorListener implements ExtendedConnectorListener {

    private WebSocketConnectionInfo connectionInfo = null;
    private Future callback;

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
            BString txtMsg = getAggregatedTextMessage(webSocketTextMessage, stringAggregator, finalFragment);
            callback.complete(txtMsg);
            connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
        } catch (IllegalAccessException e) {
            callback.complete(WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.WsConnectionClosureError));
        }
    }

    private BString getAggregatedTextMessage(WebSocketTextMessage webSocketTextMessage,
            WebSocketConnectionInfo.StringAggregator stringAggregator, boolean finalFragment)
            throws IllegalAccessException {
        BString txtMsg;
        while (true) {
            stringAggregator.appendAggregateString(webSocketTextMessage.getText());
            if (finalFragment) {
                txtMsg = StringUtils.fromString(stringAggregator.getAggregateString());
                stringAggregator.resetAggregateString();
                break;
            } else {
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        }
        return txtMsg;
    }

    @Override
    public void onMessage(WebSocketBinaryMessage webSocketBinaryMessage) {
        try {
            WebSocketConnectionInfo.ByteArrAggregator byteArrAggregator = connectionInfo
                    .createIfNullAndGetByteArrAggregator();
            byte[] binMsg = getAggregatedBinMessage(webSocketBinaryMessage, byteArrAggregator);
            callback.complete(ValueCreator.createArrayValue(binMsg));
            connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
        } catch (IllegalAccessException | IOException e) {
            callback.complete(WebSocketUtil
                    .createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.WsConnectionClosureError));
        }
    }

    private byte[] getAggregatedBinMessage(WebSocketBinaryMessage webSocketBinaryMessage,
            WebSocketConnectionInfo.ByteArrAggregator byteArrAggregator) throws IOException, IllegalAccessException {
        byte[] binMsg;
        while (true) {
            boolean finalFragment = webSocketBinaryMessage.isFinalFragment();
            byteArrAggregator.appendAggregateArr(webSocketBinaryMessage.getByteArray());
            if (finalFragment) {
                binMsg = byteArrAggregator.getAggregateByteArr();
                byteArrAggregator.resetAggregateByteArr();
                break;
            } else {
                connectionInfo.getWebSocketConnection().readNextFrame();
            }
        }
        return binMsg;
    }

    @Override
    public void onMessage(WebSocketControlMessage webSocketControlMessage) {
        WebSocketResourceDispatcher.dispatchOnPingOnPong(connectionInfo, webSocketControlMessage, false);
    }

    @Override
    public void onMessage(WebSocketCloseMessage webSocketCloseMessage) {
        try {
            int closeCode = webSocketCloseMessage.getCloseCode();
            String closeReason = webSocketCloseMessage.getCloseReason().equals("") ?
                    "Connection closed: Status code: " + closeCode :
                    webSocketCloseMessage.getCloseReason() + ": Status code: " + closeCode;
            callback.complete(WebSocketUtil
                    .createWebsocketError(closeReason, WebSocketConstants.ErrorCode.WsConnectionClosureError));

            WebSocketConnection wsConnection = connectionInfo.getWebSocketConnection();
            wsConnection.removeReadIdleStateHandler();
            WebSocketResourceDispatcher.finishConnectionClosureIfOpen(wsConnection, closeCode, connectionInfo);
        } catch (IllegalAccessException e) {
            callback.complete(WebSocketUtil.createWebsocketError("Connection already closed",
                    WebSocketConstants.ErrorCode.WsConnectionClosureError));
        }
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Throwable throwable) {
        try {
            callback.complete(WebSocketUtil
                    .createWebsocketError(throwable.getMessage(), WebSocketConstants.ErrorCode.WsGenericError));
            connectionInfo.getWebSocketConnection().removeReadIdleStateHandler();
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        }
    }

    @Override
    public void onIdleTimeout(WebSocketControlMessage controlMessage) {
        callback.complete(
                WebSocketUtil.createWebsocketError("Read timed out", WebSocketConstants.ErrorCode.ReadTimedOutError));
        try {
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
            // Ignore as it is not possible have an Illegal access
        }
    }

    public void setCallback(Future callback) {
        this.callback = callback;
    }
}

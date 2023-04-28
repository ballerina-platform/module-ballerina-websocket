/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.server;

import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.transport.contract.websocket.ServerHandshakeFuture;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketHandshaker;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

/**
 * The onUpgrade resource callback.
 */
public class OnUpgradeResourceCallback implements Callback {
    private final WebSocketHandshaker webSocketHandshaker;
    private final WebSocketServerService wsService;
    private final WebSocketConnectionManager connectionManager;

    public OnUpgradeResourceCallback(WebSocketHandshaker webSocketHandshaker, WebSocketServerService wsService,
            WebSocketConnectionManager connectionManager) {
        this.webSocketHandshaker = webSocketHandshaker;
        this.wsService = wsService;
        this.connectionManager = connectionManager;
    }

    @Override
    public void notifySuccess(Object result) {
        if (result instanceof BError) {
            BError error = (BError) result;
            if (error.getType().getName().equals(WebSocketConstants.ErrorCode.AuthnError.errorCode())) {
                webSocketHandshaker.cancelHandshake(401, error.getErrorMessage().toString());
                return;
            }
            if (error.getType().getName().equals(WebSocketConstants.ErrorCode.AuthzError.errorCode())) {
                webSocketHandshaker.cancelHandshake(403, error.getErrorMessage().toString());
                return;
            }
            webSocketHandshaker.cancelHandshake(400, error.getErrorMessage().toString());
            return;
        }
        if (!webSocketHandshaker.isCancelled() && !webSocketHandshaker.isHandshakeStarted()) {
            HttpHeaders headers = null;
            ObjectType type = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(result));
            if (type.getFields().get(WebSocketConstants.CUSTOM_HEADERS.toString()) != null) {
                BMap<BString, BString> headersMap = (BMap) ((BObject) result).get(WebSocketConstants.CUSTOM_HEADERS);
                headers = populateAndGetHttpHeaders(headersMap);
            }
            ServerHandshakeFuture future = webSocketHandshaker
                    .handshake(wsService.getNegotiableSubProtocols(), wsService.getIdleTimeoutInSeconds() * 1000,
                            headers, wsService.getMaxFrameSize());
            future.setHandshakeListener(new UpgradeListener(wsService, connectionManager, result));
        }
    }

    @Override
    public void notifyFailure(BError error) {
        // These checks are added to release the failure path since there is an authn/authz failure and responded
        // with 401/403 internally.
        if (error.getType().getName().equals(WebSocketConstants.ErrorCode.AuthnError.errorCode())) {
            webSocketHandshaker.cancelHandshake(401, null);
            return;
        }
        if (error.getType().getName().equals(WebSocketConstants.ErrorCode.AuthzError.errorCode())) {
            webSocketHandshaker.cancelHandshake(403, null);
            return;
        }
        // When panicked from the upgrade service.
        error.printStackTrace();
        webSocketHandshaker.cancelHandshake(500, error.getMessage());
    }

    private static DefaultHttpHeaders populateAndGetHttpHeaders(BMap<BString, BString> headers) {
        DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();
        BString[] keys = headers.getKeys();
        for (BString key : keys) {
            httpHeaders.add(key.toString(), headers.get(key).getValue());
        }
        return httpHeaders;
    }
}

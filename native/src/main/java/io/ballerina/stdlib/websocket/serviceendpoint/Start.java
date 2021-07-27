/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.serviceendpoint;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.http.transport.contract.ServerConnector;
import io.ballerina.stdlib.http.transport.contract.ServerConnectorFuture;
import io.ballerina.stdlib.websocket.WebSocketConnectorPortBindingListener;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.server.WebSocketServerListener;

import static io.ballerina.stdlib.websocket.WebSocketConstants.HTTP_LISTENER;

/**
 * Start the Websocket listener instance.
 *
 */
public class Start extends AbstractWebsocketNativeFunction {
    public static Object start(BObject listener) {
        BObject httpListener = (BObject) listener.get(StringUtils.fromString(HTTP_LISTENER));
        if (!isConnectorStarted(listener) && !isConnectorStarted(httpListener)) {
            return startServerConnector(listener);
        } else if (httpListener != null) {
            ServerConnectorFuture serverConnectorFuture = (ServerConnectorFuture) ((BObject) listener
                    .get(StringUtils.fromString(HTTP_LISTENER))).getNativeData(HttpConstants.SERVER_CONNECTOR_FUTURE);
            WebSocketServerListener wsListener = new WebSocketServerListener(getWebSocketServicesRegistry(listener));
            serverConnectorFuture.setWebSocketConnectorListener(wsListener);
        }
        return null;
    }

    private static Object startServerConnector(BObject serviceEndpoint) {
        ServerConnector serverConnector = getServerConnector(serviceEndpoint);
        ServerConnectorFuture serverConnectorFuture = serverConnector.start();
        WebSocketServerListener wsListener = new WebSocketServerListener(getWebSocketServicesRegistry(serviceEndpoint));
        WebSocketConnectorPortBindingListener portBindingListener = new WebSocketConnectorPortBindingListener();
        serverConnectorFuture.setWebSocketConnectorListener(wsListener);
        serverConnectorFuture.setPortBindingEventListener(portBindingListener);

        try {
            serverConnectorFuture.sync();
        } catch (Exception ex) {
            throw WebSocketUtil.createWebsocketError(
                    "failed to start server connector '" + serverConnector.getConnectorID() + "': " + ex.getMessage(),
                    WebSocketConstants.ErrorCode.Error);
        }

        serviceEndpoint.addNativeData(HttpConstants.CONNECTOR_STARTED, true);
        return null;
    }

    private Start() {}
}

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

package org.ballerinalang.net.websocket.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.transport.contract.HttpWsConnectorFactory;
import org.ballerinalang.net.transport.contract.websocket.ClientHandshakeFuture;
import org.ballerinalang.net.transport.contract.websocket.WebSocketClientConnector;
import org.ballerinalang.net.transport.contract.websocket.WebSocketClientConnectorConfig;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketService;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.client.listener.SyncClientConnectorListener;
import org.ballerinalang.net.websocket.client.listener.WebSocketHandshakeListener;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.net.websocket.WebSocketConstants.CLIENT_CONNECTION_ERROR;
import static org.ballerinalang.net.websocket.WebSocketConstants.SYNC_CLIENT_SERVICE_CONFIG;
import static org.ballerinalang.net.websocket.WebSocketUtil.createErrorByType;
import static org.ballerinalang.net.websocket.WebSocketUtil.findMaxFrameSize;

/**
 * Initialize the WebSocket Synchronous Client.
 *
 */
public class SyncInitEndpoint {
    public static Object initEndpoint(Environment env, BObject wsSyncClient) {
        try {
            @SuppressWarnings(WebSocketConstants.UNCHECKED) BMap<BString, Object> clientEndpointConfig = wsSyncClient
                    .getMapValue(WebSocketConstants.CLIENT_ENDPOINT_CONFIG);
            String remoteUrl = wsSyncClient.getStringValue(WebSocketConstants.CLIENT_URL_CONFIG).getValue();
            BObject callbackService = wsSyncClient.getObjectValue(SYNC_CLIENT_SERVICE_CONFIG);
            WebSocketService wsService = WebSocketUtil
                    .validateAndCreateWebSocketService(env.getRuntime(), callbackService);
            HttpWsConnectorFactory connectorFactory = HttpUtil.createHttpWsConnectionFactory();
            WebSocketClientConnectorConfig clientConnectorConfig = new WebSocketClientConnectorConfig(remoteUrl);
            String scheme = URI.create(remoteUrl).getScheme();
            if (scheme == null) {
                return WebSocketUtil.getWebSocketError("Malformed URL: " + remoteUrl,
                        null, WebSocketConstants.ErrorCode.Error.errorCode(), null);
            }
            populateSyncClientConnectorConfig(clientEndpointConfig, clientConnectorConfig, scheme);
            WebSocketClientConnector clientConnector = connectorFactory.createWsClientConnector(clientConnectorConfig);
            wsSyncClient.addNativeData(WebSocketConstants.CONNECTOR_FACTORY, connectorFactory);
            wsSyncClient.addNativeData(WebSocketConstants.CLIENT_CONNECTOR, clientConnector);
            wsSyncClient.addNativeData(WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE,
                    clientConnectorConfig.getMaxFrameSize());
            SyncClientConnectorListener syncClientConnectorListener = null;
            if (wsSyncClient.getNativeData(WebSocketConstants.CLIENT_LISTENER) == null) {
                syncClientConnectorListener = new SyncClientConnectorListener();
                wsSyncClient.addNativeData(WebSocketConstants.CLIENT_LISTENER, syncClientConnectorListener);
            }
            CountDownLatch countDownLatch = new CountDownLatch(1);
            wsSyncClient.addNativeData(WebSocketConstants.COUNT_DOWN_LATCH, countDownLatch);
            ClientHandshakeFuture handshakeFuture = clientConnector.connect();
            handshakeFuture.setWebSocketConnectorListener(syncClientConnectorListener);
            handshakeFuture.setClientHandshakeListener(new WebSocketHandshakeListener(wsSyncClient, wsService,
                    syncClientConnectorListener, countDownLatch));
//            WebSocketUtil.establishWebSocketConnection(clientConnector, wsSyncClient, wsService);
            // Sets the count down latch for the initial connection.
            WebSocketUtil.waitForHandshake(wsSyncClient, countDownLatch);
            if (wsSyncClient.getNativeData(CLIENT_CONNECTION_ERROR) != null) {
                return createErrorByType((Throwable) wsSyncClient.getNativeData(CLIENT_CONNECTION_ERROR));
            }
        } catch (Exception e) {
            if (e instanceof BError) {
                return e;
            }
            return WebSocketUtil.getWebSocketError(e.getMessage(),
                    null, WebSocketConstants.ErrorCode.Error.errorCode(), null);
        }
        return null;
    }

    private static void populateSyncClientConnectorConfig(BMap<BString, Object> clientEndpointConfig,
            WebSocketClientConnectorConfig clientConnectorConfig,
            String scheme) {
        clientConnectorConfig.setAutoRead(false); // Frames should be read only when client starts reading
        clientConnectorConfig.setSubProtocols(WebSocketUtil.findNegotiableSubProtocols(clientEndpointConfig));
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        BMap<BString, Object> headerValues = (BMap<BString, Object>) clientEndpointConfig
                .getMapValue(WebSocketConstants.CUSTOM_HEADERS);
        if (headerValues != null) {
            clientConnectorConfig.addHeaders(WebSocketUtil.getCustomHeaders(headerValues));
        }

        clientConnectorConfig.setMaxFrameSize(findMaxFrameSize(clientEndpointConfig));

        BMap<BString, Object> secureSocket = (BMap<BString, Object>) clientEndpointConfig
                .getMapValue(WebSocketConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        if (secureSocket != null) {
            HttpUtil.populateSSLConfiguration(clientConnectorConfig, secureSocket);
        } else if (scheme.equals(WebSocketConstants.WSS_SCHEME)) {
            clientConnectorConfig.useJavaDefaults();
        }
        clientConnectorConfig.setWebSocketCompressionEnabled(
                clientEndpointConfig.getBooleanValue(WebSocketConstants.COMPRESSION_ENABLED_CONFIG));
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

    public static Object getHttpResponse(Environment env, BObject wsSyncClient) {
        return (wsSyncClient.getNativeData(WebSocketConstants.HTTP_RESPONSE));
    }

    private SyncInitEndpoint() {
    }
}


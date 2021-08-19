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

package io.ballerina.stdlib.websocket.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.HttpWsConnectorFactory;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketClientConnector;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketClientConnectorConfig;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.client.listener.SyncClientConnectorListener;
import java.net.URI;

/**
 * Initialize the WebSocket Synchronous Client.
 *
 */
public class SyncInitEndpoint {
    private static final String INTERVAL_IN_MILLIS = "intervalInMillis";
    private static final String MAX_WAIT_INTERVAL = "maxWaitIntervalInMillis";
    private static final String MAX_COUNT = "maxCount";
    private static final String BACK_OF_FACTOR = "backOffFactor";
    public static Object initEndpoint(Environment env, BObject wsSyncClient) {
        final Future balFuture = env.markAsync();
        try {
            @SuppressWarnings(WebSocketConstants.UNCHECKED) BMap<BString, Object> clientEndpointConfig = wsSyncClient
                    .getMapValue(WebSocketConstants.CLIENT_ENDPOINT_CONFIG);
            String remoteUrl = wsSyncClient.getStringValue(WebSocketConstants.CLIENT_URL_CONFIG).getValue();
            BObject callbackService = wsSyncClient.getObjectValue(WebSocketConstants.SYNC_CLIENT_SERVICE_CONFIG);
            WebSocketService wsService = WebSocketUtil
                    .validateAndCreateWebSocketService(env.getRuntime(), callbackService);
            HttpWsConnectorFactory connectorFactory = HttpUtil.createHttpWsConnectionFactory();
            WebSocketClientConnectorConfig clientConnectorConfig = new WebSocketClientConnectorConfig(remoteUrl);
            String scheme = URI.create(remoteUrl).getScheme();
            if (scheme == null) {
                balFuture.complete(WebSocketUtil.getWebSocketError("Malformed URL: " + remoteUrl,
                        null, WebSocketConstants.ErrorCode.Error.errorCode(), null));
                return null;
            }
            populateSyncClientConnectorConfig(clientEndpointConfig, clientConnectorConfig, scheme);
            if (WebSocketUtil.hasRetryConfig(wsSyncClient)) {
                @SuppressWarnings(WebSocketConstants.UNCHECKED)
                BMap<BString, Object> retryConfig = (BMap<BString, Object>) clientEndpointConfig
                        .getMapValue(WebSocketConstants.RETRY_CONFIG);
                RetryContext retryConnectorConfig = new RetryContext();
                populateRetryConnectorConfig(retryConfig, retryConnectorConfig);
                wsSyncClient.addNativeData(WebSocketConstants.RETRY_CONFIG.toString(), retryConnectorConfig);
            }
            WebSocketClientConnector clientConnector = connectorFactory.createWsClientConnector(clientConnectorConfig);
            wsSyncClient.addNativeData(WebSocketConstants.CONNECTOR_FACTORY, connectorFactory);
            wsSyncClient.addNativeData(WebSocketConstants.CLIENT_CONNECTOR, clientConnector);
            wsSyncClient.addNativeData(WebSocketConstants.NATIVE_DATA_MAX_FRAME_SIZE,
                    clientConnectorConfig.getMaxFrameSize());
            SyncClientConnectorListener syncClientConnectorListener = new SyncClientConnectorListener();
            wsSyncClient.addNativeData(WebSocketConstants.CLIENT_LISTENER, syncClientConnectorListener);
            WebSocketUtil.establishWebSocketConnection(wsSyncClient, wsService, balFuture);
//            ClientHandshakeFuture handshakeFuture = clientConnector.connect();
//            handshakeFuture.setWebSocketConnectorListener(syncClientConnectorListener);
//            handshakeFuture.setClientHandshakeListener(new WebSocketHandshakeListener(wsSyncClient, wsService,
//                    syncClientConnectorListener, balFuture));
        } catch (Exception e) {
            if (e instanceof BError) {
                balFuture.complete(e);
            } else {
                balFuture.complete(WebSocketUtil
                        .getWebSocketError(e.getMessage(), null, WebSocketConstants.ErrorCode.Error.errorCode(), null));
            }
        }
        return null;
    }

    private static void populateSyncClientConnectorConfig(BMap<BString, Object> clientEndpointConfig,
            WebSocketClientConnectorConfig clientConnectorConfig,
            String scheme) {
        clientConnectorConfig.setAutoRead(false); // Frames should be read only when client starts reading
        clientConnectorConfig.setSubProtocols(WebSocketUtil.findNegotiableSubProtocols(clientEndpointConfig));
        @SuppressWarnings(WebSocketConstants.UNCHECKED)
        long handshakeTimeoutInSeconds = WebSocketUtil.findTimeoutInSeconds(clientEndpointConfig,
                WebSocketConstants.CLIENT_HANDSHAKE_TIMEOUT, 300);
        clientConnectorConfig.setIdleTimeoutInMillis(Math.toIntExact(handshakeTimeoutInSeconds) * 1000);
        BMap<BString, Object> headerValues = (BMap<BString, Object>) clientEndpointConfig
                .getMapValue(WebSocketConstants.CUSTOM_HEADERS);
        if (headerValues != null) {
            clientConnectorConfig.addHeaders(WebSocketUtil.getCustomHeaders(headerValues));
        }

        clientConnectorConfig.setMaxFrameSize(WebSocketUtil.findMaxFrameSize(clientEndpointConfig));

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

    public static Object getHttpResponse(Environment env, BObject wsSyncClient) {
        return (wsSyncClient.getNativeData(WebSocketConstants.HTTP_RESPONSE));
    }

    /**
     * Populate the retry config.
     *  @param retryConfig - the retry config.
     * @param retryConnectorConfig - the retry connector config.
     */
    private static void populateRetryConnectorConfig(BMap<BString, Object> retryConfig,
                                                     RetryContext retryConnectorConfig) {
        retryConnectorConfig.setInterval(getIntValue(retryConfig, INTERVAL_IN_MILLIS, 1000));
        retryConnectorConfig.setBackOfFactor(getDoubleValue(retryConfig));
        retryConnectorConfig.setMaxInterval(getIntValue(retryConfig, MAX_WAIT_INTERVAL, 30000));
        retryConnectorConfig.setMaxAttempts(getIntValue(retryConfig, MAX_COUNT, 0));
    }

    private static int getIntValue(BMap<BString, Object> configs, String key, int defaultValue) {
        int value = Math.toIntExact(configs.getIntValue(StringUtils.fromString(key)));
        if (value < 0) {
//            logger.warn("The value set for `{}` needs to be great than than -1. The `{}` value is set to {}", key, key,
//                    defaultValue);
            value = defaultValue;
        }
        return value;
    }

    private static Double getDoubleValue(BMap<BString, Object> configs) {
        double value = Math.toRadians(configs.getFloatValue(StringUtils.fromString(BACK_OF_FACTOR)));
        if (value < 1) {
//            logger.warn("The value set for `backOffFactor` needs to be great than than 1. The `backOffFactor`" +
//                    " value is set to {}", 1.0);
            value = 1.0;
        }
        return value;
    }

    private SyncInitEndpoint() {
    }
}


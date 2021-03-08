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

package org.ballerinalang.net.websocket.serviceendpoint;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.config.ConfigRegistry;
import org.ballerinalang.net.http.BallerinaConnectorException;
import org.ballerinalang.net.http.HttpConnectionManager;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.transport.contract.ServerConnector;
import org.ballerinalang.net.transport.contract.config.ListenerConfiguration;
import org.ballerinalang.net.transport.contract.config.Parameter;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_VERSION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.ballerinalang.net.http.HttpConstants.ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS;
import static org.ballerinalang.net.http.HttpConstants.ENABLED_PROTOCOLS;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_CERTIFICATE;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_HANDSHAKE_TIMEOUT;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_KEY;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_KEY_PASSWORD;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_KEY_STORE;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_OCSP_STAPLING;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_PROTOCOLS;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_SESSION_TIMEOUT;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_TRUST_CERTIFICATES;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_TRUST_STORE;
import static org.ballerinalang.net.http.HttpConstants.ENDPOINT_CONFIG_VALIDATE_CERT;
import static org.ballerinalang.net.http.HttpConstants.FILE_PATH;
import static org.ballerinalang.net.http.HttpConstants.LISTENER_CONFIGURATION;
import static org.ballerinalang.net.http.HttpConstants.PASSWORD;
import static org.ballerinalang.net.http.HttpConstants.PKCS_STORE_TYPE;
import static org.ballerinalang.net.http.HttpConstants.PROTOCOL_HTTPS;
import static org.ballerinalang.net.http.HttpConstants.SERVER_NAME;
import static org.ballerinalang.net.http.HttpConstants.SSL_CONFIG_ENABLE_SESSION_CREATION;
import static org.ballerinalang.net.http.HttpConstants.SSL_CONFIG_SSL_VERIFY_CLIENT;
import static org.ballerinalang.net.http.HttpConstants.SSL_PROTOCOL_VERSION;
import static org.ballerinalang.net.http.HttpUtil.setInboundMgsSizeValidationConfig;
import static org.ballerinalang.net.transport.contract.Constants.HTTP_1_1_VERSION;
import static org.ballerinalang.net.websocket.WebSocketConstants.ANNOTATION_ATTR_TIMEOUT;
import static org.ballerinalang.net.websocket.WebSocketConstants.ENDPOINT_CONFIG_PORT;
import static org.ballerinalang.net.websocket.WebSocketConstants.HTTP_LISTENER;
import static org.ballerinalang.net.websocket.WebSocketConstants.HTTP_SERVER_CONNECTOR;
import static org.ballerinalang.net.websocket.WebSocketConstants.SERVICE_ENDPOINT_CONFIG;
import static org.ballerinalang.net.websocket.WebSocketUtil.createWebsocketError;

/**
 * Initialize the Websocket listener.
 *
 */
public class InitEndpoint extends AbstractWebsocketNativeFunction {
    public static Object initEndpoint(BObject serviceEndpoint) {
        ServerConnector httpServerConnector;
        try {
            if (serviceEndpoint.get(StringUtils.fromString(HTTP_LISTENER)) != null) {
                // Get the server connector started by the HTTP module
                httpServerConnector = (ServerConnector) ((BObject) serviceEndpoint
                        .get(StringUtils.fromString(HTTP_LISTENER))).getNativeData(HTTP_SERVER_CONNECTOR);
            } else {
                // Creating server connector
                BMap serviceEndpointConfig = serviceEndpoint.getMapValue(SERVICE_ENDPOINT_CONFIG);
                long port = serviceEndpoint.getIntValue(ENDPOINT_CONFIG_PORT);
                ListenerConfiguration listenerConfiguration = getListenerConfig(port, serviceEndpointConfig);
                httpServerConnector = HttpConnectionManager.getInstance()
                        .createHttpServerConnector(listenerConfiguration);
            }
            serviceEndpoint.addNativeData(HTTP_SERVER_CONNECTOR, httpServerConnector);
            //Adding service registries to native data
            resetRegistry(serviceEndpoint);
            return null;
        } catch (BError errorValue) {
            return errorValue;
        } catch (Exception e) {
            return createWebsocketError(e.getMessage(), WebSocketConstants.ErrorCode.WsGenericListenerError);
        }
    }

    /**
     * Returns Listener configuration instance populated with endpoint config.
     *
     * @param port           listener port.
     * @param endpointConfig listener endpoint configuration.
     * @return transport listener configuration instance.
     */
    private static ListenerConfiguration getListenerConfig(long port, BMap endpointConfig) {
        String host = endpointConfig.getStringValue(HttpConstants.ENDPOINT_CONFIG_HOST).getValue();
        BMap sslConfig = endpointConfig.getMapValue(HttpConstants.ENDPOINT_CONFIG_SECURE_SOCKET);
        long idleTimeout = ((long) ((BDecimal) endpointConfig.get(ANNOTATION_ATTR_TIMEOUT)).floatValue()) * 1000;

        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();
        BMap<BString, Object> http1Settings = (BMap<BString, Object>) endpointConfig.get(HttpConstants.HTTP1_SETTINGS);
        listenerConfiguration.setPipeliningLimit(http1Settings.getIntValue(HttpConstants.PIPELINING_REQUEST_LIMIT));
        String keepAlive = http1Settings.getStringValue(HttpConstants.ENDPOINT_CONFIG_KEEP_ALIVE).getValue();
        listenerConfiguration.setKeepAliveConfig(HttpUtil.getKeepAliveConfig(keepAlive));

        BMap<BString, Object> requestLimits = (BMap<BString, Object>) endpointConfig
                .getMapValue(HttpConstants.REQUEST_LIMITS);
        setInboundMgsSizeValidationConfig(requestLimits.getIntValue(HttpConstants.MAX_URI_LENGTH),
                requestLimits.getIntValue(HttpConstants.MAX_HEADER_SIZE),
                requestLimits.getIntValue(HttpConstants.MAX_ENTITY_BODY_SIZE),
                listenerConfiguration.getMsgSizeValidationConfig());

        if (host == null || host.trim().isEmpty()) {
            listenerConfiguration.setHost(ConfigRegistry.getInstance()
                    .getConfigOrDefault("b7a.websocket.host", WebSocketConstants.WEBSOCKET_DEFAULT_HOST));
        } else {
            listenerConfiguration.setHost(host);
        }

        if (port == 0) {
            throw new BallerinaConnectorException("Listener port is not defined!");
        }
        listenerConfiguration.setPort(Math.toIntExact(port));

        if (idleTimeout < 0) {
            throw new BallerinaConnectorException(
                    "Idle timeout cannot be negative. If you want to disable the " + "timeout please use value 0");
        }
        listenerConfiguration.setSocketIdleTimeout(Math.toIntExact(idleTimeout));

        listenerConfiguration.setVersion(HTTP_1_1_VERSION);

        if (endpointConfig.getType().getName().equalsIgnoreCase(LISTENER_CONFIGURATION)) {
            BString serverName = endpointConfig.getStringValue(SERVER_NAME);
            listenerConfiguration.setServerHeader(serverName != null ? serverName.getValue() : getServerName());
        } else {
            listenerConfiguration.setServerHeader(getServerName());
        }

        if (sslConfig != null) {
            return setSslConfig(sslConfig, listenerConfiguration);
        }

        listenerConfiguration.setPipeliningEnabled(true); //Pipelining is enabled all the time
        Object webSocketCompressionEnabled = endpointConfig.get(WebSocketConstants.COMPRESSION_ENABLED_CONFIG);
        if (webSocketCompressionEnabled != null) {
            listenerConfiguration.setWebSocketCompressionEnabled((Boolean) webSocketCompressionEnabled);
        }

        return listenerConfiguration;
    }

    private static String getServerName() {
        String userAgent;
        String version = System.getProperty(BALLERINA_VERSION);
        if (version != null) {
            userAgent = "ballerina/" + version;
        } else {
            userAgent = "ballerina";
        }
        return userAgent;
    }

    private static ListenerConfiguration setSslConfig(BMap sslConfig, ListenerConfiguration listenerConfiguration) {
        listenerConfiguration.setScheme(PROTOCOL_HTTPS);
        BMap trustStore = sslConfig.getMapValue(ENDPOINT_CONFIG_TRUST_STORE);
        BMap keyStore = sslConfig.getMapValue(ENDPOINT_CONFIG_KEY_STORE);
        BMap protocols = sslConfig.getMapValue(ENDPOINT_CONFIG_PROTOCOLS);
        BMap validateCert = sslConfig.getMapValue(ENDPOINT_CONFIG_VALIDATE_CERT);
        BMap ocspStapling = sslConfig.getMapValue(ENDPOINT_CONFIG_OCSP_STAPLING);
        String keyFile = sslConfig.getStringValue(ENDPOINT_CONFIG_KEY).getValue();
        String certFile = sslConfig.getStringValue(ENDPOINT_CONFIG_CERTIFICATE).getValue();
        String trustCerts = sslConfig.getStringValue(ENDPOINT_CONFIG_TRUST_CERTIFICATES).getValue();
        String keyPassword = sslConfig.getStringValue(ENDPOINT_CONFIG_KEY_PASSWORD).getValue();

        if (keyStore != null && isNotBlank(keyFile)) {
            throw WebSocketUtil.createWebsocketError("Cannot configure both keyStore and keyFile at the same time.",
                    WebSocketConstants.ErrorCode.SslError);
        } else if (keyStore == null && (isBlank(keyFile) || isBlank(certFile))) {
            throw WebSocketUtil.createWebsocketError(
                    "Either keystore or certificateKey and server certificates must be provided "
                            + "for secure connection", WebSocketConstants.ErrorCode.SslError);
        }
        if (keyStore != null) {
            String keyStoreFile = keyStore.getStringValue(FILE_PATH).getValue();
            if (isBlank(keyStoreFile)) {
                throw WebSocketUtil
                        .createWebsocketError("Keystore file location must be provided for secure connection.",
                                WebSocketConstants.ErrorCode.SslError);
            }
            String keyStorePassword = keyStore.getStringValue(PASSWORD).getValue();
            if (isBlank(keyStorePassword)) {
                throw WebSocketUtil.createWebsocketError("Keystore password must be provided for secure connection",
                        WebSocketConstants.ErrorCode.SslError);
            }
            listenerConfiguration.setKeyStoreFile(keyStoreFile);
            listenerConfiguration.setKeyStorePass(keyStorePassword);
        } else {
            listenerConfiguration.setServerKeyFile(keyFile);
            listenerConfiguration.setServerCertificates(certFile);
            if (isNotBlank(keyPassword)) {
                listenerConfiguration.setServerKeyPassword(keyPassword);
            }
        }
        String sslVerifyClient = sslConfig.getStringValue(SSL_CONFIG_SSL_VERIFY_CLIENT).getValue();
        listenerConfiguration.setVerifyClient(sslVerifyClient);
        listenerConfiguration
                .setSslSessionTimeOut((int) (sslConfig).getDefaultableIntValue(ENDPOINT_CONFIG_SESSION_TIMEOUT));
        listenerConfiguration
                .setSslHandshakeTimeOut((sslConfig).getDefaultableIntValue(ENDPOINT_CONFIG_HANDSHAKE_TIMEOUT));
        if (trustStore == null && isNotBlank(sslVerifyClient) && isBlank(trustCerts)) {
            throw WebSocketUtil.createWebsocketError(
                    "Truststore location or trustCertificates must be provided to enable Mutual SSL",
                    WebSocketConstants.ErrorCode.SslError);
        }
        if (trustStore != null) {
            String trustStoreFile = trustStore.getStringValue(FILE_PATH).getValue();
            String trustStorePassword = trustStore.getStringValue(PASSWORD).getValue();
            if (isBlank(trustStoreFile) && isNotBlank(sslVerifyClient)) {
                throw WebSocketUtil.createWebsocketError("Truststore location must be provided to enable Mutual SSL",
                        WebSocketConstants.ErrorCode.SslError);
            }
            if (isBlank(trustStorePassword) && isNotBlank(sslVerifyClient)) {
                throw WebSocketUtil
                        .createWebsocketError("Truststore password value must be provided to enable Mutual SSL",
                                WebSocketConstants.ErrorCode.SslError);
            }
            listenerConfiguration.setTrustStoreFile(trustStoreFile);
            listenerConfiguration.setTrustStorePass(trustStorePassword);
        } else if (isNotBlank(trustCerts)) {
            listenerConfiguration.setServerTrustCertificates(trustCerts);
        }
        List<Parameter> serverParamList = new ArrayList<>();
        Parameter serverParameters;
        if (protocols != null) {
            List<String> sslEnabledProtocolsValueList = Arrays
                    .asList(protocols.getArrayValue(ENABLED_PROTOCOLS).getStringArray());
            if (!sslEnabledProtocolsValueList.isEmpty()) {
                String sslEnabledProtocols = sslEnabledProtocolsValueList.stream()
                        .collect(Collectors.joining(",", "", ""));
                serverParameters = new Parameter(ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS, sslEnabledProtocols);
                serverParamList.add(serverParameters);
            }

            String sslProtocol = protocols.getStringValue(SSL_PROTOCOL_VERSION).getValue();
            if (isNotBlank(sslProtocol)) {
                listenerConfiguration.setSSLProtocol(sslProtocol);
            }
        }

        List<String> ciphersValueList = Arrays
                .asList(sslConfig.getArrayValue(HttpConstants.SSL_CONFIG_CIPHERS).getStringArray());
        if (!ciphersValueList.isEmpty()) {
            String ciphers = ciphersValueList.stream().collect(Collectors.joining(",", "", ""));
            serverParameters = new Parameter(HttpConstants.CIPHERS, ciphers);
            serverParamList.add(serverParameters);
        }
        if (validateCert != null) {
            boolean validateCertificateEnabled = validateCert.getBooleanValue(HttpConstants.ENABLE);
            long cacheSize = validateCert.getIntValue(HttpConstants.SSL_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = validateCert.getIntValue(HttpConstants.SSL_CONFIG_CACHE_VALIDITY_PERIOD);
            listenerConfiguration.setValidateCertEnabled(validateCertificateEnabled);
            if (validateCertificateEnabled) {
                if (cacheSize != 0) {
                    listenerConfiguration.setCacheSize(Math.toIntExact(cacheSize));
                }
                if (cacheValidationPeriod != 0) {
                    listenerConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheValidationPeriod));
                }
            }
        }
        if (ocspStapling != null) {
            boolean ocspStaplingEnabled = ocspStapling.getBooleanValue(HttpConstants.ENABLE);
            listenerConfiguration.setOcspStaplingEnabled(ocspStaplingEnabled);
            long cacheSize = ocspStapling.getIntValue(HttpConstants.SSL_CONFIG_CACHE_SIZE);
            long cacheValidationPeriod = ocspStapling.getIntValue(HttpConstants.SSL_CONFIG_CACHE_VALIDITY_PERIOD);
            listenerConfiguration.setValidateCertEnabled(ocspStaplingEnabled);
            if (ocspStaplingEnabled) {
                if (cacheSize != 0) {
                    listenerConfiguration.setCacheSize(Math.toIntExact(cacheSize));
                }
                if (cacheValidationPeriod != 0) {
                    listenerConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheValidationPeriod));
                }
            }
        }
        listenerConfiguration.setTLSStoreType(PKCS_STORE_TYPE);
        String serverEnableSessionCreation = String
                .valueOf(sslConfig.getBooleanValue(SSL_CONFIG_ENABLE_SESSION_CREATION));
        Parameter enableSessionCreationParam = new Parameter(SSL_CONFIG_ENABLE_SESSION_CREATION.getValue(),
                serverEnableSessionCreation);
        serverParamList.add(enableSessionCreationParam);
        if (!serverParamList.isEmpty()) {
            listenerConfiguration.setParameters(serverParamList);
        }

        listenerConfiguration
                .setId(HttpUtil.getListenerInterface(listenerConfiguration.getHost(), listenerConfiguration.getPort()));

        return listenerConfiguration;
    }
}

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

import io.ballerina.runtime.api.values.BArray;
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
import org.ballerinalang.net.transport.contract.config.SslConfiguration;
import org.ballerinalang.net.websocket.WebSocketConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_VERSION;
import static org.ballerinalang.net.http.HttpConstants.ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS;
import static org.ballerinalang.net.http.HttpConstants.LISTENER_CONFIGURATION;
import static org.ballerinalang.net.http.HttpConstants.PKCS_STORE_TYPE;
import static org.ballerinalang.net.http.HttpConstants.PROTOCOL_HTTPS;
import static org.ballerinalang.net.http.HttpConstants.SERVER_NAME;
import static org.ballerinalang.net.http.HttpUtil.setInboundMgsSizeValidationConfig;
import static org.ballerinalang.net.transport.contract.Constants.HTTP_1_1_VERSION;
import static org.ballerinalang.net.websocket.WebSocketConstants.ANNOTATION_ATTR_TIMEOUT;
import static org.ballerinalang.net.websocket.WebSocketConstants.ENDPOINT_CONFIG_PORT;
import static org.ballerinalang.net.websocket.WebSocketConstants.HTTP_SERVER_CONNECTOR;
import static org.ballerinalang.net.websocket.WebSocketConstants.SERVICE_ENDPOINT_CONFIG;
import static org.ballerinalang.net.websocket.WebSocketUtil.createWebsocketError;

/**
 * Initialize the Websocket listener.
 *
 */
public class InitEndpoint extends AbstractWebsocketNativeFunction {
    public static Object initEndpoint(BObject serviceEndpoint) {
        try {
            // Creating server connector
            BMap serviceEndpointConfig = serviceEndpoint.getMapValue(SERVICE_ENDPOINT_CONFIG);
            long port = serviceEndpoint.getIntValue(ENDPOINT_CONFIG_PORT);
            ListenerConfiguration listenerConfiguration = getListenerConfig(port, serviceEndpointConfig);
            ServerConnector httpServerConnector =
                    HttpConnectionManager.getInstance().createHttpServerConnector(listenerConfiguration);
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
        BMap<BString, Object> sslConfig = endpointConfig.getMapValue(HttpConstants.ENDPOINT_CONFIG_SECURESOCKET);
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

    private static ListenerConfiguration setSslConfig(BMap<BString, Object> secureSocket,
            ListenerConfiguration listenerConfiguration) {
        List<Parameter> serverParamList = new ArrayList<>();
        listenerConfiguration.setScheme(PROTOCOL_HTTPS);
        BMap<BString, Object> key = getBMapValueIfPresent(secureSocket, HttpConstants.SECURESOCKET_CONFIG_KEY);
        assert key != null; // This validation happens at Ballerina level
        evaluateKeyField(key, listenerConfiguration);
        BMap<BString, Object> mutualSsl = getBMapValueIfPresent(secureSocket,
                HttpConstants.SECURESOCKET_CONFIG_MUTUAL_SSL);
        if (mutualSsl != null) {
            String verifyClient = mutualSsl.getStringValue(HttpConstants.SECURESOCKET_CONFIG_VERIFY_CLIENT).getValue();
            listenerConfiguration.setVerifyClient(verifyClient);
            Object cert = mutualSsl.get(HttpConstants.SECURESOCKET_CONFIG_CERT);
            evaluateCertField(cert, listenerConfiguration);
        }
        BMap<BString, Object> protocol = getBMapValueIfPresent(secureSocket,
                HttpConstants.SECURESOCKET_CONFIG_PROTOCOL);
        if (protocol != null) {
            evaluateProtocolField(protocol, listenerConfiguration, serverParamList);
        }
        BMap<BString, Object> certValidation = getBMapValueIfPresent(secureSocket,
                HttpConstants.SECURESOCKET_CONFIG_CERT_VALIDATION);
        if (certValidation != null) {
            evaluateCertValidationField(certValidation, listenerConfiguration);
        }
        BArray ciphers = secureSocket.containsKey(HttpConstants.SECURESOCKET_CONFIG_CIPHERS) ?
                secureSocket.getArrayValue(HttpConstants.SECURESOCKET_CONFIG_CIPHERS) :
                null;
        if (ciphers != null) {
            evaluateCiphersField(ciphers, serverParamList);
        }
        evaluateCommonFields(secureSocket, listenerConfiguration, serverParamList);

        listenerConfiguration.setTLSStoreType(PKCS_STORE_TYPE);
        if (!serverParamList.isEmpty()) {
            listenerConfiguration.setParameters(serverParamList);
        }
        listenerConfiguration
                .setId(HttpUtil.getListenerInterface(listenerConfiguration.getHost(), listenerConfiguration.getPort()));
        return listenerConfiguration;
    }

    private static void evaluateKeyField(BMap<BString, Object> key, SslConfiguration sslConfiguration) {
        if (key.containsKey(HttpConstants.SECURESOCKET_CONFIG_KEYSTORE_FILE_PATH)) {
            String keyStoreFile = key.getStringValue(HttpConstants.SECURESOCKET_CONFIG_KEYSTORE_FILE_PATH).getValue();
            if (keyStoreFile.isBlank()) {
                throw createWebsocketError("KeyStore file location must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            String keyStorePassword = key.getStringValue(HttpConstants.SECURESOCKET_CONFIG_KEYSTORE_PASSWORD)
                    .getValue();
            if (keyStorePassword.isBlank()) {
                throw createWebsocketError("KeyStore password must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            sslConfiguration.setKeyStoreFile(keyStoreFile);
            sslConfiguration.setKeyStorePass(keyStorePassword);
        } else {
            String certFile = key.getStringValue(HttpConstants.SECURESOCKET_CONFIG_CERTKEY_CERT_FILE).getValue();
            String keyFile = key.getStringValue(HttpConstants.SECURESOCKET_CONFIG_CERTKEY_KEY_FILE).getValue();
            BString keyPassword = key.containsKey(HttpConstants.SECURESOCKET_CONFIG_CERTKEY_KEY_PASSWORD) ?
                    key.getStringValue(HttpConstants.SECURESOCKET_CONFIG_CERTKEY_KEY_PASSWORD) :
                    null;
            if (certFile.isBlank()) {
                throw createWebsocketError("Certificate file location must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            if (keyFile.isBlank()) {
                throw createWebsocketError("Private key file location must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            if (sslConfiguration instanceof ListenerConfiguration) {
                sslConfiguration.setServerCertificates(certFile);
                sslConfiguration.setServerKeyFile(keyFile);
                if (keyPassword != null && !keyPassword.getValue().isBlank()) {
                    sslConfiguration.setServerKeyPassword(keyPassword.getValue());
                }
            } else {
                sslConfiguration.setClientCertificates(certFile);
                sslConfiguration.setClientKeyFile(keyFile);
                if (keyPassword != null && !keyPassword.getValue().isBlank()) {
                    sslConfiguration.setClientKeyPassword(keyPassword.getValue());
                }
            }
        }
    }

    private static void evaluateCertField(Object cert, SslConfiguration sslConfiguration) {
        if (cert instanceof BMap) {
            BMap<BString, BString> trustStore = (BMap<BString, BString>) cert;
            String trustStoreFile = trustStore.getStringValue(HttpConstants.SECURESOCKET_CONFIG_TRUSTSTORE_FILE_PATH)
                    .getValue();
            String trustStorePassword = trustStore.getStringValue(HttpConstants.SECURESOCKET_CONFIG_TRUSTSTORE_PASSWORD)
                    .getValue();
            if (trustStoreFile.isBlank()) {
                throw createWebsocketError("TrustStore file location must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            if (trustStorePassword.isBlank()) {
                throw createWebsocketError("TrustStore password must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            sslConfiguration.setTrustStoreFile(trustStoreFile);
            sslConfiguration.setTrustStorePass(trustStorePassword);
        } else {
            String certFile = ((BString) cert).getValue();
            if (certFile.isBlank()) {
                throw createWebsocketError("Certificate file location must be provided for secure connection.",
                        WebSocketConstants.ErrorCode.SslError);
            }
            if (sslConfiguration instanceof ListenerConfiguration) {
                sslConfiguration.setServerTrustCertificates(certFile);
            } else {
                sslConfiguration.setClientTrustCertificates(certFile);
            }
        }
    }

    private static void evaluateProtocolField(BMap<BString, Object> protocol, SslConfiguration sslConfiguration,
            List<Parameter> paramList) {
        List<String> sslEnabledProtocolsValueList = Arrays
                .asList(protocol.getArrayValue(HttpConstants.SECURESOCKET_CONFIG_PROTOCOL_VERSIONS).getStringArray());
        if (!sslEnabledProtocolsValueList.isEmpty()) {
            String sslEnabledProtocols = sslEnabledProtocolsValueList.stream().collect(Collectors.joining(",", "", ""));
            Parameter serverProtocols = new Parameter(ANN_CONFIG_ATTR_SSL_ENABLED_PROTOCOLS, sslEnabledProtocols);
            paramList.add(serverProtocols);
        }
        String sslProtocol = protocol.getStringValue(HttpConstants.SECURESOCKET_CONFIG_PROTOCOL_NAME).getValue();
        if (!sslProtocol.isBlank()) {
            sslConfiguration.setSSLProtocol(sslProtocol);
        }
    }

    private static void evaluateCertValidationField(BMap<BString, Object> certValidation,
            SslConfiguration sslConfiguration) {
        String type = certValidation.getStringValue(HttpConstants.SECURESOCKET_CONFIG_CERT_VALIDATION_TYPE).getValue();
        if (type.equals(HttpConstants.SECURESOCKET_CONFIG_CERT_VALIDATION_TYPE_OCSP_STAPLING.getValue())) {
            sslConfiguration.setOcspStaplingEnabled(true);
        } else {
            sslConfiguration.setValidateCertEnabled(true);
        }
        long cacheSize = certValidation.getIntValue(HttpConstants.SECURESOCKET_CONFIG_CERT_VALIDATION_CACHE_SIZE)
                .intValue();
        long cacheValidityPeriod = ((BDecimal) certValidation
                .get(HttpConstants.SECURESOCKET_CONFIG_CERT_VALIDATION_CACHE_VALIDITY_PERIOD)).intValue();
        if (cacheValidityPeriod != 0) {
            sslConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheValidityPeriod));
        }
        if (cacheSize != 0) {
            sslConfiguration.setCacheSize(Math.toIntExact(cacheSize));
        }
    }

    private static void evaluateCiphersField(BArray ciphers, List<Parameter> paramList) {
        Object[] ciphersArray = ciphers.getStringArray();
        List<Object> ciphersList = Arrays.asList(ciphersArray);
        if (ciphersList.size() > 0) {
            String ciphersString = ciphersList.stream().map(Object::toString).collect(Collectors.joining(",", "", ""));
            Parameter serverParameters = new Parameter(HttpConstants.CIPHERS, ciphersString);
            paramList.add(serverParameters);
        }
    }

    private static void evaluateCommonFields(BMap<BString, Object> secureSocket, SslConfiguration sslConfiguration,
            List<Parameter> paramList) {
        if (!(sslConfiguration instanceof ListenerConfiguration)) {
            boolean hostNameVerificationEnabled = secureSocket
                    .getBooleanValue(HttpConstants.SECURESOCKET_CONFIG_HOST_NAME_VERIFICATION_ENABLED);
            sslConfiguration.setHostNameVerificationEnabled(hostNameVerificationEnabled);
        }
        sslConfiguration.setSslSessionTimeOut(
                (int) getLongValueOrDefault(secureSocket, HttpConstants.SECURESOCKET_CONFIG_SESSION_TIMEOUT));
        sslConfiguration.setSslHandshakeTimeOut(
                getLongValueOrDefault(secureSocket, HttpConstants.SECURESOCKET_CONFIG_HANDSHAKE_TIMEOUT));
        String enableSessionCreation = String
                .valueOf(secureSocket.getBooleanValue(HttpConstants.SECURESOCKET_CONFIG_SHARE_SESSION));
        Parameter enableSessionCreationParam = new Parameter(HttpConstants.SECURESOCKET_CONFIG_SHARE_SESSION.getValue(),
                enableSessionCreation);
        paramList.add(enableSessionCreationParam);
    }

    private static BMap<BString, Object> getBMapValueIfPresent(BMap<BString, Object> map, BString key) {
        return map.containsKey(key) ? (BMap<BString, Object>) map.getMapValue(key) : null;
    }

    private static long getLongValueOrDefault(BMap<BString, Object> map, BString key) {
        return map.containsKey(key) ? ((BDecimal) map.get(key)).intValue() : 0L;
    }
}

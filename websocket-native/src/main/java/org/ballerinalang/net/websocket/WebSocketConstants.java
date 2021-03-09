/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.net.websocket;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Constants of WebSocket.
 */
public class WebSocketConstants {

    public static final String BALLERINA_ORG = "ballerina";
    public static final String PACKAGE_HTTP = "http";
    public static final String PACKAGE_WEBSOCKET = "websocket";
    public static final String SEPARATOR = ":";
    public static final String LISTENER = "Listener";
    public static final String WEBSOCKET_CONNECTOR = "WebSocketConnector";
    public static final String WEBSOCKET_CALLER = "Caller";
    public static final String WEBSOCKET_ASYNC_CLIENT = "AsyncClient";
    public static final String WEBSOCKET_SERVICE = "WebSocketService";
    public static final String WEBSOCKET_CLIENT_SERVICE = "WebSocketClientService";
    public static final String WSS_SCHEME = "wss";
    public static final String WS_SCHEME = "ws";
    public static final String BACK_SLASH = "/";
    public static final String GET = "get";
    public static final String WEBSOCKET_CLIENT_NAME = PACKAGE_WEBSOCKET + SEPARATOR + WEBSOCKET_CALLER;

    public static final String WEBSOCKET_ANNOTATION_CONFIGURATION = "ServiceConfig";
    public static final BString ANNOTATION_ATTR_PATH = StringUtils.fromString("path");
    public static final BString ANNOTATION_ATTR_SUB_PROTOCOLS = StringUtils.fromString("subProtocols");
    public static final BString ANNOTATION_ATTR_IDLE_TIMEOUT = StringUtils.fromString("idleTimeout");
    public static final BString ANNOTATION_ATTR_READ_IDLE_TIMEOUT = StringUtils.fromString("readTimeout");
    public static final BString ANNOTATION_ATTR_TIMEOUT = StringUtils.fromString("timeout");
    public static final BString ANNOTATION_ATTR_MAX_FRAME_SIZE = StringUtils.fromString("maxFrameSize");

    public static final String RESOURCE_NAME_ON_OPEN = "onOpen";
    public static final String RESOURCE_NAME_ON_TEXT_MESSAGE = "onTextMessage";
    public static final String RESOURCE_NAME_ON_BINARY_MESSAGE = "onBinaryMessage";
    public static final String RESOURCE_NAME_ON_PING = "onPing";
    public static final String RESOURCE_NAME_ON_PONG = "onPong";
    public static final String RESOURCE_NAME_ON_CLOSE = "onClose";
    public static final String RESOURCE_NAME_ON_IDLE_TIMEOUT = "onIdleTimeout";
    public static final String RESOURCE_NAME_ON_ERROR = "onError";
    public static final String RESOURCE_NAME_CLOSE = "close";
    public static final String RESOURCE_NAME_PING = "ping";
    public static final String RESOURCE_NAME_PONG = "pong";
    public static final String WRITE_BINARY_MESSAGE = "writeBinaryMessage";
    public static final String WRITE_TEXT_MESSAGE = "writeTextMessage";
    public static final String RESOURCE_NAME_READY = "ready";
    public static final String RESOURCE_NAME_UPGRADE = "onUpgrade";

    public static final String WEBSOCKET_HANDSHAKER = "WEBSOCKET_MESSAGE";

    public static final String NATIVE_DATA_WEBSOCKET_CONNECTION_INFO = "NATIVE_DATA_WEBSOCKET_CONNECTION_INFO";
    public static final String NATIVE_DATA_BASE_PATH = "BASE_PATH";
    public static final String NATIVE_DATA_MAX_FRAME_SIZE = "MAX_FRAME_SIZE";

    public static final BString CLIENT_URL_CONFIG = StringUtils.fromString("url");
    public static final BString CLIENT_SERVICE_CONFIG = StringUtils.fromString("callbackService");
    public static final BString SYNC_CLIENT_SERVICE_CONFIG = StringUtils.fromString("pingPongService");
    public static final BString CUSTOM_HEADERS = StringUtils.fromString("customHeaders");
    public static final BString CLIENT_READY_ON_CONNECT = StringUtils.fromString("readyOnConnect");
    public static final BString WEBSOCKET_UPGRADE_SERVICE_CONFIG = StringUtils.fromString("upgradeService");
    public static final String SYNC_CLIENT = "Client";

    public static final BString RETRY_CONTEXT = StringUtils.fromString("retryConfig");
    public static final String COUNT_DOWN_LATCH = "countDownLatch";
    public static final String CLIENT_LISTENER = "clientListener";
    public static final String CLIENT_CONNECTOR = "clientConnector";
    public static final String CLIENT_CONNECTION_ERROR = "clientConnectorError";

    public static final BString CLIENT_ENDPOINT_CONFIG = StringUtils.fromString("config");
    public static final BString TARGET_URLS = StringUtils.fromString("targetUrls");
    public static final String FAILOVER_CONTEXT = "failoverContext";
    public static final String CONNECTOR_FACTORY = "connectorFactory";
    public static final String FAILOVER_WEBSOCKET_CLIENT = "WebSocketFailoverClient";
    public static final String FULL_FAILOVER_WEBSOCKET_CLIENT_NAME =
            ModuleUtils.getPackageIdentifier() + SEPARATOR + FAILOVER_WEBSOCKET_CLIENT;
    public static final BString ENDPOINT_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");

    public static final String COLON = ":";
    public static final String PACKAGE = "ballerina";
    public static final String PROTOCOL_WEBSOCKET = "websocket";

    public static final BString COMPRESSION_ENABLED_CONFIG = StringUtils.fromString("webSocketCompressionEnabled");

    // WebSocketListener field names
    public static final BString LISTENER_ID_FIELD = StringUtils.fromString("id");
    public static final BString LISTENER_NEGOTIATED_SUBPROTOCOLS_FIELD = StringUtils.fromString(
            "negotiatedSubProtocol");
    public static final BString INITIALIZED_BY_SERVICE = StringUtils.fromString("initializedByService");
    public static final BString LISTENER_IS_SECURE_FIELD = StringUtils.fromString("secure");
    public static final BString LISTENER_IS_OPEN_FIELD = StringUtils.fromString("open");
    public static final BString LISTENER_CONNECTOR_FIELD = StringUtils.fromString("conn");

    // WebSocketClient struct field names
    public static final BString CLIENT_RESPONSE_FIELD = StringUtils.fromString("response");
    public static final BString CLIENT_CONNECTOR_FIELD = StringUtils.fromString("conn");

    public static final String WEBSOCKET_ERROR_DETAILS = "Detail";

    // WebSocketConnector
    public static final BString CONNECTOR_IS_READY_FIELD = StringUtils.fromString("isReady");

    public static final int STATUS_CODE_ABNORMAL_CLOSURE = 1006;
    public static final int STATUS_CODE_FOR_NO_STATUS_CODE_PRESENT = 1005;

    public static final int DEFAULT_MAX_FRAME_SIZE = 65536;

    // Warning suppression
    public static final String UNCHECKED = "unchecked";
    public static final String WEBSOCKET_CONNECTION_FAILURE = "WebSocket connection failure";

    public static final String WS_SERVICE_REGISTRY = "WS_SERVICE_REGISTRY";
    public static final BString SERVICE_ENDPOINT_CONFIG = StringUtils.fromString("config");
    public static final BString ENDPOINT_CONFIG_PORT = StringUtils.fromString("port");
    public static final String HTTP_SERVER_CONNECTOR = "HTTP_SERVER_CONNECTOR";
    public static final String CONNECTOR_STARTED = "CONNECTOR_STARTED";
    public static final String HTTP_LISTENER = "httpListener";

    public static final String PARAM_TYPE_STRING = "string";
    public static final String PARAM_TYPE_BYTE_ARR = "byte[]";

    public static final String WSS_ENDPOINT_STARTED = "[ballerina/websocket] started WSS listener ";
    public static final String WS_ENDPOINT_STARTED = "[ballerina/websocket] started WS listener ";
    public static final String WSS_ENDPOINT_STOPPED = "[ballerina/websocket] stopped WSS listener ";
    public static final String WS_ENDPOINT_STOPPED = "[ballerina/websocket] stopped WS listener ";

    public static final String WEBSOCKET_DEFAULT_HOST = "0.0.0.0";

    public WebSocketConstants() {
    }

    /**
     * Specifies the error code for webSocket module.
     */
    public enum ErrorCode {

        ConnectionClosureError("ConnectionClosureError"),
        InvalidHandshakeError("InvalidHandshakeError"),
        PayloadTooLargeError("PayloadTooLargeError"),
        ProtocolError("ProtocolError"),
        ConnectionError("ConnectionError"),
        InvalidContinuationFrameError("InvalidContinuationFrameError"),
        HandshakeTimedOut("HandshakeTimedOut"),
        ReadTimedOutError("ReadTimedOutError"),
        SslError("SslError"),
        Error("Error");

        private String errorCode;

        ErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String errorCode() {
            return errorCode;
        }
    }
}

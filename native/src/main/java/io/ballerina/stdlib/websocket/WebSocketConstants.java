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

package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.api.nativeimpl.ModuleUtils;

import static io.ballerina.stdlib.http.api.HttpConstants.ANN_NAME_HEADER;
import static io.ballerina.stdlib.http.api.HttpConstants.COLON;

/**
 * Constants of WebSocket.
 */
public class WebSocketConstants {

    public static final String BALLERINA_ORG = "ballerina";
    public static final String PACKAGE_WEBSOCKET = "websocket";
    public static final String SEPARATOR = ":";
    public static final String WEBSOCKET_CALLER = "Caller";
    public static final String WSS_SCHEME = "wss";
    public static final String BACK_SLASH = "/";
    public static final String GET = "get";
    public static final String WEBSOCKET_CLIENT_NAME = PACKAGE_WEBSOCKET + SEPARATOR + WEBSOCKET_CALLER;

    public static final String WEBSOCKET_ANNOTATION_CONFIGURATION = "ServiceConfig";
    public static final BString ANNOTATION_ATTR_SUB_PROTOCOLS = StringUtils.fromString("subProtocols");
    public static final BString ANNOTATION_ATTR_IDLE_TIMEOUT = StringUtils.fromString("idleTimeout");
    public static final BString ANNOTATION_ATTR_READ_IDLE_TIMEOUT = StringUtils.fromString("readTimeout");
    public static final BString ANNOTATION_ATTR_TIMEOUT = StringUtils.fromString("timeout");
    public static final BString ANNOTATION_ATTR_MAX_FRAME_SIZE = StringUtils.fromString("maxFrameSize");
    public static final BString ANNOTATION_ATTR_VALIDATION_ENABLED = StringUtils.fromString("validation");
    public static final BString ANNOTATION_ATTR_DISPATCHER_KEY = StringUtils.fromString("dispatcherKey");

    public static final BString RETRY_CONFIG = StringUtils.fromString("retryConfig");
    public static final String LOG_MESSAGE = "{} {}";
    public static final int STATUS_CODE_ABNORMAL_CLOSURE = 1006;

    public static final String RESOURCE_NAME_ON_OPEN = "onOpen";
    public static final String RESOURCE_NAME_ON_TEXT_MESSAGE = "onTextMessage";
    public static final String RESOURCE_NAME_ON_BINARY_MESSAGE = "onBinaryMessage";
    public static final String RESOURCE_NAME_ON_MESSAGE = "onMessage";
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
    public static final String RESOURCE_NAME_UPGRADE = "onUpgrade";

    public static final String NATIVE_DATA_WEBSOCKET_CONNECTION_INFO = "NATIVE_DATA_WEBSOCKET_CONNECTION_INFO";
    public static final String NATIVE_DATA_BASE_PATH = "BASE_PATH";
    public static final String NATIVE_DATA_MAX_FRAME_SIZE = "MAX_FRAME_SIZE";

    public static final BString CLIENT_URL_CONFIG = StringUtils.fromString("url");
    public static final BString SYNC_CLIENT_SERVICE_CONFIG = StringUtils.fromString("pingPongService");
    public static final BString CUSTOM_HEADERS = StringUtils.fromString("customHeaders");
    public static final String SYNC_CLIENT = "Client";

    public static final String CLIENT_LISTENER = "clientListener";
    public static final String CLIENT_CONNECTOR = "clientConnector";
    public static final String CALL_BACK_SERVICE = "callbackService";

    public static final BString CLIENT_ENDPOINT_CONFIG = StringUtils.fromString("config");
    public static final String CONSTRAINT_VALIDATION = "validation";
    public static final String CONNECTOR_FACTORY = "connectorFactory";
    public static final String FAILOVER_WEBSOCKET_CLIENT = "WebSocketFailoverClient";
    public static final BString ENDPOINT_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString CLIENT_HANDSHAKE_TIMEOUT = StringUtils.fromString("handShakeTimeout");
    public static final BString CLIENT_WRITE_TIMEOUT = StringUtils.fromString("writeTimeout");
    public static final MapType MAP_TYPE = TypeCreator.createMapType(
            TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING));

    public static final String PACKAGE = "ballerina";
    public static final String PROTOCOL_WEBSOCKET = "websocket";

    public static final BString COMPRESSION_ENABLED_CONFIG = StringUtils.fromString("webSocketCompressionEnabled");

    // WebSocketListener field names
    public static final String CONNECTION_ID_FIELD = "id";
    public static final String NEGOTIATED_SUBPROTOCOL = "negotiatedSubProtocol";
    public static final BString INITIALIZED_BY_SERVICE = StringUtils.fromString("initializedByService");
    public static final String IS_SECURE = "secure";
    public static final BString LISTENER_IS_OPEN_FIELD = StringUtils.fromString("open");

    // WebSocketClient struct field names
    public static final String HTTP_RESPONSE = "response";

    // WebSocketConnector
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

    public static final String BYTE_ARRAY = "byte[]";
    public static final String PARAM_TYPE_STRING = "string";
    public static final String PARAM_TYPE_BOOLEAN = "boolean";
    public static final String PARAM_TYPE_INT = "int";
    public static final String PARAM_TYPE_FLOAT = "float";
    public static final String PARAM_TYPE_DECIMAL = "decimal";
    public static final String PARAM_ANNOT_PREFIX = "$param$.";
    public static final String HEADER_ANNOTATION = COLON + ANN_NAME_HEADER;
    public static final String BALLERINA_HTTP_HEADER = ModuleUtils.getHttpPackageIdentifier() + COLON + ANN_NAME_HEADER;
    public static final String PATH_PARAM_IDENTIFIER = "^";
    public static final String STREAMING_NEXT_FUNCTION = "next";
    public static final String CONNECTION_CLOSED = "Connection closed";
    public static final String STATUS_CODE = "Status code:";

    // Close Frame Records
    public static final BString CLOSE_FRAME_TYPE = StringUtils.fromString("type");
    public static final BString CLOSE_FRAME_STATUS_CODE = StringUtils.fromString("status");
    public static final BString CLOSE_FRAME_REASON = StringUtils.fromString("reason");

    private WebSocketConstants() {
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
        AuthzError("AuthzError"),
        AuthnError("AuthnError"),
        PayloadValidationError("PayloadValidationError"),
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

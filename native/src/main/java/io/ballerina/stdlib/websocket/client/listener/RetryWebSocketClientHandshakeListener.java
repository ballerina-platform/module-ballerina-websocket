package io.ballerina.stdlib.websocket.client.listener;

import io.ballerina.runtime.api.Future;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.transport.contract.websocket.ClientHandshakeListener;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.message.HttpCarbonResponse;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import io.ballerina.stdlib.websocket.client.RetryContext;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The retry handshake listener for the client.
 *
 */
public class RetryWebSocketClientHandshakeListener implements ClientHandshakeListener {

    private final WebSocketService wsService;
    private final SyncClientConnectorListener connectorListener;
    private final BObject webSocketClient;
    private WebSocketConnectionInfo connectionInfo;
    private Future balFuture;
    private RetryContext retryConfig;
    private static final Logger logger = LoggerFactory.getLogger(RetryWebSocketClientHandshakeListener.class);

    public RetryWebSocketClientHandshakeListener(BObject webSocketClient, WebSocketService wsService,
                               SyncClientConnectorListener connectorListener, Future future, RetryContext retryConfig) {
        this.webSocketClient = webSocketClient;
        this.wsService = wsService;
        this.connectorListener = connectorListener;
        this.balFuture = future;
        this.retryConfig = retryConfig;
    }

    @Override
    public void onSuccess(WebSocketConnection webSocketConnection, HttpCarbonResponse carbonResponse) {
        webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(carbonResponse));
        WebSocketUtil.populatWebSocketEndpoint(webSocketConnection, webSocketClient);
        setWebSocketOpenConnectionInfo(webSocketConnection, webSocketClient, wsService);
        connectorListener.setConnectionInfo(connectionInfo);
        if (retryConfig.isFirstConnectionMadeSuccessfully()) {
            webSocketConnection.readNextFrame();
        } else {
            balFuture.complete(null);
        }
        WebSocketObservabilityUtil.observeConnection(connectionInfo);
        adjustContextOnSuccess(retryConfig);
    }

    @Override
    public void onError(Throwable throwable, HttpCarbonResponse response) {
        if (response != null) {
            webSocketClient.addNativeData(WebSocketConstants.HTTP_RESPONSE, HttpUtil.createResponseStruct(response));
        }
        setWebSocketOpenConnectionInfo(null, webSocketClient, wsService);
        if (throwable instanceof IOException && WebSocketUtil.reconnect(connectionInfo, balFuture)) {
            return;
        }
        balFuture.complete(WebSocketUtil.createErrorByType(throwable));
    }

    private void setWebSocketOpenConnectionInfo(WebSocketConnection webSocketConnection,
                                                BObject webSocketClient, WebSocketService wsService) {
        this.connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection, webSocketClient);
        webSocketClient.addNativeData(WebSocketConstants.NATIVE_DATA_WEBSOCKET_CONNECTION_INFO, connectionInfo);
    }

    /**
     * Sets the value into the `retryContext`.
     *
     * @param retryConfig - the retry context that represents a retry config
     */
    private void adjustContextOnSuccess(RetryContext retryConfig) {
        retryConfig.setFirstConnectionMadeSuccessfully();
        retryConfig.setReconnectAttempts(0);
    }
}

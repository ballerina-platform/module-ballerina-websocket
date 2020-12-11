## Module Overview

This module provides an implementation for connecting and interacting with WebSocket endpoints. The module facilitates two types of network entry points as ‘Client’ and ‘Listener’.

### WebSocket Module

This module also provides support for WebSockets. There are two types of WebSocket endpoints: `WebSocketClient` and `WebSocketListener`. Both endpoints support all WebSocket frames. The `WebSocketClient` has a callback service.

There are two types of services for WebSockets. The service of the server has the `WebSocketCaller` as the resource parameter and the callback service of the client has `WebSocketClient` as the resource parameter. The WebSocket services have a fixed set of resources that do not have a resource config. The incoming messages are passed to these resources.

**WebSocket upgrade**: During a WebSocket upgrade, the initial message received is an HTTP request. To intercept this request and perform the upgrade explicitly with custom headers, the user must create an HTTP resource with WebSocket-specific configurations as follows:

```ballerina
@http:ResourceConfig {
    webSocketUpgrade: {
        upgradePath: "/{name}",
        upgradeService: chatApp
    }
}
resource function upgrader(http:Caller caller, http:Request req, string name) {
}
```
The `upgradeService` is a server callback service.

**onOpen resource**: As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` resource is dispatched. This resource is only available in the service of the server.

**onText resource**: The received text messages are dispatched to this resource.

**onBinary resource**: The received binary messages are dispatched to this resource.

**onPing and onPong resources**: The received ping and pong messages are dispatched to these resources respectively.

**onIdleTimeout**: This resource is dispatched when the idle timeout is reached. The `idleTimeout` has to be configured either in the WebSocket service or the client configuration.

**onClose**: This resource is dispatched when a close frame with a statusCode and a reason is received.

**onError**: This resource is dispatched when an error occurs in the WebSocket connection. This will always be preceded by a connection closure with an appropriate close frame.

For more information, see the following.
* [WebSocket Basic Example](https://ballerina.io/swan-lake/learn/by-example/websocket-basic-sample.html)
* [HTTP to WebSocket Upgrade Example](https://ballerina.io/swan-lake/learn/by-example/http-to-websocket-upgrade.html)
* [WebSocket Chat Application](https://ballerina.io/swan-lake/learn/by-example/websocket-chat-application.html)
* [WebSocket Proxy Server](https://ballerina.io/swan-lake/learn/by-example/websocket-proxy-server.html)
* [Client Endpoint](https://ballerina.io/swan-lake/learn/by-example/websocket-client.html)   
* [Retry](https://ballerina.io/swan-lake/learn/by-example/websocket-retry.html)
* [Failover](https://ballerina.io/swan-lake/learn/by-example/websocket-failover.html)
* [Cookie](https://ballerina.io/swan-lake/learn/by-example/websocket-cookie.html)

## Module Overview

This module provides an implementation for connecting and interacting with WebSocket endpoints. The module facilitates three types of network entry points as ‘AsyncClient’, ‘SyncClient’ and ‘Listener’.

The `AsyncClient` has a callback service that can be registered at the initialization of the client. It has a fixed set of remote methods in this service and they get called on the receipt of messages from the server. The ‘SyncClient’ also can have a callback service registered to receive control frames like ping, pong and close.

In the server side, initial websocket service is there to handle upgrade requests. It has a single `onUpgrade` resource which takes in `http:Caller` and `http:Request` optionally. The `onUpgrade` resource returns a `websocket:Service` to which incoming messages gets dispatched after a successful websocket connection upgrade. This resource can be used to intercept the initial HTTP upgrade with custom headers or to cancel the websocket upgrade by returning an error.
The returning `websocket:Service` has a fixed set of remote methods.

**WebSocket upgrade**: During a WebSocket upgrade, the initial message received is an HTTP request. 

```ballerina
service /basePath on new websocket:Listener(21003) {
    resource function onUpgrade .(http:Caller caller, http:Request req) returns websocket:Service|websocket:UpgradeError {
        returns new WsService();
}
        
service class WsService {
  *websocket:Service;
  remote isolated function onText(websocket:Caller caller, string data) {
      checkpanic caller->pushText(data);
  }
}              
```
The `upgradeService` is a server callback service.

**onOpen resource**: As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` resource is dispatched. This resource is only available in the service of the server.

**onString resource**: The received text messages are dispatched to this resource. This resource is not applicable for `SyncClient`

**onBytes resource**: The received binary messages are dispatched to this resource. This resource is not applicable for `SyncClient`

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

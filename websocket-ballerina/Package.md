## Package Overview

This package provides an implementation for connecting and interacting with WebSocket endpoints. The package facilitates three types of network entry points as ‘Client’, ‘AsyncClient’, and ‘Listener’.

The `websocket:Client` reads text and binary messages synchronously. A callback service with the two `onPing` and `onPong` remote functions can be registered at the initialization of the client to receive `ping/pong` control frames.
The `websocket:AsyncClient`, which behaves asynchronously also has a callback `websocket:Service` that can be registered at the initialization of the client. It has a fixed set of remote methods in this service and they get called on the receipt of messages from the server. 

On the server-side, an initial WebSocket service is there to handle upgrade requests. It has a single `get` resource, which takes in an `http:Request` optionally. The `get` resource returns a `websocket:Service` to which incoming messages get dispatched after a successful WebSocket connection upgrade. This resource can be used to intercept the initial HTTP upgrade with custom headers or to cancel the WebSocket upgrade by returning an error.
The returning `websocket:Service` has a fixed set of remote methods.

**WebSocket upgrade**: During a WebSocket upgrade, the initial message received is an HTTP request. 

```ballerina
service /ws on new websocket:Listener(21003) {
    resource function get .(http:Request req) returns websocket:Service|websocket:UpgradeError {
        return new WsService();
}
        
service class WsService {
  *websocket:Service;
  remote isolated function onTextMessage(websocket:Caller caller, string data) returns websocket:Error? {
      check caller->writeTextMessage(data);
  }
}              
```

### Remote methods associated with `websocket:Service`

**onOpen**: As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` remote method is dispatched.

**onTextMessage**: The received text messages are dispatched to this remote method.

**onBinaryMessage**: The received binary messages are dispatched to this remote method.

**onPing and onPong**: The received ping and pong messages are dispatched to these remote methods respectively.

**onIdleTimeout**: This remote method is dispatched when the idle timeout is reached. The `idleTimeout` has to be configured either in the WebSocket service or the client configuration.

**onClose**: This remote method is dispatched when a close frame with a statusCode and a reason is received.

**onError**: This remote method is dispatched when an error occurs in the WebSocket connection. This will always be preceded by a connection closure with an appropriate close frame.

### Remote methods associated with `websocket:Client` service

**onPing**: The received `ping` messages are dispatched to this remote method.

**onPong**: The received `pong` messages are dispatched to this remote method.

For more information, see the following.
* [Server Example](https://ballerina.io/learn/by-example/websocket-basic-sample.html)
* [Client](https://ballerina.io/learn/by-example/websocket-text-client.html) 

# Specification: Ballerina WebSocket Library

_Owners_: @shafreenAnfar @bhashinee
_Reviewers_: @shafreenAnfar    
_Created_: 2021/11/30  
_Updated_: 2021/11/30  
_Issue_: [#2165](https://github.com/ballerina-platform/ballerina-standard-library/issues/2165)

# Introduction

This is the specification for WebSocket standard library which is used to implement WebSocket compliant 'listener' and 'client' using [Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the
cloud that makes it easier to use, combine, and create network services. 

# Contents
1. [Overview](#1-overview)
2. [Listener](#2-listener)
3. [Service Types](#3-service-types)
   * 3.1. [Upgrade Service](#31-upgrade-service)
   * 3.2. [WebSocket Service](#32-websocket-service)
4. [Client](#3-client)

## 1. Overview

WebSocket is a protocol that allows a long held full-duplex connection between a server and client. This specification elaborates on how Ballerina language provides a tested WebSocket client and server implementation that is compliant with the [RFC 6455](https://www.rfc-editor.org/rfc/rfc6455.html).

2. [Listener](#2-listener)

The WebSocket listener can be constructed with a port or an http:Listener. When initiating the listener it opens up the port and attaches the upgrade service which quite similar to an http service at the given service path. 

3. [Service Types](#3-service-types)

### 3.1 UpgradeService
Upgrade service is pretty much similar to a http service. It has a single `get` resource, which takes in a http:Request optionally. The `get` resource returns a websocket:Service to which incoming messages get dispatched after a successful WebSocket connection upgrade. This resource can be used to intercept the initial HTTP upgrade with custom headers or to cancel the WebSocket upgrade by returning an error.

### 3.1 Service
Once the WebSocket upgrade is accepted by the UpgradeService, it returns a ws:Service. This service has a fixed set of remote functions(onTextMessage, onBinaryMessage, onError, onOpen, onIdleTimeout, onClose, onPing, onPong) that do not have any configs. Receiving messages will get dispatched to the relevant remote function. 

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

#### Remote methods associated with websocket:Service

onOpen: As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` remote method is dispatched.
```ballerina
remote function onOpen(websocket:Caller caller) returns error? {
   io:println("Opened a WebSocket connection"`);
}
```

onTextMessage: The received text messages are dispatched to this remote method.
```ballerina
remote isolated function onTextMessage(websocket:Caller caller, string text) returns websocket:Error? {
    io:println("Text message: " + text);
}
```

onBinaryMessage: The received binary messages are dispatched to this remote method.
```ballerina
remote isolated function onBinaryMessage(websocket:Caller caller, byte[] data) returns websocket:Error? {
    io:println("Binary message: " + data);
}
```

onPing and onPong: The received ping and pong messages are dispatched to these remote methods respectively. You do not need to explicitly control these messages as they are handled automatically by the services and clients.
```ballerina
remote function onPing(websocket:Caller caller, byte[] data) returns error? {
    io:println(string `Ping received with data: ${data.toBase64()}`);
    check caller->pong(data);
}
 
remote function onPong(websocket:Caller caller, byte[] data) {
    io:println(string `Pong received with data: ${data.toBase64()}`);
}
```

onIdleTimeout: This remote method is dispatched when the idle timeout is reached. The idleTimeout has to be configured either in the WebSocket service or the client configuration.
```ballerina
remote function onIdleTimeout(websocket:Client caller) {
    io:println("Connection timed out");
}
```

onClose: This remote method is dispatched when a close frame with a statusCode and a reason is received.
```ballerina
remote function onClose(websocket:Caller caller, int statusCode, string reason) {
    io:println(string `Client closed connection with ${statusCode} because of ${reason}`);
}
```

onError: This remote method is dispatched when an error occurs in the WebSocket connection. This will always be preceded by a connection closure with an appropriate close frame.
```ballerina
remote function onError(websocket:Caller caller, error err) {
    io:println(err.message());
}
```

4. [Client](#4-client)

`websocket:Client` can be used to send and receive data synchronously over WebSocket connection. 

#### Send and receive messages using 'websocket:Client'

1. Send a text message.
```ballerina
   check wsClient->writeTextMessage("Text message");
```

2. Send a binary message
```ballerina
   check wsClient->writeBinaryMessage("Text message".toBytes());
```

3. Receive a text message
```ballerina
   string textResp = check wsClient->readTextMessage();
```

4. Receive a binary message
```ballerina
   byte[] textResp = check wsClient->readBinaryMessage();
```

5. Receive a message without prior knowledge of message type.
```ballerina
   byte[]|string|websocket:Error data = wsClient->readMessage();
   if (data is string) {
       io:println(data);
   } else if (data is byte[]) {
       io:println(data);
   } else {
       io:println("Error occurred", data.message());
   }
```

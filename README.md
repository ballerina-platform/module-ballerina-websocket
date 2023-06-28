Ballerina Websocket Library
===================

  [![Build](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/build-timestamped-master.yml)
  [![codecov](https://codecov.io/gh/ballerina-platform/module-ballerina-websocket/branch/main/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerina-websocket)
  [![Trivy](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/trivy-scan.yml)
  [![GraalVM Check](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerina-websocket/actions/workflows/build-with-bal-test-graalvm.yml)
  [![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-websocket.svg)](https://github.com/ballerina-platform/module-ballerina-websocket/commits/main)
  [![Github issues](https://img.shields.io/github/issues/ballerina-platform/ballerina-standard-library/module/websocket.svg?label=Open%20Issues)](https://github.com/ballerina-platform/ballerina-standard-library/labels/module%2Fwebsocket)

This library provides an implementation for connecting and interacting with WebSocket endpoints over the network.

### Client

The `websocket:Client` can be used to read/write text/binary messages synchronously.

A simple client code to handle text messages as follows.
```ballerina
import ballerina/websocket;

public function main() returns error? {
   websocket:Client wsClient = check new("ws://echo.websocket.org");

   check wsClient->writeTextMessage("Text message");

   string textResp = check wsClient->readTextMessage();
}
```
Similar to the above, this module has the `writeBinaryMessage` and `readBinaryMessage` functions to handle binary messages.
A callback service with the two `onPing` and `onPong` remote functions can be registered at the initialization of the client to receive the `ping/pong` control frames.
```ballerina
import ballerina/io;
import ballerina/websocket;

public function main() returns error? {
   websocket:Client wsClient = check new("ws://echo.websocket.org", pingPongHandler = new clientPingPongCallbackService());
   check wsClient->writeTextMessage("Text message");
}

service class clientPingPongCallbackService {
    *websocket:PingPongService;
    remote isolated function onPing(websocket:Caller caller, byte[] localData) returns byte[] {
        io:println("Ping message received");
        return localData;
    }

    remote isolated function onPong(websocket:Caller caller, byte[] localData) {
        io:println("Pong message received");
    }
}
```

### Listener

On the listener-side, an initial WebSocket upgrade service can be attached to the `websocket:Listener` to handle upgrade requests. It has a single `get` resource, which takes in an `http:Request` optionally. The `get` resource returns a `websocket:Service` to which incoming messages get dispatched after a successful WebSocket connection upgrade. This resource can be used to intercept the initial HTTP upgrade with custom headers or to cancel the WebSocket upgrade by returning an error.
The returning `websocket:Service` has a fixed set of remote methods.

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

#### Remote methods associated with `websocket:Service`

**onOpen**: As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` remote method is dispatched.

**onTextMessage**: The received text messages are dispatched to this remote method.

**onBinaryMessage**: The received binary messages are dispatched to this remote method.

**onPing and onPong**: The received ping and pong messages are dispatched to these remote methods respectively.

**onIdleTimeout**: This remote method is dispatched when the idle timeout is reached. The `idleTimeout` has to be configured either in the WebSocket service or the client configuration.

**onClose**: This remote method is dispatched when a close frame with a `statusCode` and a reason is received.

**onError**: This remote method is dispatched when an error occurs in the WebSocket connection. This will always be preceded by a connection closure with an appropriate close frame.

### Control messages

A WebSocket contains three types of control messages: `close`, `ping`, and `pong`. A WebSocket server or a client can send a `ping` message and the opposite side should respond with a corresponding `pong` message by returning the same payload sent with the `ping` message. These ping/pong sequences are used as a heartbeat mechanism to check if the connection is healthy.

You do not need to explicitly control these messages as they are handled automatically by the services and clients. However, if required, you can override the default implementations of the ping/pong messages by registering a `websocket:PingPongService` in the client side as given in the above client code sample and by including the `onPing` and `onPong` remote functions in the `websocket:Service` in the server side.

```ballerina
remote function onPing(websocket:Caller caller, byte[] data) returns error? {
    io:println(string `Ping received with data: ${data.toBase64()}`);
    check caller->pong(data);
}
 
remote function onPong(websocket:Caller caller, byte[] data) {
    io:println(string `Pong received with data: ${data.toBase64()}`);
}
```

A WebSocket server or a client can close the WebSocket connection by calling the `close` function. In the event of a connection closure, the service will be notified by invoking the `onClose` remote function. Also, on the client side, you will get a connection closure error if you try to read/write messages.

```ballerina
remote function onClose(websocket:Caller caller, int statusCode, string reason) {
    io:println(string `Client closed connection with ${statusCode} because of ${reason}`);
}
```

### WebSocket compression

Per message compression extensions are supported by the Ballerina `websocket` module and this is enabled by default for both the WebSocket client and the server. Compression can be enabled or disabled by setting the `webSocketCompressionEnabled` to `true` or `false` in the `ClientConfiguration` and `ListenerConfiguration`. Once the compression is successfully negotiated, receiving compressed messages will be automatically decompressed when reading.

### Origin considerations

The `Origin` header can be used to differentiate between WebSocket connections from different hosts or between those made from a browser and some other kind of network client. It is recommended to validate this `Origin` header before accepting the WebSocket upgrade.
```ballerina
import ballerina/http;
import ballerina/websocket;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .(http:Request httpRequest) returns websocket:Service|websocket:UpgradeError {
       string|error header = httpRequest.getHeader("Origin");
       if header is string {
           // Implement validateOrigin function to validate the origin header.
	       boolean validated = validateOrigin(header);
           if validated {
              return new WsService();
           }
       }
       return error("Invalid upgrade request");
   }
}
service class WsService {
    *websocket:Service;
    remote function onTextMessage(websocket:Caller caller, string text) {
        
    }
}
```

### Using the TLS protocol to secure WebSocket communication

It is strongly recommended to use the `wss://` protocol to protect against man-in-the-middle attacks. The Ballerina `websocket` module allows the use of TLS in communication to do this. This expects a secure socket to be set in the connection configuration as shown below.

#### Configuring TLS in server side

```ballerina
listener websocket:Listener wssListener = new (9090, {
    secureSocket: {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
});
service /basic/ws on wssListener {
    
}
```

#### Configuring TLS in client side

```ballerina
websocket:Client wssClient = new ("wss://echo.websocket.org", {
    secureSocket: {
        cert: "../resource/path/to/public.crt"
    }
});
```

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the module.

## Building from the Source

### Setting up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 17 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html)
   
   * [OpenJDK](https://adoptium.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
### Building the source

Execute the commands below to build from source.

1. To build the package:
   ```    
   ./gradlew clean build
   ```
2. To run the tests:
   ```
   ./gradlew clean test
   ```
   
3. To run a group of tests
   ```
   ./gradlew clean test -Pgroups=<test_group_names>
   ```
   
4. To build the without the tests:
   ```
   ./gradlew clean build -x test
   ```
   
5. To debug package implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```
   
6. To debug with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```

7. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```
8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```
   
## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`websocket` library](https://lib.ballerina.io/ballerina/websocket/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](performance/benchmarks/summary.md).

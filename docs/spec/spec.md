# Specification: Ballerina WebSocket Library

_Owners_: @shafreenAnfar @bhashinee  
_Reviewers_: @shafreenAnfar  
_Created_: 2021/12/09  
_Updated_: 2021/12/09  
_Issue_: [#2165](https://github.com/ballerina-platform/ballerina-standard-library/issues/2165)

# Introduction

This is the specification for WebSocket standard library which is used to implement WebSocket compliant `listener` and `client` using [Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the
cloud that makes it easier to use, combine, and create network services. 

# Contents
1. [Overview](#1-overview)
2. [Listener](#2-listener)
3. [Service Types](#3-service-types)
   * 3.1. [UpgradeService](#31-upgradeservice)
   * 3.2. [WebSocket Service](#32-websocket-service)
     * 3.2.1. [Remote methods associated with WebSocket Service](#321-remote-methods-associated-with-websocket-service)
       * [onOpen](#onopen)
       * [onTextMessage](#ontextmessage)
       * [onBinaryMessage](#onbinarymessage)
       * [onPing and onPong](#onping-and-onpong)
       * [onIdleTimeout](#onidletimeout)
       * [onClose](#onclose)
       * [onError](#onerror)
4. [Client](#4-client)
   * 4.1. [Send and receive messages using the Client](#41-send-and-receive-messages-using-the-client)
     * [writeTextMessage](#writetextmessage)
     * [writeBinaryMessage](#writebinarymessage)
     * [readTextMessage](#readtextmessage)
     * [readBinaryMessage](#readbinarymessage)
     * [readMessage](#readmessage)
     * [close](#close)
     * [ping](#ping)
     * [pong](#pong)
     * [onPing and onPong remote methods](#onping-and-onpong-remote-methods)
5. [Securing the WebSocket Connections](#5-securing-the-websocket-connections)
   * 5.1. [SSL/TLS](#51-ssl-tls)
   * 5.2. [Authentication and Authorization](#52-authentication-and-authorization)
6. [Samples](#6-samples)

## 1. [Overview](#1-overview)

WebSocket is a protocol that allows a long held full-duplex connection between a server and client. This specification elaborates on how Ballerina language provides a tested WebSocket client and server implementation that is compliant with the [RFC 6455](https://www.rfc-editor.org/rfc/rfc6455.html).

## 2. [Listener](#2-listener)

The WebSocket listener can be constructed with a port or an http:Listener. When initiating the listener it opens up the port and attaches the upgrade service at the given service path. It is also worth noting that upgrade service is quite similar to an HTTP service.

## 2.1. [Configurations](#21-configurations)

When initializing the listener, following configurations can be provided,
```ballerina
# Provides a set of configurations for HTTP service endpoints.
#
# + host - The host name/IP of the endpoint
# + http1Settings - Configurations related to HTTP/1.x protocol
# + secureSocket - The SSL configurations for the service endpoint. This needs to be configured in order to
#                  communicate through WSS.
# + timeout - Period of time in seconds that a connection waits for a read/write operation in the
#                     initial upgrade request. Use value 0 to disable timeout
# + server - The server name which should appear as a response header
# + webSocketCompressionEnabled - Enable support for compression in WebSocket
# + requestLimits - Configurations associated with inbound request size limits
public type ListenerConfiguration record {|
    string host = "0.0.0.0";
    ListenerHttp1Settings http1Settings = {};
    ListenerSecureSocket secureSocket?;
    decimal timeout = 120;
    string? server = ();
    boolean webSocketCompressionEnabled = true;
    RequestLimitConfigs requestLimits = {};
|};
```
## 3. [Service Types](#3-service-types)

### 3.1. [UpgradeService](#31-upgrade-service)

Upgrade service is pretty much similar to an HTTP service. It has a single `get` resource, which takes in an `http:Request` optionally. The `get` resource returns a `websocket:Service` to which incoming messages get dispatched after a successful WebSocket connection upgrade. This resource can be used to intercept the initial HTTP upgrade with custom headers or to cancel the WebSocket upgrade by returning an error.

### 3.2. [WebSocket Service](#32-websocket-service)

Once the WebSocket upgrade is accepted by the UpgradeService, it returns a `websocket:Service`. This service has a fixed set of remote functions that do not have any configs. Receiving messages will get dispatched to the relevant remote function. Each remote function is explained below.

```ballerina
service /ws on new websocket:Listener(21003) {
    resource function get .(http:Request req) returns websocket:Service|websocket:UpgradeError {
        return new WsService();
    }    
}
        
service class WsService {
    *websocket:Service;
    remote isolated function onTextMessage(websocket:Caller caller, string data) returns websocket:Error? {
        check caller->writeTextMessage(data);
    }
}              
```

#### 3.2.1. [Remote methods associated with WebSocket Service](#remote-methods-associated-with-websocket-service)

##### [onOpen](onopen)

As soon as the WebSocket handshake is completed and the connection is established, the `onOpen` remote method is dispatched.

```ballerina
remote function onOpen(websocket:Caller caller) returns error? {
    io:println("Opened a WebSocket connection"`);
}
```

##### [onTextMessage](#ontextmessage)

The received text messages are dispatched to this remote method.

```ballerina
remote isolated function onTextMessage(websocket:Caller caller, string text) returns websocket:Error? {
     io:println("Text message: " + text);
}
```

##### [onBinaryMessage](#onbinarymessage)

The received binary messages are dispatched to this remote method.

```ballerina
remote isolated function onBinaryMessage(websocket:Caller caller, byte[] data) returns websocket:Error? {
    io:println(data);
}
```

##### [onPing and onPong](#onping-and-onpong)

The received ping and pong messages are dispatched to these remote methods respectively. You do not need to explicitly control these messages as they are handled automatically by the services and clients.

```ballerina
remote function onPing(websocket:Caller caller, byte[] data) returns error? {
    io:println(string `Ping received with data: ${data.toBase64()}`);
    check caller->pong(data);
}
 
remote function onPong(websocket:Caller caller, byte[] data) {
    io:println(string `Pong received with data: ${data.toBase64()}`);
}
```

##### [onIdleTimeout](#onidletimeout)

This remote method is dispatched when the idle timeout is reached. The idleTimeout has to be configured either in the WebSocket service or the client configuration.

```ballerina
remote function onIdleTimeout(websocket:Client caller) {
    io:println("Connection timed out");
}
```

##### [onClose](#onclose)

This remote method is dispatched when a close frame with a statusCode and a reason is received.

```ballerina
remote function onClose(websocket:Caller caller, int statusCode, string reason) {
    io:println(string `Client closed connection with ${statusCode} because of ${reason}`);
}
```

##### [onError](#onerror)

This remote method is dispatched when an error occurs in the WebSocket connection. This will always be preceded by a connection closure with an appropriate close frame.

```ballerina
remote function onError(websocket:Caller caller, error err) {
    io:println(err.message());
}
```

## 4. [Client](#4-client)

`websocket:Client` can be used to send and receive data synchronously over WebSocket connection. The underlying implementation is non-blocking.

#### 4.1. [Send and receive messages using the Client](#41-send-and-receive-messages-using-the-client)

##### [writeTextMessage](#writetextmessage)

```ballerina
remote isolated function writeTextMessage(string data) returns Error? {}
```
`writeTextMessage` API can be used to send a text message. It takes in the message to be sent as a `string` and returns an error if an error occurs while sending the text message to the connection.

```ballerina
   check wsClient->writeTextMessage("Text message");
```

##### [writeBinaryMessage](#writebinarymessage)

```ballerina
remote isolated function writeBinaryMessage(byte[] data) returns Error? {}
```

`writeBinaryMessage` API can be used to send a binary message. It takes in the message to be sent as a `byte[]` and returns an error if an error occurs while sending the binary message to the connection.

```ballerina
   check wsClient->writeBinaryMessage("Text message".toBytes());
```

##### [readTextMessage](#readtextmessage)

```ballerina
remote isolated function readTextMessage() returns string|Error {}
```

`readTextMessage` API can be used to receive a text message. It returns the complete text message as a `string` or else an error if an error occurs while reading the messages.
```ballerina
   string textResp = check wsClient->readTextMessage();
```

##### [readBinaryMessage](#readbinarymessage)

```ballerina
remote isolated function readBinaryMessage() returns byte[]|Error {}
```

`readBinaryMessage` API can be used to receive a binary message. It returns the complete binary message as a `byte[]` or else an error if an error occurs while reading the messages.

```ballerina
   byte[] textResp = check wsClient->readBinaryMessage();
```

##### [readMessage](#readmessage)

```ballerina
remote isolated function readMessage() returns string|byte[]|Error {}
```

`readMessage` API can be used to receive a message without prior knowledge of message type. It returns a `string` if a text message is received, `byte[]` if a binary message is received or else an error if an error occurs while reading the messages.

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

##### [close](#close)

```ballerina
remote isolated function close(int? statusCode = 1000, string? reason = (), decimal timeout = 60) returns Error? {}
```

`close` API can be used to close the connection. It takes in the optional parameters `statusCode` for closing the connection, `reason` for closing the connection if there is any and the `timeout` to wait until a close frame is received from the remote endpoint.

```ballerina
   check wsClient->close();
```

##### [ping](#ping)

```ballerina
remote isolated function ping(byte[] data) returns Error? {}
```

`ping` API can be used to send ping messages. It takes in the message to be sent as a `byte[]` and returns an error if an error occurs while sending the ping message to the connection.

```ballerina
   check wsClient->ping([5, 24, 56, 243]);
```

##### [pong](#pong)

```ballerina
remote isolated function pong(byte[] data) returns Error? {}
```

`pong` API can be used to send pong messages. It takes in the message to be sent as a `byte[]` and returns an error if an error occurs while sending the pong message to the connection.

```ballerina
   check wsClient->pong([5, 24, 56, 243]);
```

##### [onPing and onPong remote methods](#onping-and-onpong-remote-methods)

To receive ping/pong messages, users have to register a `websocket:PingPongService` when creating the client. If the service is registered, receiving ping/pong messages will get dispatched to the `onPing` and `onPong` remote functions respectively.
```ballerina
   service class PingPongService {
       *websocket:PingPongService;
       remote function onPong(websocket:Caller wsEp, byte[] data) {
           io:println("Pong received", data);
       }
       
       remote isolated function onPing(websocket:Caller caller, byte[] localData) returns byte[] {
           return localData;
       }    
       
   }
   
   websocket:Client wsClient = check new ("ws://localhost:21020", {pingPongHandler : new PingPongService()});
```
If the user has implemented `onPing` on their service, it's user's responsibility to send the `pong` frame. It can be done simply by returning the data from the remote function, or else can be done using the `pong` API of websocket:Caller. If the user hasn't implemented the `onPing` remote function, `pong` will be sent automatically.

## 5. [Securing the WebSocket Connections](#5-securing-the-websocket-connections)

Ballerina provides inbuilt support for SSL/TLS and configurations to enforce authentication and authorization such as Basic Auth, JWT auth, and OAuth2.

### 5.1. [SSL/TLS](#51-ssl-tls)
You can configure a secure socket for your WebSocket listener and client to upgrade to a TCP connection with TLS.

The TLS-enabled Listener

```ballerina
listener websocket:Listener wsListener = new(9090,
    secureSocket = {
        key: {
             certFile: "/path/to/public.crt",
             keyFile: "/path/to/private.key"
        }
    }
);
```

The TLS-enabled Client

```ballerina
websocket:Client wsClient = check new (string `wss://localhost:9090/taxi/${username}`,
    secureSocket = {
        cert: "/path/to/public.crt"
    }
);
```

### 5.2. [Authentication and Authorization](#52-authentication-and-authorization)

#### Listener

The Ballerina WebSocket library provides built-in support for the following listener authentication mechanisms that are validated in the initial upgrade request.
1. Basic authentication
2. JWT authentication
3. OAuth2 authentication

To enable one of the above, you should configure the `auth` field in `websocket:ServiceConfig` annotation which consists of the following records:
1. FileUserStoreConfigWithScopes
2. LdapUserStoreConfigWithScopes
3. JwtValidatorConfigWithScopes
4. OAuth2IntrospectionConfigWithScopes

Each of the above records consists of configurations specific to each type as `FileUserStoreConfig` , `LdapUserStoreConfig` ,`JwtValidatorConfig` and `OAuth2IntrospectionConfig` respectively. You just have to configure them and there will be no need for any extensions or handlers. Ballerina will perform the required validation for you.

```ballerina

listener websocket:Listener wsListener = new(9090,
    secureSocket = {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
);

@websocket:ServiceConfig {
     auth: [
        {
            oauth2IntrospectionConfig: {
                url: "https://localhost:9445/oauth2/introspect",
                tokenTypeHint: "access_token",
                scopeKey: "scp",
                clientConfig: {
                    secureSocket: {
                        cert: "../resource/path/to/introspect/service/public.crt"
                    }
                }
            },
            scopes: ["write", "update"]
        }
    ]
}
service /ws on wsListener {
    resource function get .() returns websocket:Service|websocket:UpgradeError {
        // ....
    }
}
```

#### Client

The Ballerina WebSocket client can be configured to send authentication information to the endpoint being invoked. The Ballerina WebSocket library also has built-in support for the following client authentication mechanisms.
1. Basic authentication
2. JWT authentication
3. OAuth2 authentication

The following code snippet shows how a WebSocket client can be configured to call a secured endpoint. The `auth` field of the client configurations (websocket:ClientConfiguration) should have either one of the `CredentialsConfig`, `BearerTokenConfig`, `JwtIssuerConfig`, `OAuth2ClientCredentialsGrantConfig`, `OAuth2PasswordGrantConfig`, and `OAuth2RefreshTokenGrantConfig` records. Once this is configured, Ballerina will take care of the rest of the validation process.

```ballerina
websocket:Client wsClient = check new (string `wss://localhost:9090/taxi/${username}`,
     auth = {
         tokenUrl: "https://localhost:9445/oauth2/token",
         username: "johndoe",
         password: "A3ddj3w",
         clientId: "3MVG9YDQS5WtC11paU2WcQjBB3L5w4gz52uriT8ksZ3nUVjKvrfQMrU4uvZohTftxStwNEW4cfStBEGRxRL68",
         clientSecret: "9205371918321623741",
         scopes: ["write", "update"],
         clientConfig: {
             secureSocket: {
                 cert: "../resource/path/to/introspect/service/public.crt"
             }
         }
     },
     secureSocket = {
         cert: "../resource/path/to/public.crt"
     }
);
```

## 6. [Samples](#6-samples)

Listener

```ballerina
import ballerina/io;
import ballerina/websocket;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .() returns websocket:Service|websocket:Error {
       return new WsService();
   }
}

service class WsService {
    *websocket:Service;
    remote isolated function onTextMessage(websocket:Caller caller, string text) returns websocket:Error? {
        io:println("Text message: " + text);
        check caller->writeTextMessage(text);
    }
    
    remote isolated function onBinaryMessage(websocket:Caller caller, byte[] data) returns websocket:Error? {
        io:println(data);
        check caller->writeBinaryMessage(data);
    }
}
```

Client

```ballerina
import ballerina/io;
import ballerina/websocket;

public function main() returns error? {
   websocket:Client wsClient = check new("ws://localhost:9090/basic/ws");

   check wsClient->writeTextMessage("Text message");

   string textResp = check wsClient->readTextMessage();
   io:println(textResp);
   
   check wsClient->writeBinaryMessage("Binary message".toBytes());

   byte[] byteResp = check wsClient->readBinaryMessage();
   string stringResp = check 'string:fromBytes(byteResp);
   io:println(stringResp);
}
```

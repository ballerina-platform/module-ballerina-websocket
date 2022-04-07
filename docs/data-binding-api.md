## Summary

Data binding helps to access the incoming and outgoing text data in the user's desired parameter type. Subtypes of `anydata` will be the supported parameter types. This proposal discusses ways to provide data binding for both on listener side as well as the client side.

## Goals

- Improve user experience by adding data binding support for WebSocket service, Client, and the Caller.

## Motivation

As of now, the Ballerina WebSocket package doesn’t provide direct data binding for sending and receiving data. Only `string` and `byte[]` are the supported data types. Therefore, users have to do data manipulations by themselves. With this new feature, user experience can be improved by introducing data binding to reduce the burden of developers converting string data to the desired format as discussed in the next section.

## Description

Data binding support will be added to the text messages as the serialization is text-based and will be supported in both the client-side and the listener side. Subtypes of `anydata` are the data types to be supported.

### Listener

On the listener side, the function signature will be updated to accept the above-mentioned parameter types. The user can state the required data type as a parameter in the remote function signature. So the received data will be converted to the requested type and dispatched to the relevant remote function. WebSocket services have two remote functions to accept incoming data(`onTextMessage` and `onBinaryMessage`). As mentioned above data binding is supported only for text messages, `onTextMessage` signature will be updated as follows,

Ex:

**onTextMessage**

```ballerina
service class WsService { 
    *websocket:Service;
    remote isolated function onTextMessage(xml data) returns websocket:Error? { 
    } 
}
```

If the data binding fails, the connection will get terminated by sending a close frame with the 1003 error code(1003 indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept ) and will print an error log at the listener side.

### Client

#### Send data

The WebSocket client has `writeTextMessage`  API to write text data to the connection. As of now `writeTextMessage` API only accepts `string` as a parameter.

This API will be extended to accept  `anydata` types as well.

**writeTextMessage**

```ballerina
remote isolated function writeTextMessage(anydata data) returns Error?
```
Whatever the data type given as the input parameter will be converted to a `string` internally and sent as text data.

If the data binding fails, a `websocket:Error` will be returned from the API.

#### Receive data

To receive data, the WebSocket client has `readTextMessage`, `readBinaryMessage` and `readMessage`. `readMessage` API was introduced because of a limitation for a scenario like a reverse proxy where the client does not have prior knowledge of the data. And it is a rarely used API.  As mentioned earlier, we support binding for sub-protocols of text messages and this API supports both text and binary data types, data binding support will be added only to `readTextMessage` API.

The contextually-expected data type is inferred from the LHS variable type. Allowed data types would be subtypes of `anydata`.

Ex:
```ballerina
string result = check readTextMessage();
json data = check readTextMessage();
```

If the data binding fails, a `websocket:Error` will be returned from the API.

### Caller

Caller APIs will also be extended similar to the Client's `writeTextMessage` API. That behaves the same as the Client's `writeTextMessage` API.

**writeTextMessage**

```ballerina
remote isolated function writeTextMessage(anydata data) returns Error?
```

If the data binding fails, a `websocket:Error` will be returned from the API.

## Testing

- Testing compiler plugin validation to accept new data types.
- Testing the data type conversions on the client-side and server-side.

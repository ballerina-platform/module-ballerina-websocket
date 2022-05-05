# Proposal: Implement Data Binding Support

_Owners_: @shafreenAnfar @bhashinee  
_Reviewers_: @shafreenAnfar    
_Created_: 2022/04/12  
_Updated_: 2022/04/12  
_Issue_: [#2761](https://github.com/ballerina-platform/ballerina-standard-library/issues/2761)  

## Summary

Data binding helps to access the incoming and outgoing text data in the user's desired parameter type. Subtypes of `anydata` will be the supported parameter types. This proposal discusses ways to provide data binding for both on listener side as well as the client side.

## Goals

- Improve user experience by adding data binding support for WebSocket service, Client, and the Caller.

## Motivation

As of now, the Ballerina WebSocket package doesn’t provide direct data binding for sending and receiving data. Only `string` and `byte[]` are the supported data types. Therefore, users have to do data manipulations by themselves. With this new feature, the user experience can be improved by introducing data binding to reduce the burden of developers converting data to the desired format as discussed in the next section.

## Description

Data binding support will be added to both the text and binary messages and will be supported on both the client-side and the listener side. Subtypes of `anydata` are the data types to be supported.

### Listener

On the listener side, the function signature will be updated to accept the above-mentioned parameter types. The user can state the required data type as a parameter in the remote function signature. So the received data will be converted to the requested type and dispatched to the relevant remote function. WebSocket services have three remote functions to accept incoming data(`onTextMessage`, `onBinaryMessage`, and the newly introduced `onMessage`). To support data binding, these APIs will be updated as follows to accept `anydata` as function parameters.

Ex:

**onTextMessage**

```ballerina
service class WsService { 
    *websocket:Service;
    remote isolated function onTextMessage(xml data) returns websocket:Error? { 
    } 
}
```

Here the incoming text data will be deserialized into the expected data type.

Data deserialization happens similar to the following,

- If the contextually-expected data type is `string`, received data will be directly presented to the API without doing any deserialization.
- If the contextually-expected data type is `xml`, received text data will be deserialized to `xml`.
- All the other data types are treated as `json` and received text data will be deserialized to `json`.

**onBinaryMessage**

```ballerina
service class WsService { 
    *websocket:Service;
    remote isolated function onBinaryMessage(json data) returns websocket:Error? { 
    } 
}
```

Here the incoming binary data other than the `byte[]` will first be deserialized into the string representation of the `byte[]` and then be converted to the expected data type.

Data deserialization happens similar to the following,

- If the contextually-expected data type is `byte[]`, received data will be directly presented to the API without doing any deserialization.
- If the contextually-expected data type is `xml`, received binary data will be first converted to the string representation of the `byte[]` and then deserialized to `xml`.
- All the other data types are treated as `json` and received binary data will be first converted to the string representation of the `byte[]` and then will be deserialized to `json`.

**onMessage**

```ballerina
service class WsService { 
    *websocket:Service;
    remote isolated function onMessage(json data) returns websocket:Error? { 
    } 
}
```

The newly introduced `onMessage` remote function accepts both types of text and binary data frames. Similar to the above two APIs this will also accept `anydata` as the function parameter. Incoming text data will be deserialized into the requested data type similar to what happens on the `onTextMessage` and incoming binary data will first get deserialized into the string representation of the binary data and then to the requested data type if data binding is expected. (See the above)

If the data binding fails, the connection will get terminated by sending a close frame with the 1003 error code(1003 indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept ) and will print an error log at the listener side.

### Client

#### Send data

The WebSocket client has `writeTextMessage`, `writeBinaryMessage`, and newly introduced `writeMessage`  APIs to write data to the connection.

These APIs will be extended to accept  `anydata` types as well.

**writeTextMessage**

```ballerina
remote isolated function writeTextMessage(anydata data) returns Error?
```
Whatever the data type given as the input parameter will be converted to a `string` internally and sent as text data.

Data serialization happens as follows,
- If a `string` is given, it is directly sent to the connection without any data conversions.
- If an `xml` is given, it is directly converted to a `string` using the Ballerina `toString` function.
- Rest of the data types will be converted to `json` and then to the `string` representation of that using the Ballerina `toJsonString` function.

If the data binding fails, a `websocket:Error` will be returned from the API.

**writeBinaryMessage**

```ballerina
remote isolated function writeBinaryMessage(anydata data) returns Error?
```
Whatever the data type given as the input parameter will be converted to a `byte[]` internally and sent as binary data.

Data serialization happens as follows,
- If a `byte[]` is given, it is directly sent to the connection without any data conversions.
- If an `xml` is given, it is first converted to a `string` using the Ballerina `toString` function and then to a `byte[]` using `toBytes()`.
- Rest of the data types will be converted to a json string using the Ballerina `toJsonString` function and then to `byte[]` using `toBytes()`.

If the data binding fails, a `websocket:Error` will be returned from the API.

**writeMessage**

```ballerina
remote isolated function writeMessage(anydata data) returns Error?
```

Data serialization happens as follows,

- If a `string` is given as the input, it is directly sent to the connection as text data without any data conversions.
- If a `byte[]` is given, it is directly sent to the connection as binary data without any data conversions.
- If an `xml` is given, it is directly converted to a `string` using the Ballerina `toString` function and sent as text data.
- Rest of the data types will be converted to `json` and then to the `string` representation of that using the Ballerina `toJsonString` function and sent as text data.

If the data binding fails, a `websocket:Error` will be returned from the API.

#### Receive data

To receive data, the WebSocket client has `readTextMessage`, `readBinaryMessage`, and `readMessage`. Data binding support will be introduced for both text messages and binary messages.

The contextually-expected data type is inferred from the LHS variable type. Allowed data types would be subtypes of `anydata`.

**readTextMessage**

Ex:
```ballerina
string result = check readTextMessage();
json data = check readTextMessage();
```

If the data binding fails, a `websocket:Error` will be returned from the API.

**readBinaryMessage**

Ex:
```ballerina
string result = check readBinaryMessage();
json data = check readBinaryMessage();
```
If the LHS type is some other data type apart from `byte[]`, incoming data will first be converted to the string representation of the binary data and then be converted to the expected data type.

If the data binding fails, a `websocket:Error` will be returned from the API.

**readMessage**

Ex:
```ballerina
string result = check readMessage();
json data = check readMessage();
```

When the receiving data are of the text frame type, data will be deserialized to the expected data type. Similar to the `readBinaryMessage`, if the incoming data is of binary frames, and if the LHS type is some other data type apart from `byte[]`, Incoming data will first be converted to the string representation of the binary data and then be converted to the expected data type.

Deserialization of the above APIs happens similar to the `onTextMessage`, `onBinaryMessage` and `onMessage` deserialization. For more details refer to the relevant section.

If the data binding fails, a `websocket:Error` will be returned from the API.

### Caller

Caller APIs will also be extended similar to the Client's `writeTextMessage`, `writeBinaryMessage` and `writeMessage` APIs.

## Testing

- Testing compiler plugin validation to accept new data types.
- Testing the data type conversions on the client-side and server-side.

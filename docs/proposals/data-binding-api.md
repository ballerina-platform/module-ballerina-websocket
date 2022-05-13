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

On the listener side, a new remote function `onMessage` is introduced with data binding support. The user can state the required data type as a parameter in the remote function signature. So the received data will be converted to the requested type and dispatched to the remote function.

Ex:
**onMessage**

```ballerina
service class WsService { 
    *websocket:Service;
    remote isolated function onMessage(json data) returns websocket:Error? { 
    } 
}
```
The newly introduced `onMessage` remote function accepts both types of text and binary data frames. This function accepts `anydata` as the function parameter

Data deserialization for text messages happens similar to the following,

- If the contextually-expected data type is `string`, received data will be directly presented to the API without doing any deserialization.
- If the contextually-expected data type is `xml`, received text data will be deserialized to `xml`.
- All the other data types are treated as `json` and received text data will be deserialized to `json`.

Data deserialization for binary messages happens similar to the following,

- If the contextually-expected data type is `byte[]`, received data will be directly presented to the API without doing any deserialization.
- If the contextually-expected data type is `xml`, received binary data will be first converted to the string representation of the `byte[]` and then deserialized to `xml`.
- All the other data types are treated as `json` and received binary data will be first converted to the string representation of the `byte[]` and then will be deserialized to `json`.

If the data binding fails, the connection will get terminated by sending a close frame with the 1003 error code(1003 indicates that an endpoint is terminating the connection because it has received a type of data it cannot accept ) and will print an error log at the listener side.

### Client

#### Send data

A newly introduced `writeMessage` API with data binding support to write data to the connection.

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

To receive data, the existing `readMessage` API will be extended to support `anydata`. Data binding support will be introduced for both text messages and binary messages.

The contextually-expected data type is inferred from the LHS variable type. Allowed data types would be subtypes of `anydata`.

**readMessage**

Ex:
```ballerina
string result = check readMessage();
json data = check readMessage();
```

When the receiving data is of the text frame type, data will be deserialized to the expected data type. If the incoming data is of binary frames, and if the LHS type is some other data type apart from `byte[]`, incoming data will first be converted to the string representation of the binary data and then be converted to the expected data type.

Deserialization of the above `readMessage` API happens similar to the `onMessage` deserialization. For more details refer to the relevant section.

If the data binding fails, a `websocket:Error` will be returned from the API.

### Caller

Caller will also be extended similar to the Client's `writeMessage` API.

## Testing

- Testing compiler plugin validation to accept new data types.
- Testing the data type conversions on the client-side and server-side.

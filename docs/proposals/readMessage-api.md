# Proposal: Implement readMessage API for WebSocket client

_Owners_: @shafreenAnfar @bhashinee  
_Reviewers_: @shafreenAnfar    
_Created_: 2021/11/01  
_Updated_: 2021/11/01  
_Issue_: [#1180](https://github.com/ballerina-platform/ballerina-standard-library/issues/1180)

## Summary
Introduce a Client API to handle data where the type(text or binary) is unknown in advance of receiving the message.

## Goals
* Allow the client to handle messages without prior knowledge of message type.

## Motivation
As per the current design, we have two client APIs to read data from the WebSocket connection. Those are `readTextMessage` and `readBinaryMessage`. So to work with these two APIs, the client needs to know the data type(whether it is text or binary) that it receives beforehand. So for example, in a scenario like a reverse proxy where the client does not have prior knowledge of the data, this becomes a limitation.

To write a message to the WebSocket connection, the client has two APIs as `writeTextMessage` and `writeBinaryMessage`. When the user writes data to the connection user is aware of the data type user wants to write. So they can use one of the writing APIs. So there is no particular need of introducing a `writeMessage` API in that case.

So this has some limitations when it comes to reading data where the type can be unknown in advance upon receiving the data.

## Description
As mentioned in the Goals section the purpose of this proposal is to introduce an API for the client to handle data in situations where the data type is unknown.

The key functionality expected from this API is to be able to read any type (string or byte[]) of data received from the server-side.

Following is an example for `wsClient->readMessage()` API with proposed solution:
```ballerina
websocket:Client wsClient = check new("ws://localhost:9090/chat");
byte[]|string|websocket:Error data = wsClient->readMessage();
if (data is string) {
    io:println(data);
} else if (data is byte[]) {
    io:println(data);
} else {
    io:println("Error occurred", data.message());
}
```

### API Additions
- Following new remote method will be introduced to `websocket:Client` declaration:
```ballerina
# Reads data from the WebSocket connection
#
# + return - A `string` if a text message is received, `byte[]` if a binary message is received or a `websocket:Error`
#            if an error occurs when receiving
remote isolated function readMessage() returns string|byte[]|Error {
 // implementation goes here 
}
```

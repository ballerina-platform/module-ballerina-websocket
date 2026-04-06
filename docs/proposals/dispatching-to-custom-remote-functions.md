# Proposal: Dispatching to custom remote functions based on the message type

_Owners_: @shafreenAnfar @bhashinee  
_Reviewers_: @shafreenAnfar    
_Created_: 2023/02/06  
_Updated_: 2023/02/06  
_Issue_: [#3670](https://github.com/ballerina-platform/ballerina-standard-library/issues/3670)  

## Summary
Dispatching messages to custom remote functions based on the message type(declared by a field in the received message) with the end goal of generating meaningful Async APIs.

## Goals
Generating meaningful AsyncAPIs and improving the readability of the code.

## Motivation

With AsyncAPI gaining its's popularity with increased usage of event-driven microservices it is worthwhile to think of ways to generate AsyncAPI specifications using WebSocket service code. The motivation is to improve the service code to be more understandable to retrieve the maximum details to generate meaningful AsyncAPIs and to improve the readability of the code.

## Description

In most real-world use cases, the WebSocket protocol will be used along with a sub-protocol. Most of the time those sub-protocols are differentiated from a dedicated field in the message and it contains the type of the message. For example: In [Kraken API](https://www.asyncapi.com/blog/websocket-part1) the type of the message is identified by the `event` field.

```
{"event": "ping"}
{"event": "subscribe",  "pair": [    "XBT/USD",    "XBT/EUR"  ],  "subscription": {    "name": "ticker"  }}
{"event": "heartbeat"}
```

Another example is [`GraphQL over WebSocket Protocol`](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md)

The WebSocket sub-protocol for the above specification is: `graphql-transport-ws`. And the type of the message can be identified by the value of the field named `type` of the message.

```
{"type": "ping"}
{"type": "subscribe", "id":"1", "payload":{"query": "{ __schema { types { name } } }"}}
```

As of now, when using the Ballerina WebSocket service, all these messages are dispatched to the generic `onMessage` remote function. When the user writes a logic based on the received message, all have to be handled inside the `onMessage` using an if/else ladder or similar. This reduces the readability of the code.

And also, if we want to generate an AsyncAPI specification by referring to the service code, it is not possible to capture all the details like the response message for a particular type of message.

Ex:

Following is a part of the Kraken AsyncAPI specification describing the types of messages and their responses.

```
  messages:
    ping:
      summary: Ping server to determine whether connection is alive
      description: Client can ping server to determine whether connection is alive, server responds with pong. This is an application level ping as opposed to default ping in websockets standard which is server initiated
      payload:
        $ref: '#/components/schemas/ping'
      x-response:
        $ref: '#/components/messages/pong'
        
    unsubscribe:
      description: Unsubscribe, can specify a channelID or multiple currency pairs.
      payload:
        $ref: '#/components/schemas/subscribe'
      examples:
        - payload:
            event: unsubscribe
            pair:
              - XBT/EUR
              - XBT/USD
            subscription:
              name: ticker
        - payload:
            event: unsubscribe
            subscription:
              name: ownTrades
              token: WW91ciBhdXRoZW50aWNhdGlvbiB0b2tlbiBnb2VzIGhlcmUu
      x-response:
        $ref: '#/components/messages/subscriptionStatus'    
```

In the above AsyncAPI specification, it has the messages given as `ping` and `unsubscribe`. Their response messages are given by the field `x-response`.

If this part is written using existing WebSocket service functionalities, it would look like the following.

```ballerina
service class MyService {
    *websocket:Service;

    remote function onMessage(websocket:Caller caller, Ping|UnSubscribe message) returns Pong|SubscriptionStatus {
        if message is Ping {
            return {'type: WS_PONG};
        } else {
            return {'type: WS_UNSUBSCRIBE, id: "5"};
        }
    }
}
```

Therefore, if we have all the messages dispatched to a single `onMessage` remote function, it is difficult to differentiate the response for `ping` message and the response message for `unsubscribe` operation.

As a solution for this, the idea is to have custom remote functions based on the message type within the WebSocket service. For example, if the message is `{"type": "ping"}` it will get dispatched to `onPing` remote function. Similarly,

Message | Remote function
-- | --
{"event": "ping"} | onPing
{"event": "subscribe",  "pair": [    "XBT/USD",    "XBT/EUR"  ],  "subscription": {    "name": "ticker"  }} | onSubscribe
{"event": "heartbeat"} | onHeartbeat

**Dispatching rules**

1. The user can configure the field name(key) to identify the messages and the allowed values as message types.

The `dispatcher` is used to identify the event type of the incoming message by its value. The default value is `'type`.

Ex:
incoming message = ` {"event": "ping"}`
dispatcherKey = "event"
event/message type = "ping"
dispatching to remote function = "onPing"

```ballerina
@websocket:ServiceConfig {
    dispatcherKey: "event"
}
service / on new websocket:Listener(9090) {}
```

2. Naming of the remote function.

- If there are spaces and underscores between message types, those will be removed and made camel case("un subscribe" -> "onUnSubscribe").
- The 'on' word is added as the predecessor and the remote function name is in the camel case("ping" -> "onPing").

3. If an unmatching message type receives where a matching remote function is not implemented in the WebSocket service, it gets dispatched to the default `onMessage` remote function if it is implemented. Or else it will get ignored.

**An example code for Kraken API with the proposed changes.**

```ballerina
@websocket:ServiceConfig {
    dispatcherKey: "'type"
}
service / on new websocket:Listener(9090) {
    resource function get .() returns websocket:Service {
        return new MyService();
    }
}

service class MyService {
    *websocket:Service;

    remote function onPing(Ping message) returns Pong {
        return {'type: WS_PONG};
    }

    remote function onSubscribe(Subscribe message) returns SubscriptionStatus {
        return {id: "4", 'type: WS_SUBSCRIBE};
    }

    remote function onHeartbeat(Hearbeat message) {
        io:println(message);
    }
}
```

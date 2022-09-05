# Real-time crypto market data updates - A simulation of Kraken WebSocket API using Ballerina WebSocket service

## Overview

[Kraken WebSocket API](https://docs.kraken.com/websockets/) is an API offering real-time crypto market data updates. This example is to simulate how the same functionality can be implemented using Ballerina WebSocket service and the client.

## Implementation

### The WebSocket Server
This is implemented as per the Kraken WebSocket Async API. You can find more details about the used Async API from this [link](https://www.asyncapi.com/blog/websocket-part1) and also you can find it by navigating into the `examples/kraken-api/async-api` folder.
The server will accept the subscribe requests and serve the updates to the client until they send `unsubscribe` requests.

### Users - WebSocket Clients
Once the client is connected to the server, server will respond with the status of the server. If the status is `online` client then send a subscription request to the server. Then the server will send the response to the `subscription` request by saying whether the user is successfully subscribed to the channel. If it is a failure server will send the response with the reasons in an error message. If the subscription is successful the client will start reading the updates coming from the server.

## Run the Example

First, clone this repository, and then run the following commands to run this example in your local machine.

```sh
// Run the WebSocket server
$ cd examples/kraken-api/server
$ bal run
```

In another terminal, run the client as follows.
```sh
// Run the WebSocket client
$ cd examples/kraken-api/client
$ bal run
```
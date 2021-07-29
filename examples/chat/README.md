# Chat Application

## Overview

This application shows how to use the Ballerina WebSocket package to implement a simple chat application.

## Implementation

### 1. The WebSocket Server
The WebSocket Server is in charge of registering users to the chat application and broadcasting the messages received by the users.

### 2. Users - WebSocket Clients
Clients can register for the chat applicaton by sending requests to the WebSocket server. When the application starts it will request for a username. Once a non-empty username is entered, the user will get registered.

After getting registered, the users can send messages to the chat group by typing messages on the console and pressing `Enter`. Then the server will broadcast messages by looping over the registered clients.
If a user wants to exit from the chat, he/she can type in `exit` and `Enter`, so the connection will get closed, and the client gets unregistered from the chat.

Client will have one Ballerina strand writing the messages to the WebSocket connection and one for reading. 

## Run the Example

First, clone this repository, and then run the following commands to run this example in your local machine.

```sh
// Run the WebSocket server
$ cd examples/chat/server
$ bal run
```

In another terminal, run the client as follows.
```sh
// Run the route guide client
$ cd examples/chat/client
$ bal run
```
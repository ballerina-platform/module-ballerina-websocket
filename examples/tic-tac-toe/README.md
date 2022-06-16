# Tic Tac Toe - A multiplayer game

## Overview

This application shows how to use the Ballerina WebSocket package to implement a multiplayer game. In this example, we have chosen to implement the famous Tic Tac Toe which is a game in which two players take turns in drawing either an 'O' or an 'X' in one square of a grid consisting of nine squares. 
It is developed in a way that two remote users can play the game.

## Implementation

The user who initially joins the game is given the sign `X` and the second user is given the sign `0`. More than two players are not allowed to play. The first player who gets the three symbols in a row will be the winner.

### The WebSocket Server
The WebSocket server is in charge of registering users to the game, handling the moves taken by the individual user and determining the winner while sending the updates to the UI. Here, we have used a simple React based UI.

### Users - WebSocket Clients
The UI of this game is a React Application which shows the square containing 9 grids. Users can click on the desired square to mark their response. Once there is a winner it will be notified to both users.

## Run the Example

First, clone this repository, and then run the following commands to run this example in your local machine.

```sh
// Run the WebSocket server
$ cd examples/tic-tac-toe/server
$ bal run
```

To run the Webapp

1. Make sure you have a recent version of [Node.js](https://nodejs.org/en/) installed.
2. Navigate to the folder `examples/tic-tac-toe/web-app`
3. Install the required node modules using the command `npm install`
4. Run the application using `npm start`
5. Running the application will open a Web page with the grid. You can click on the squares play the game.


import ballerina/websocket;

service /foo on new websocket:Listener(9090) {}

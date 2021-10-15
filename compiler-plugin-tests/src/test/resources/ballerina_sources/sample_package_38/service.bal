import ballerina/websocket;

service /foo on new websocket:Listener(9090) returns websocket:Service|error {
    return new WsService();
}

import ballerina/websocket;

service /foo on new websocket:Listener(9090) {
    int x = 5;
    string y = "xx";
}

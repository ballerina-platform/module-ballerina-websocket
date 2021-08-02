import ballerina/test;
import ballerina/log;
import ballerina/websocket;

@test:Config {}
function testText() returns websocket:Error? {
    websocket:Client wsClient = check new("ws://localhost:9090/chat/Alice");
    websocket:Error? result = wsClient->writeTextMessage("Hey");
    if (result is websocket:Error) {
        log:printError("Error occurred when writing text", 'error = result);
    }
    string serviceReply = check wsClient->readTextMessage();
    test:assertEquals(serviceReply, "Hi Alice! You have successfully connected to the chat");
    websocket:Error? err = wsClient->close(statusCode = 1000, timeout = 10);
}
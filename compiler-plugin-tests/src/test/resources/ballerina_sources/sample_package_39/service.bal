import ballerina/http;
import ballerina/websocket;
import ballerina/io;

listener websocket:Listener securedEP = new(9090);

service /foo on securedEP {
    resource function get bar(http:Request req) returns websocket:Service|websocket:AuthError {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;
    remote isolated function onBinaryMessage(websocket:Caller caller,
                                 byte[] text) returns websocket:Error? {
        io:println(text);
        return caller->writeBinaryMessage(text);
    }

    remote function onPing(websocket:Caller caller, byte[] data) returns error? {
        io:println(string `Ping received with data: ${data.toBase64()}`);
        check caller->pong(data);
    }

    remote function onPong(websocket:Caller caller, byte[] data) {
        io:println(string `Pong received with data: ${data.toBase64()}`);
    }
}

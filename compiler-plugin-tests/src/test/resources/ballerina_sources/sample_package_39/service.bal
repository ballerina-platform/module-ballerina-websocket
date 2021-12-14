import ballerina/http;
import ballerina/websocket;

listener websocket:Listener securedEP = new(9090);

service /foo on securedEP {
    resource function get bar(http:Request req) returns websocket:Service|websocket:AuthError {
        return new WsService();
    }
}

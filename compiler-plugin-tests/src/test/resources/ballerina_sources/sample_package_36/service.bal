import ballerina/http;
import ballerina/websocket;

listener websocket:Listener securedEP = new(9090,
   secureSocket = {
       key: {
           certFile: "../resources/public.crt",
           keyFile: "../resources/private.key"
       }
   }
);

service /foo on securedEP {
    resource function get bar(http:Request req) returns websocket:Service|websocket:AuthError {
        return new WsService();
    }
}

service isolated class WsService {
    *websocket:Service;
    remote function onTextMessage(websocket:Caller caller, string text) returns websocket:Error? {
        return caller->writeTextMessage(text);
    }
}

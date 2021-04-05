import ballerina/websocket;
import ballerina/http;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .(http:Request req) returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}
service class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(string text) returns byte[]? {
    }
}

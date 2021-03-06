import ballerina/websocket;
import ballerina/http;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .(http:Caller caller) returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}
service isolated class WsService {
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(websocket:Caller caller, string text) {
    }
}

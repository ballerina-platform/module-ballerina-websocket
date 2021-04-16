import ballerina/websocket;
import ballerina/http;

listener http:Listener hl = check new(21001);
listener websocket:Listener socketListener = new(<@untainted> hl);

service /basic/ws on socketListener {
   remote isolated function get() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }
}

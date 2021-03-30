import ballerina/websocket;
import ballerina/http;

listener http:Listener hl = check new(21001);

service /basic/ws on hl {
   remote isolated function get() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }
}

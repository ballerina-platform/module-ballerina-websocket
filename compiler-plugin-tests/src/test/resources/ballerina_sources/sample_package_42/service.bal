import ballerina/websocket;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;

    remote function onTextMessage(xml data) {
    }
}

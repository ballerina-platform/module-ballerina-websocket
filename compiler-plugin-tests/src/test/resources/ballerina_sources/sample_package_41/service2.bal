import ballerina/websocket;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService1();
   }
}

service isolated class WsService1 {
    *websocket:Service;

    remote function onTextMessage(websocket:Caller caller, json data) {
    }
}

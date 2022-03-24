import ballerina/websocket;

public type Coord record {
    int x;
    int y;
};

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService2();
   }
}

service isolated class WsService2 {
    *websocket:Service;

    remote function onTextMessage(Coord data) {
    }
}

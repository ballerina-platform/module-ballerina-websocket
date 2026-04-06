import ballerina/websocket;

service /basic/ws on new websocket:Listener(9090) {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;

    remote function onBinaryMessage(readonly & websocket:Caller caller, readonly & byte[] data) {
    }

    remote function onPing(websocket:Caller caller, readonly & byte[] data) {
    }

    remote function onPong(websocket:Caller caller, readonly & byte[] data) {
    }
}

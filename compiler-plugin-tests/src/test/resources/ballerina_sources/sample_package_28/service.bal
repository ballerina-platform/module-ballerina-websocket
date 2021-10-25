import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    remote function onOpen(websocket:Caller caller, string xyz) returns websocket:Error? {
        return ();
    }

    remote function onBinaryMessage(websocket:Caller caller, byte[] data) returns byte[]? {
         return "hello".toBytes();
    }
}

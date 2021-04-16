import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service class WsService {
    *websocket:Service;

    remote function onBinaryMessage(websocket:Caller caller, int status) returns byte[]|string|int? {
         return "hello".toBytes();
    }
}

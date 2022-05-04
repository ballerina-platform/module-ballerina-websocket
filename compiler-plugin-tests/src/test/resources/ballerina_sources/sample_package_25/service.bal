import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws1 on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;

    remote function onBinaryMessage(json data) returns json {
         return data;
    }
}

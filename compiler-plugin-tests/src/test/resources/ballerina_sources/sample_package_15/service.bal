import ballerina/websocket as ws;

listener ws:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns ws:Service|ws:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *ws:Service;
    remote function onOpen(ws:Client caller) {
    }

    remote function onClose(ws:Client caller, string message, int status) returns string {
          return "hello";
    }
}

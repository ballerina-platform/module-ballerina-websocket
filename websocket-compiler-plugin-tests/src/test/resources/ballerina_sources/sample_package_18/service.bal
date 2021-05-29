import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) returns string {
        return "Hello";
    }

    remote function onClose(websocket:Caller caller, string message, int status, int status2) {
    }
}

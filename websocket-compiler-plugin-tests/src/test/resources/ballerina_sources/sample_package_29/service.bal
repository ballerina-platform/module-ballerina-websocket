import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service class WsService {
    remote function onOpen(websocket:Caller caller) returns int {
        return 5;
    }

    remote function onClose(websocket:Caller caller, string message, int status) {
    }

    remote function onError(error err) returns error? {
    }
}

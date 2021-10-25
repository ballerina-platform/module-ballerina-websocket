import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    remote function onError(websocket:Caller caller) returns websocket:Error? {
        return ();
    }

    remote function onIdleTimeout(websocket:Client caller) returns string {
        return "text";
    }

    remote function onTextMessage(websocket:Caller caller, int status) returns websocket:Error? {
        return ();
    }
}

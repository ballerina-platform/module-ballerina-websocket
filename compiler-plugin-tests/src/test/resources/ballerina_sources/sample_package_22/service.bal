import ballerina/websocket;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;
    remote function onError(websocket:Caller caller, string 'err) returns error? {
        return ();
    }

    remote function onIdleTimeout(websocket:Caller caller, string text) {
    }
}

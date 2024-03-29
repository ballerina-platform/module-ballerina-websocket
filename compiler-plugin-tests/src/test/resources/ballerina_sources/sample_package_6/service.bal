import ballerina/websocket;
import ballerina/http;

listener http:Listener hl = check new(21001);
listener websocket:Listener socketListener = new(<@untainted> hl);

service /basic/ws on socketListener {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(websocket:Caller caller, string text) returns error? {
        return ();
    }
}

service /helloWorld on hl {
    resource function get hello(http:Caller caller, http:Request req) returns error? {
        return caller->respond("Hello World!");
    }
}

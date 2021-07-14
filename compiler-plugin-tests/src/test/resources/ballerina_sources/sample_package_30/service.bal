import ballerina/websocket;
import ballerina/io;

listener websocket:Listener hl = check new(21001);

service /basic/ws on hl {
   resource isolated function get abc() returns websocket:Service|websocket:UpgradeError {
       lock {
           self.testFunc();
           return new WsService();
       }
   }

   isolated function testFunc() {
       io:println("Invoked the function");
   }
}

service isolated class WsService {
    remote function onClose(websocket:Caller caller, string message, int status, int status2) {
    }

    remote function onIdleTimeout() {
    }

    remote function onError(websocket:Caller caller, websocket:Error err) returns string {
        return "error";
    }
}

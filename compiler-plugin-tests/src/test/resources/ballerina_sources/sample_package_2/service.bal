import ballerina/websocket;
import ballerina/http;
import ballerina/io;

websocket:ListenerConfiguration conf = {
               secureSocket: {
                        key: {
                            path: "tests/certsAndKeys/ballerinaKeystore.p12",
                            password: "ballerina"
                        }
                    }
               };

@websocket:ServiceConfig {
    subProtocols: ["xml", "json"],
    idleTimeout: 120
}
service /basic/ws on new websocket:Listener(9090, conf) {
   resource function get [string name](http:Request req) returns
                            websocket:Service|websocket:UpgradeError {
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
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(string text) returns byte[]? {
    }
}

import ballerina/websocket;
import ballerina/io;

service /basic/ws on new websocket:Listener(9090, {
                 secureSocket: {
                          key: {
                              path: "tests/certsAndKeys/ballerinaKeystore.p12",
                              password: "ballerina"
                          }
                      }
                 }) {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       return new WsService();
   }
}
service class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(websocket:Caller caller, string text) returns byte[] {
        self.testFunc();
        return "hello".toBytes();
    }

    isolated function testFunc() {
        io:println("Invoked the function");
    }
}

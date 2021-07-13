import ballerina/websocket;
import ballerina/http;
import ballerina/io;

http:ListenerConfiguration httpConf = {
               secureSocket: {
                        key: {
                            path: "tests/certsAndKeys/ballerinaKeystore.p12",
                            password: "ballerina"
                        }
                    }
               };

listener http:Listener hl = new(21001, httpConf);

websocket:ListenerConfiguration conf = {
               secureSocket: {
                        key: {
                            path: "tests/certsAndKeys/ballerinaKeystore.p12",
                            password: "ballerina"
                        }
                    }
               };
listener websocket:Listener socketListener = new(hl, conf);

service /basic/ws on socketListener {
   resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
       io:println("hello");
       return new WsService();
   }
}

service isolated class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) {
    }

    remote function onTextMessage(websocket:Caller caller, string text) returns error? {
    }
}

service /helloWorld on hl {
    resource function get hello(http:Caller caller, http:Request req) returns error? {
        check caller->respond("Hello World!");
    }
}

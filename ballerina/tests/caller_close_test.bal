import ballerina/test;
import ballerina/lang.runtime;

string closeResult = "";
string close2Result = "";

service /basic/ws on new Listener(9090) {
   resource function get .() returns Service|Error {
       return new WsService();
   }
}

service class WsService {
    *Service;
    remote function onMessage(Caller caller, json text) returns Error? {
        Error? close = caller->close(timeout = 3);
        if close is Error {
           closeResult = close.message();
        } else {
           closeResult = "success";
        }
        Error? close2 = caller->close(timeout = 3);
        if close2 is Error {
           close2Result = close2.message();
        } else {
           close2Result = "success";
        }
    }
}

@test:Config {}
public function testCallerClose() returns Error? {
   Client wsClient = check new("ws://localhost:9090/basic/ws");
   check wsClient->writeMessage({"Text": "message"});
   json|Error resp = wsClient->readMessage();
   if resp is json {
       test:assertFail("Expected a connection closure error");
   }
   runtime:sleep(8);
   test:assertEquals(closeResult, "success");
   test:assertEquals(close2Result, "ConnectionClosureError: Close frame already sent. Cannot send close frame again.");
}
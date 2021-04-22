import ballerina/websocket;
import ballerina/http;

websocket:ListenerConfiguration conf = {
               secureSocket: {
                        key: {
                            path: "tests/certsAndKeys/ballerinaKeystore.p12",
                            password: "ballerina"
                        }
                    }
               };
service /basic/ws on new websocket:Listener(9090, conf){
   resource function get [string name](http:Request req) {
   }
}

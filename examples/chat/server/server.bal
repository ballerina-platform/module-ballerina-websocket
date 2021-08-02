import ballerina/http;
import ballerina/io;
import ballerina/websocket;

final string NAME = "name";
string nameValue = "";
map<websocket:Caller> connectionsMap = {};

service /chat on new websocket:Listener(9090) {
    resource function get [string name](http:Request req) returns websocket:Service|websocket:UpgradeError {
        nameValue = name;
        if (nameValue != "") {
            // Server can accept a WebSocket connection by returning a `websocket:Service`.
            return service object websocket:Service {
                remote function onOpen(websocket:Caller caller) returns websocket:Error? {
                    string welcomeMsg = "Hi " + nameValue + "! You have successfully connected to the chat";
                    websocket:Error? err = check caller->writeTextMessage(welcomeMsg);
                    string msg = nameValue + " connected to chat";
                    broadcast(msg);
                    caller.setAttribute(NAME, nameValue);
                    lock {
                        connectionsMap[caller.getConnectionId()] = caller;
                    }
                }
                remote function onTextMessage(websocket:Caller caller, string text) {
                    string msg = getAttributeStr(caller, NAME) + ": " + text;
                    io:println(msg);
                    @strand {
                        thread:"any"
                    }
                    worker broadcast returns error? {
                        broadcast(msg);
                    }
                }
                remote function onClose(websocket:Caller caller, int statusCode, string reason) {
                    lock {
                        _ = connectionsMap.remove(caller.getConnectionId());
                    }
                    string msg = getAttributeStr(caller, NAME) + " left the chat";
                    broadcast(msg);
                }
            };
        } else {
            // Server can cancel the WebSocket upgrade by
            websocket:UpgradeError err = error("Username must be a non-empty value");
            return err;
        }
    }
}

// Function to perform the broadcasting of text messages.
function broadcast(string text) {
    foreach websocket:Caller con in connectionsMap {
        websocket:Error? err = con->writeTextMessage(text);
        if err is websocket:Error {
            io:println("Error sending message to the :" + getAttributeStr(con, NAME) +
                        ". Reason: " + err.message());
        }
    }
}

isolated function getAttributeStr(websocket:Caller ep, string key) returns (string) {
    var name = ep.getAttribute(key);
    if name is string {
        return name;
    }
    return "";
}

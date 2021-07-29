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
            return service object websocket:Service {
                remote function onOpen(websocket:Caller caller) {
                    string welcomeMsg = "Hi " + nameValue + "! You have successfully connected to the chat";
                    websocket:Error? err = caller->writeTextMessage(welcomeMsg);
                    if (err is websocket:Error) {
                        io:println("Error sending message:" + err.message());
                    }
                    string msg = nameValue + " connected to chat";
                    broadcast(msg);
                    caller.setAttribute(NAME, nameValue);
                    connectionsMap[caller.getConnectionId()] = caller;
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
            websocket:UpgradeError err = error("Username must be a non-empty value");
            return err;
        }
    }
}

// Function to perform the broadcasting of text messages.
function broadcast(string text) {
    lock {
        foreach var con in connectionsMap {
            var err = con->writeTextMessage(text);
            if (err is websocket:Error) {
                io:println("Error sending message to the :" + getAttributeStr(con, NAME) +
                            ". Reason: " + err.message());
            }
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

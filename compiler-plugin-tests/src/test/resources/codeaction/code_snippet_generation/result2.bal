import ballerina/websocket;

service /foo on new websocket:Listener(9090) {
    int x = 5;
    string y = "xx";
	resource function get .() returns websocket:Service|websocket:Error {
		return new WsService();
	}
}

service class WsService {
	*websocket:Service;

	remote function onMessage(websocket:Caller caller, anydata data) returns websocket:Error? {
	}
}

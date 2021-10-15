import ballerina/websocket;

service /foo on new websocket:Listener(9090) {
	resource function get .() returns websocket:Service|websocket:Error {
		return new WsService();
	}
}

service class WsService {
	*websocket:Service;

	remote isolated function onTextMessage(websocket:Caller caller, string text) returns websocket:Error? {
	}
}

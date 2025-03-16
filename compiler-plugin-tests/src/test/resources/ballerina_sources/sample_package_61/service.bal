import ballerina/websocket;

listener websocket:Listener hl = check new (21001);

service /ws1 on hl {
    resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService1();
    }
}

service /ws2 on hl {
    resource isolated function get .() returns websocket:Service|websocket:UpgradeError {
        return new WsService2();
    }
}

service isolated class WsService1 {
    *websocket:Service;

    remote function onError(error err) returns websocket:CloseFrame {
        return websocket:NORMAL_CLOSURE;
    }
}

service isolated class WsService2 {
    *websocket:Service;

    remote function onError(error err) returns error|websocket:NormalClosure {
        return websocket:NORMAL_CLOSURE;
    }
}

//// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
////
//// WSO2 Inc. licenses this file to you under the Apache License,
//// Version 2.0 (the "License"); you may not use this file except
//// in compliance with the License.
//// You may obtain a copy of the License at
////
//// http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing,
//// software distributed under the License is distributed on an
//// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//// KIND, either express or implied.  See the License for the
//// specific language governing permissions and limitations
//// under the License.
//
//import ballerina/log;
//import ballerina/runtime;
//import ballerina/test;
////import ballerina/websocket;
//
//string proxyData = "";
//
//@WebSocketServiceConfig {
//}
//service on new Listener(21018) {
//
//    resource function onOpen(WebSocketCaller wsEp) {
//        WebSocketClient wsClientEp = new ("ws://localhost:21019/websocket", {
//                callbackService:
//                    clientCallbackService9,
//                readyOnConnect: false
//            });
//        var returnVal = wsClientEp->ready();
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//    resource function onText(WebSocketCaller wsEp, string text) {
//        var returnVal = wsEp->pushText(text);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//    resource function onBinary(WebSocketCaller wsEp, byte[] data) {
//        var returnVal = wsEp->pushBinary(data);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//    resource function onClose(WebSocketCaller wsEp, int statusCode, string reason) {
//        var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//}
//
//@WebSocketServiceConfig {
//    path: "/websocket"
//}
//service proxyServer on new Listener(21019) {
//
//    resource function onOpen(WebSocketCaller caller) {
//        log:printInfo("The Connection ID: " + caller.getConnectionId());
//    }
//
//    resource function onText(WebSocketCaller caller, string text, boolean finalFrame) {
//        var err = caller->pushText(text, finalFrame);
//        if (err is WebSocketError) {
//            log:printError("Error occurred when sending text message", err);
//        }
//    }
//
//    resource function onBinary(WebSocketCaller caller, byte[] data) {
//        var returnVal = caller->pushBinary(data);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//}
//
//service clientCallbackService9 = @WebSocketServiceConfig {} service {
//    resource function onText(WebSocketClient wsEp, string text) {
//        //http:WebSocketCaller serviceEp = getAssociatedListener(wsEp);
//        var returnVal = wsEp->pushText(text);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//    resource function onBinary(WebSocketClient wsEp, byte[] data) {
//        var returnVal = wsEp->pushBinary(data);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//
//    resource function onClose(WebSocketClient wsEp, int statusCode, string reason) {
//        var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//};
//
//service proxyCallbackService = @WebSocketServiceConfig {} service {
//    resource function onText(WebSocketClient wsEp, string text) {
//        proxyData = <@untainted>text;
//    }
//
//    resource function onBinary(WebSocketClient wsEp, byte[] data) {
//        expectedBinaryData = <@untainted>data;
//    }
//
//    resource function onClose(WebSocketClient wsEp, int statusCode, string reason) {
//        var returnVal = wsEp->close(statusCode = statusCode, reason = reason, timeoutInSeconds = 0);
//        if (returnVal is WebSocketError) {
//            panic <error>returnVal;
//        }
//    }
//};
//
//// Tests sending and receiving of text frames in WebSockets.
//@test:Config {}
//public function testSendText() {
//    WebSocketClient wsClient = new ("ws://localhost:21018", {callbackService: proxyCallbackService});
//    checkpanic wsClient->pushText("Hi kalai");
//    runtime:sleep(500);
//    test:assertEquals(proxyData, "Hi kalai", msg = "Data mismatched");
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
//    //if (result is WebSocketError) {
//    //   log:printError("Error occurred when closing connection", result);
//    //}
//}
//
//// Tests sending and receiving of binary frames in WebSocket.
//@test:Config {}
//public function testSendBinary() {
//    WebSocketClient wsClient = new ("ws://localhost:21018", {callbackService: proxyCallbackService});
//    byte[] binaryData = [5, 24, 56, 243];
//    checkpanic wsClient->pushBinary(binaryData);
//    runtime:sleep(500);
//    test:assertEquals(expectedBinaryData, binaryData, msg = "Data mismatched");
//    error? result = wsClient->close(statusCode = 1000, reason = "Close the connection", timeoutInSeconds = 0);
//    //if (result is WebSocketError) {
//    //   log:printError("Error occurred when closing connection", result);
//    //}
//}

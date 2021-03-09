// // Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/jballerina.java;
// import ballerina/http;

// # A WebSocket client endpoint, which provides failover support for multiple WebSocket targets.
// public client class WebSocketFailoverClient {

//     private string id = "";
//     private string? negotiatedSubProtocol = ();
//     private boolean secure = false;
//     private boolean open = false;
//     private http:Response? response = ();
//     private map<any> attributes = {};
//     private string url = "";

//     private WebSocketConnector conn = new;
//     private WebSocketFailoverClientConfiguration config = {};

//     # Initializes the   failover client, which provides failover capabilities to a WebSocket client endpoint.
//     #
//     # + config - The `WebSocketFailoverClientConfiguration` of the endpoint
//     public isolated function init(WebSocketFailoverClientConfiguration config) {
//         self.url = config.targetUrls[0];
//         //addCookies(config);
//         self.config = config;
//         return externFailoverInit(self);
//     }

//     # Pushes text to the connection. If an error occurs while sending the text message to the connection, that message
//     # will be lost.
//     #
//     # + data - Data to be sent. If it is a byte[], it is converted to a UTF-8 string for sending
//     # + return  - An `error` if an error occurs when sending
//     remote isolated function writeTextMessage(string data) returns Error? {
//         return self.conn.writeTextMessage(data);
//     }

//     # Pushes binary data to the connection. If an error occurs while sending the binary message to the connection,
//     # that message will be lost.
//     #
//     # + data - Binary data to be sent
//     # + return  - An `error` if an error occurs when sending
//     remote isolated function writeBinaryMessage(byte[] data) returns Error? {
//         return self.conn.writeBinaryMessage(data);
//     }

//     # Pings the connection. If an error occurs while sending the ping frame to the connection, that frame will be lost.
//     #
//     # + data - Binary data to be sent
//     # + return  - An `error` if an error occurs when sending
//     remote isolated function ping(byte[] data) returns Error? {
//         return self.conn.ping(data);
//     }

//     # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
//     # frame will be lost.
//     #
//     # + data - Binary data to be sent
//     # + return  - An `error` if an error occurs when sending
//     remote isolated function pong(byte[] data) returns Error? {
//         return self.conn.pong(data);
//     }

//     # Closes the connection.
//     #
//     # + statusCode - Status code for closing the connection
//     # + reason - Reason for closing the connection
//     # + timeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint before closing the
//     #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
//     #                   is not received from the remote endpoint. If the value is < 0 (e.g., -1), then the connection
//     #                   waits until a close frame is received. If the WebSocket frame is received from the remote
//     #                   endpoint within the waiting period, the connection is terminated immediately.
//     # + return - An `error` if an error occurs while closing the webSocket connection
//     remote isolated function close(int? statusCode = 1000, string? reason = (),
//     int timeout = 60) returns Error? {
//         return self.conn.close(statusCode, reason, timeout);
//     }

//     # Sets a connection-related attribute.
//     #
//     # + key - The key to identify the attribute
//     # + value - The value of the attribute
//     public isolated function setAttribute(string key, any value) {
//         self.attributes[key] = value;
//     }

//     # Gets connection-related attributes if any.
//     #
//     # + key - The key to identify the attribute
//     # + return - The attribute related to the given key or `nil`
//     public isolated function getAttribute(string key) returns any {
//         return self.attributes[key];
//     }

//     # Removes connection-related attributes if any.
//     #
//     # + key - The key to identify the attribute
//     # + return - The attribute related to the given key or `nil`
//     public isolated function removeAttribute(string key) returns any {
//         return self.attributes.remove(key);
//     }

//     # Gives the connection ID associated with this connection.
//     #
//     # + return - The unique ID associated with the connection
//     public isolated function getConnectionId() returns string {
//         return self.id;
//     }

//     # Gives the subprotocol if any that is negotiated with the client.
//     #
//     # + return - Returns the subprotocol if any that is negotiated with the client or `nil`
//     public isolated function getNegotiatedSubProtocol() returns string? {
//         return self.negotiatedSubProtocol;
//     }

//     # Gives the secured status of the connection.
//     #
//     # + return - Returns `true` if the connection is secure
//     public isolated function isSecure() returns boolean {
//         return self.secure;
//     }

//     # Gives the open or closed status of the connection.
//     #
//     # + return - Returns `true` if the connection is open
//     public isolated function isOpen() returns boolean {
//         return self.open;
//     }

//     # Gives any HTTP response of     the client handshake request if received.
//     #
//     # + return - Returns the HTTP response received for the client handshake request
//     public isolated function getHttpResponse() returns http:Response? {
//         return self.response;
//     }
// }

// # Configurations for the WebSocket client endpoint.
// #
// # |                                                                              |
// # |:---------------------------------------------------------------------------- |
// # | callbackService - Copied from CommonClientConfiguration             |
// # | subProtocols - Copied from CommonClientConfiguration                |
// # | customHeaders - Copied from CommonClientConfiguration               |
// # | idleTimeout - Copied from CommonClientConfiguration        |
// # | secureSocket - Copied from CommonClientConfiguration                |
// # | maxFrameSize - Copied from CommonClientConfiguration                |
// # | webSocketCompressionEnabled - Copied from CommonClientConfiguration |
// # | handShake - Copied from CommonClientConfiguration   |
// # | cookieConfig - Copied from CommonClientConfiguration                |
// # + targetUrls - The set of URLs, which are used to connect to the server
// # + failoverIntervalInMillis - The maximum number of milliseconds to delay a failover attempt
// public type WebSocketFailoverClientConfiguration record {|
//     *CommonClientConfiguration;
//     string[] targetUrls = [];
//     int failoverIntervalInMillis = 1000;
// |};

// isolated function externFailoverInit(WebSocketFailoverClient wsClient) = @java:Method {
//     'class: "org.ballerinalang.net.websocket.client.FailoverInitEndpoint",
//     name: "initEndpoint"
// } external;

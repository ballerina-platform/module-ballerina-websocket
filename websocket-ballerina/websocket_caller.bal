// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

# Represents a WebSocket caller.
public client class Caller {

    private string id = "";
    private string? negotiatedSubProtocol = ();
    private boolean secure = false;
    private boolean open = false;
    private map<any> attributes = {};
    private boolean initializedByService = false;

    private WebSocketConnector conn = new;

    isolated function init() {
        // package private function to prevent object creation
    }

    # Pushes text to the connection. If an error occurs while sending the text message to the connection, that message
    # will be lost.
    #
    # + data - Data to be sent.
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeTextMessage(string data) returns Error? {
        return self.conn.writeTextMessage(data);
    }

    # Pushes binary data to the connection. If an error occurs while sending the binary message to the connection,
    # that message will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function writeBinaryMessage(byte[] data) returns Error? {
        return self.conn.writeBinaryMessage(data);
    }

    # Pings the connection. If an error occurs while sending the ping frame to the server, that frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function ping(byte[] data) returns Error? {
        return self.conn.ping(data);
    }

    # Sends a pong message to the connection. If an error occurs while sending the pong frame to the connection, that
    # frame will be lost.
    #
    # + data - Binary data to be sent
    # + return  - An `error` if an error occurs when sending
    remote isolated function pong(byte[] data) returns Error? {
        return self.conn.pong(data);
    }

    # Closes the connection.
    #
    # + statusCode - Status code for closing the connection
    # + reason - Reason for closing the connection
    # + timeout - Time to wait (in seconds) for the close frame to be received from the remote endpoint before closing the
    #                   connection. If the timeout exceeds, then the connection is terminated even though a close frame
    #                   is not received from the remote endpoint. If the value < 0 (e.g., -1), then the connection waits
    #                   until a close frame is received. If WebSocket frame is received from the remote endpoint
    #                   within the waiting period, the connection is terminated immediately.
    # + return - An `error` if an error occurs when sending
    remote isolated function close(int? statusCode = 1000, string? reason = (),
        decimal timeout = 60) returns Error? {
        return self.conn.close(statusCode, reason, timeout);
    }

    # Sets a connection related attribute.
    #
    # + key - The key, which identifies the attribute
    # + value - The value of the attribute
    public isolated function setAttribute(string key, any value) {
        self.attributes[key] = value;
    }

    # Gets connection related attribute if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function getAttribute(string key) returns any {
        return self.attributes[key];
    }

    # Removes connection related attribute if any.
    #
    # + key - The key to identify the attribute
    # + return - The attribute related to the given key or `nil`
    public isolated function removeAttribute(string key) returns any {
        return self.attributes.remove(key);
    }

    # Gives the connection id associated with this connection.
    #
    # + return - The unique ID associated with the connection
    public isolated function getConnectionId() returns string {
        return self.id;
    }

    # Gives the subprotocol if any that is negotiated with the client.
    #
    # + return - The subprotocol if any negotiated with the client or `nil`
    public isolated function getNegotiatedSubProtocol() returns string? {
        return self.negotiatedSubProtocol;
    }

    # Gives the secured status of the connection.
    #
    # + return - `true` if the connection is secure
    public isolated function isSecure() returns boolean {
        return self.secure;
    }

    # Gives the open or closed status of the connection.
    #
    # + return - `true` if the connection is open
    public isolated function isOpen() returns boolean {
        return self.open;
    }
}

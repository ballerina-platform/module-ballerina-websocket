// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type Error distinct error;

# Raised during failures in connection closure
public type ConnectionClosureError distinct Error;

# Raised during the handshake when the WebSocket upgrade fails
public type InvalidHandshakeError distinct Error;

# Raised when receiving a frame with a payload exceeding the maximum size
public type PayloadTooBigError distinct Error;

# Raised when the other side breaks the protocol
public type ProtocolError distinct Error;

# Raised during connection failures
public type ConnectionError distinct Error;

# Raised when an out of order/invalid continuation frame is received
public type InvalidContinuationFrameError distinct Error;

# Raised for errors not captured by the specific errors
public type GenericError distinct Error;

# Raised when the websocket upgrade is not accepted
public type UpgradeError distinct Error;

# Raised when the client creation fails
public type GenericClientError distinct Error;

# Raised when the initial WebSocket handshake timed out
public type HandshakeTimedOut distinct Error;

# Raised when the client creation fails
public type ReadTimedOutError distinct Error;

# Defines the Auth error types that returned from client
public type ClientAuthError distinct Error;

//# The union of all the WebSocket related errors
//public type Error ConnectionClosureError|InvalidHandshakeError|PayloadTooBigError|
//ProtocolError|ConnectionError|InvalidContinuationFrameError|GenericError|UpgradeError|
//GenericClientError|ReadTimedOutError|HandshakeTimedOut|ClientAuthError;

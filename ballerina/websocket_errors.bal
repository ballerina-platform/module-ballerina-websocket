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

# Represents any error related to the WebSocket module.
public type Error distinct error;

# Raised during the handshake when the WebSocket upgrade fails.
public type InvalidHandshakeError distinct Error;

# Raised when receiving a frame with a payload exceeding the maximum size.
public type PayloadTooLargeError distinct Error;

# Raised when the other side breaks the protocol.
public type CorruptedFrameError distinct Error;

# Raised during connection failures.
public type ConnectionError distinct Error;

# Raised during failures in connection closure.
public type ConnectionClosureError distinct ConnectionError;

# Raised when an out of order/invalid continuation frame is received.
public type InvalidContinuationFrameError distinct CorruptedFrameError;

# Raised when the WebSocket upgrade is not accepted.
public type UpgradeError distinct Error;

# Raised when the initial WebSocket handshake timed out.
public type HandshakeTimedOut distinct Error;

# Raised when the client read time out reaches.
public type ReadTimedOutError distinct Error;

# Defines the Auth error types that returned from the client.
public type AuthError distinct Error;

# Defines the authentication error type that returned from the listener.
public type AuthnError distinct Error;

# Defines the authorization error type that returned from the listener.
public type AuthzError distinct Error;

# Raised when the SSL handshake fails.
public type SslError distinct Error;

# Represents an error, which occurred due to payload binding.
public type PayloadBindingError distinct Error;

# Represents an error, which occurred due to payload constraint validation.
public type PayloadValidationError distinct PayloadBindingError;

//  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
//
//  WSO2 LLC. licenses this file to you under the Apache License,
//  Version 2.0 (the "License"); you may not use this file except
//  in compliance with the License.
//  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing,
//  software distributed under the License is distributed on an
//  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//  KIND, either express or implied. See the License for the
//  specific language governing permissions and limitations
//  under the License.

public readonly distinct class PredefinedCloseFrameType {
};

public readonly distinct class CustomCloseFrameType {
};

public final PredefinedCloseFrameType PREDEFINED_CLOSE_FRAME = new;
public final CustomCloseFrameType CUSTOM_CLOSE_FRAME = new;

type CloseFrameBase record {|
    readonly object {} 'type;
    readonly int status;
    string reason?;
|};

public type CustomCloseFrame record {|
    *CloseFrameBase;
    readonly CustomCloseFrameType 'type = CUSTOM_CLOSE_FRAME;
|};

public type NormalClosure record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1000 status = 1000;
|};

public type GoingAway record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1001 status = 1001;
|};

public type ProtocolError record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1002 status = 1002;
    string reason = "Connection closed due to protocol error";
|};

public type UnsupportedData record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1003 status = 1003;
    string reason = "Endpoint received unsupported frame";
|};

public type InvalidPayload record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1007 status = 1007;
    string reason = "Payload does not match the expected format or encoding";
|};

public type PolicyViolation record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1008 status = 1008;
    string reason = "Received message violates its policy";
|};

public type MessageTooBig record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1009 status = 1009;
    string reason = "The received message exceeds the allowed size limit";
|};

public type InternalServerError record {|
    *CloseFrameBase;
    readonly PredefinedCloseFrameType 'type = PREDEFINED_CLOSE_FRAME;
    readonly 1011 status = 1011;
    string reason = "Internal server error occurred";
|};

public final readonly & NormalClosure NORMAL_CLOSURE = {};
public final readonly & GoingAway GOING_AWAY = {};
public final readonly & ProtocolError PROTOCOL_ERROR = {};
public final readonly & UnsupportedData UNSUPPORTED_DATA = {};
public final readonly & InvalidPayload INVALID_PAYLOAD = {};
public final readonly & PolicyViolation POLICY_VIOLATION = {};
public final readonly & MessageTooBig MESSAGE_TOO_BIG = {};
public final readonly & InternalServerError INTERNAL_SERVER_ERROR = {};

public type CloseFrame NormalClosure|GoingAway|ProtocolError|UnsupportedData|InvalidPayload|
                        PolicyViolation|MessageTooBig|InternalServerError|CustomCloseFrame;

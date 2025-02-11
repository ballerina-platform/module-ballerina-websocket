// Copyright (c) 2025 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public type Status distinct object {
    public int code; //  Constraint minValue: 1000, maxValue: 4999
};

type CloseFrame record {|
    readonly Status status;
    string reason?;
|};

public const NORMAL_CLOSURE_STATUS_CODE = 1000;
public const GOING_AWAY_STATUS_CODE = 1001;
// public const PROTOCOL_ERROR_STATUS_CODE = 1002;
public const UNSUPPORTED_DATA_STATUS_CODE = 1003;
public const INVALID_PAYLOAD_STATUS_CODE = 1007;
public const POLICY_VIOLATION_STATUS_CODE = 1008;
public const MESSAGE_TOO_BIG_STATUS_CODE = 1009;
public const INTERNAL_SERVER_ERROR_STATUS_CODE = 1011;

public readonly distinct class NormalClosureStatus {
    *Status;
    public int code = NORMAL_CLOSURE_STATUS_CODE;
}

public readonly distinct class GoingAwayStatus {
    *Status;
    public int code = GOING_AWAY_STATUS_CODE;
}

// public readonly distinct class ProtocolErrorStatus {
//     *Status;
//     public int code = PROTOCOL_ERROR_STATUS_CODE;
// }

public readonly distinct class UnsupportedDataStatus {
    *Status;
    public int code = UNSUPPORTED_DATA_STATUS_CODE;
}

public readonly distinct class InvalidPayloadStatus {
    *Status;
    public int code = INVALID_PAYLOAD_STATUS_CODE;
}

public readonly distinct class PolicyViolationStatus {
    *Status;
    public int code = POLICY_VIOLATION_STATUS_CODE;
}

public readonly distinct class MessageTooBigStatus {
    *Status;
    public int code = MESSAGE_TOO_BIG_STATUS_CODE;
}

public readonly distinct class InternalServerErrorStatus {
    *Status;
    public int code = INTERNAL_SERVER_ERROR_STATUS_CODE;
}

public final NormalClosureStatus NORMAL_CLOSURE_STATUS_OBJ = new;
public final GoingAwayStatus GOING_AWAY_STATUS_OBJ = new;
// public final ProtocolErrorStatus PROTOCOL_ERROR_STATUS_OBJ = new;
public final UnsupportedDataStatus UNSUPPORTED_DATA_STATUS_OBJ = new;
public final InvalidPayloadStatus INVALID_PAYLOAD_STATUS_OBJ = new;
public final PolicyViolationStatus POLICY_VIOLATION_STATUS_OBJ = new;
public final MessageTooBigStatus MESSAGE_TOO_BIG_STATUS_OBJ = new;
public final InternalServerErrorStatus INTERNAL_SERVER_ERROR_STATUS_OBJ = new;

public type NormalClosure record {|
    *CloseFrame;
    readonly NormalClosureStatus status = NORMAL_CLOSURE_STATUS_OBJ;
|};

public type GoingAway record {|
    *CloseFrame;
    readonly GoingAwayStatus status = GOING_AWAY_STATUS_OBJ;
|};

// public type ProtocolError record {|
//     *CloseFrame;
//     readonly ProtocolErrorStatus status = PROTOCOL_ERROR_STATUS_OBJ;
//     string reason = "Connection closed due to protocol error";
// |};

public type UnsupportedData record {|
    *CloseFrame;
    readonly UnsupportedDataStatus status = UNSUPPORTED_DATA_STATUS_OBJ;
    string reason = "Endpoint received unsupported frame";
|};

public type InvalidPayload record {|
    *CloseFrame;
    readonly InvalidPayloadStatus status = INVALID_PAYLOAD_STATUS_OBJ;
    string reason = "Payload does not match the expected format or encoding";
|};

public type PolicyViolation record {|
    *CloseFrame;
    readonly PolicyViolationStatus status = POLICY_VIOLATION_STATUS_OBJ;
    string reason = "Received message violates its policy";
|};

public type MessageTooBig record {|
    *CloseFrame;
    readonly MessageTooBigStatus status = MESSAGE_TOO_BIG_STATUS_OBJ;
    string reason = "The received message exceeds the allowed size limit";
|};

public type InternalServerError record {|
    *CloseFrame;
    readonly InternalServerErrorStatus status = INTERNAL_SERVER_ERROR_STATUS_OBJ;
    string reason = "Internal server error occurred";
|};

public final readonly & NormalClosure NORMAL_CLOSURE = {};
public final readonly & GoingAway GOING_AWAY = {};
// public final readonly & ProtocolError PROTOCOL_ERROR = {};
public final readonly & UnsupportedData UNSUPPORTED_DATA = {};
public final readonly & InvalidPayload INVALID_PAYLOAD = {};
public final readonly & PolicyViolation POLICY_VIOLATION = {};
public final readonly & MessageTooBig MESSAGE_TOO_BIG = {};
public final readonly & InternalServerError INTERNAL_SERVER_ERROR = {};

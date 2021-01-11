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

# The Websocket service type
public type Service service object {
};

# The Websocket upgrade service type
public type UpgradeService service object {
};

# The type of the user-defined custom record
type CustomRecordType record {| anydata...; |};

# The types of the message that are returned by the Sync `client` after the data binding operation
public type PayloadType string|xml|json|byte[]|CustomRecordType;

# The types of data values that are expected by the Sync `client` to return after the data binding operation
public type TargetType typedesc<string|xml|json|byte[]|CustomRecordType>;

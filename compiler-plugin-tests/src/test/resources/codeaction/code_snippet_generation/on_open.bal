// Copyright (c) 2022 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// //www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/websocket;

service  on new websocket:Listener(0) {
	resource function get .() returns websocket:Service|websocket:Error {
		return new WsService();
	}
}

service class WsService {
	*websocket:Service;

	remote isolated function onOpen(websocket:Caller caller) returns websocket:Error? {

	}
}

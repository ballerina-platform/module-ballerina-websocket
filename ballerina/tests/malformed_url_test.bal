// Copyright (c) 2021 WSO2 Inc. (//www.wso2.org) All Rights Reserved.
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

import ballerina/test;

// Tests the malformed url error in synchronous client
@test:Config {}
public function testMalformedUrl() returns Error? {
    Client|Error wsClient = new("xxx");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "Error: Malformed URL: xxx");
    } else {
        test:assertFail("Expected a malformed URL error");
    }
}

@test:Config {}
public function testUrlWithIpAddress() returns Error? {
    Client|Error wsClient = new("127.0.0.1:9090/echo");
    if wsClient is Error {
        test:assertEquals(wsClient.message(), "Error: Illegal character in scheme name at index 0: 127.0.0.1:9090/echo");
    } else {
        test:assertFail("Expected an URL error");
    }
}

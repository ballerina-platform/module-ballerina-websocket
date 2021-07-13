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

listener Listener l75 = new(21075);

service /onPanic on l75 {
    resource function get .() returns Service|UpgradeError {
        if (true) {
           panic error("panic from the service");
        }
        return new WsService75();
    }
}

service class WsService75 {
    *Service;
    remote function onTextMessage(Caller caller, string data) returns error? {
        return error("error returned");
    }
}

// Tests error panicked from get resource.
@test:Config {}
public function testPanicErrorFromUpgradeService() returns Error? {
    Client|Error wsClient = new ("ws://localhost:21075/onPanic/");
    if (wsClient is Error) {
        test:assertEquals(wsClient.message(), "InvalidHandshakeError: Invalid handshake response getStatus: "
                               + "500 Internal Server Error");
    } else {
        test:assertFail("Should return an InvalidHandshakeError");
        error? result = wsClient->close(1001, "Close the connection", timeout = 0);
    }
}

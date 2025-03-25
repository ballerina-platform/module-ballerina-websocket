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

import ballerina/test;

final readonly & json jsonMessageWithEscapedQuotes = {
    "message": "Cannot query field \"invalidField\" on type \"Subscription\"."
};

service / on new Listener(22101) {

    resource function get .() returns Service|UpgradeError {
        return new WsService22101();
    }
}

service class WsService22101 {
    *Service;

    remote function onMessage(string message) returns json {
        return jsonMessageWithEscapedQuotes;
    }
}

@test:Config {}
public function testEscapedDoubleQuoteInJson() returns error? {
    Client wsClient = check new ("ws://localhost:22101");
    check wsClient->writeMessage("Hello");
    json response = check wsClient->readMessage();
    test:assertEquals(response, jsonMessageWithEscapedQuotes);
}

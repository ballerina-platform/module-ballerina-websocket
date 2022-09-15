// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/websocket;
import ballerina/lang.runtime;
import ballerina/random;

listener websocket:Listener localListener = new (80);

const hasSubscription = "hasSubscription";

const connId = "ConnectionId";

service / on localListener {
    resource function get .() returns websocket:Service|websocket:Error {
        return new WsService();
    }
}

service class WsService {
    *websocket:Service;
    remote function onOpen(websocket:Caller caller) returns error? {
        int randomInteger = check random:createIntInRange(1, 100);
        caller.setAttribute(connId, randomInteger);
        check sendMessage(caller);
    }

    remote function onMessage(websocket:Caller caller, Subscribe|Unsubscribe message) returns error? {
        return processReceivedMessage(message, caller);
    }

    remote function onPing(websocket:Caller caller, byte[] message) returns byte[] {
        return message;
    }
}

function sendMessage(websocket:Caller caller) returns error? {
    SystemStatus systemStatus = {
        connectionID: <int>check caller.getAttribute(connId),
        event: "systemStatus",
        'version: "1.0.0",
        status: online
    };
    check caller->writeMessage(systemStatus);
}

function sendUpdates(websocket:Caller caller) returns error? {
    Heartbeat hearbeat = {event: "heartbeat"};
    while true {
        if <boolean>check caller.getAttribute(hasSubscription) {
            check caller->writeMessage(hearbeat);
            runtime:sleep(2);
            check caller->writeMessage(getUpdates());
        } else {
            break;
        }
    }
}

function getUpdates() returns Ticker {
    Ticker ticker = {
        id: 340,
        ask: {
            price: <decimal>random:createDecimal(),
            wholeLotVolume: 6,
            lotVolume: <decimal>random:createDecimal()
        },
        bid: {
            price: <decimal>random:createDecimal() * 10000,
            wholeLotVolume: 0,
            lotVolume: <decimal>random:createDecimal()
        },
        close: {
            price: <decimal>random:createDecimal() * 40000,
            lotVolume: <decimal>random:createDecimal()
        },
        volume: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        },
        volumeWeightedAvgPrice: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        },
        noOfTrades: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        },
        lowPrice: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        },
        highPrice: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        },
        openPrice: {
            today: <decimal>random:createDecimal() * 10000,
            last24Hours: <decimal>random:createDecimal() * 5000
        }
    };
    return ticker;
}

function processReceivedMessage(Subscribe|Unsubscribe message, websocket:Caller caller) returns error? {
    if message is Subscribe {
        string event = <string>message?.event;
        string[] pair = <string[]>message?.pair;
        Subscribe subscription = <Subscribe>message;
        string? nameVal = subscription?.subscription?.name;
        Name name = <Name>nameVal;
        if event is "subscribe" {
            if pair.length() > 1 || pair[0] != "XBT/USD" {
                SubscriptionStatus subscriptionStatus = {
                    subscritionStatusError: {
                        errorMessage: "Only XBT/USD is supported for now",
                        subscriptionStatusCommon: {
                            channelID: <int>check caller.getAttribute(connId),
                            channelName: name,
                            event: "subscriptionStatus",
                            pair: pair,
                            status: errors,
                            subscription: {
                                depth: 42,
                                name: book
                            }
                        }
                    }
                };
                check caller->writeMessage(subscriptionStatus);
                return;
            } else {
                SubscriptionStatus subscriptionStatus = {
                    subscriptionStatusSuccess: {
                        subscriptionStatusCommon: {
                            channelID: <int>check caller.getAttribute(connId),
                            channelName: name,
                            event: "subscriptionStatus",
                            pair: pair,
                            status: subscribed,
                            subscription: {name: name}
                        }
                    }
                };
                caller.setAttribute(hasSubscription, true);
                check caller->writeMessage(subscriptionStatus);
                future<(error?)> _ = start sendUpdates(caller);
            }

        }
    } else {
        string[] pair = <string[]>message?.subscribe?.pair;
        Subscribe subscription = <Subscribe>message?.subscribe;
        string? nameVal = subscription?.subscription?.name;
        Name name = <Name>nameVal;
        SubscriptionStatus unSubscriptionStatus = {
            subscriptionStatusSuccess: {
                subscriptionStatusCommon: {
                    channelID: <int>check caller.getAttribute(connId),
                    channelName: name,
                    'event: "subscriptionStatus",
                    pair: pair,
                    status: unsubscribed,
                    subscription: {name: name}
                }
            }
        };
        caller.setAttribute(hasSubscription, false);
        check caller->writeMessage(unSubscriptionStatus);
    }
}

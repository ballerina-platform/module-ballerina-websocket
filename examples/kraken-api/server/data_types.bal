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

public type Heartbeat record {
    string event?;
};

public type Unsubscribe record {
    Subscribe subscribe;
};

public type Subscribe record {
    Subscription subscription?;
    string event;
    Pair pair?;
    Reqid reqid?;
};

public type Ping record {
    string event;
    Reqid reqid?;
};

public type Ratecounter boolean?;

public type Pong record {
    string event?;
    Reqid reqid?;
};

public type Pair string[]?;

public type Token string?;

public type Reqid int?;

public type Depth int?;

public type SystemStatus record {
    int connectionID?;
    string event?;
    string 'version?;
    Status status?;
};

public type SubscriptionStatusCommon record {
    int channelID;
    string channelName;
    string event;
    Reqid reqid?;
    Pair pair?;
    Status status?;
    Subscription subscription?;
};

public type SubscriptionStatusError record {
    string errorMessage;
    SubscriptionStatusCommon subscriptionStatusCommon?;
};

public type SubscriptionStatusSuccess record {
    SubscriptionStatusCommon subscriptionStatusCommon?;
};

public type SubscriptionStatus record {
    SubscriptionStatusError subscritionStatusError?;
    SubscriptionStatusSuccess subscriptionStatusSuccess?;
};

public type subscriptionStatusCommon record {
    int channelID;
    Subscription subscription?;
};

public type Subscription record {
    Depth depth?;
    Interval interval?;
    MaxRateCount maxratecount?;
    Name name;
    Token token?;
};

public type MaxRateCount int?;

public enum Name {
    book,
    ohlc,
    openOrders,
    ownTrades,
    spread,
    ticker,
    trade
}

public type Interval int?;

public type Snapshot boolean?;

public enum Status {
    online,
    maintenance,
    cancel_only,
    limit_only,
    post_only,
    subscribed,
    unsubscribed,
    errors
}

public type Ask record {
    decimal price;
    int wholeLotVolume;
    decimal lotVolume;
};

public type Bid record {
    decimal price;
    int wholeLotVolume;
    decimal lotVolume;
};

public type Close record {
    decimal price;
    decimal lotVolume;
};

public type Volume record {
    decimal today;
    decimal last24Hours;
};

public type VolumeWeightedAvgPrice record {
    decimal today;
    decimal last24Hours;
};

public type NoOfTrades record {
    decimal today;
    decimal last24Hours;
};

public type LowPrice record {
    decimal today;
    decimal last24Hours;
};

public type HighPrice record {
    decimal today;
    decimal last24Hours;
};

public type OpenPrice record {
    decimal today;
    decimal last24Hours;
};

public type Ticker record {
    int id;
    Ask ask;
    Bid bid;
    Close close;
    Volume volume;
    VolumeWeightedAvgPrice volumeWeightedAvgPrice;
    NoOfTrades noOfTrades;
    LowPrice lowPrice;
    HighPrice highPrice;
    OpenPrice openPrice;
};

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

public type ListenerConfigs record {
};

public type Heartbeat record {
    string event?;
};

public type Unsubscribe record {
    Subscribe subscribe;
};

public type Subscribe record {
    record  { Depth depth?; Ratecounter ratecounter?; Name name; Interval interval?; Snapshot snapshot?; Token token?;}  subscription?;
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
    # The ID of the connection
    int connectionID?;
    string event?;
    string 'version?;
    Status status?;
};

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
    post_only
}

public type GenericDataType Heartbeat|Subscribe|Ping|Ratecounter|Pong|Pair|Token|Reqid|Depth|SystemStatus|Interval|Snapshot;

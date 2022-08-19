// Listener related configurations should be included here
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

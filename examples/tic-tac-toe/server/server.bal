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

import ballerina/io;
import ballerina/websocket;

const string USER_SIGN = "userSign";
const string SIGN_X = "X";
const string SIGN_O = "O";

map<websocket:Caller> connectionsMap = {};
string[9] squares = [];
boolean started = false;
string next = "";
string winner = "";

type UserMove record {
    string 'type;
    int move;
    string next;
    string player;
};

type FirstMessage record {
    string 'type;
    boolean success;
    string sign;
    string next;
};

type Winner record {
    readonly "end" 'type = "end";
    string winner;
};

type PlayerLeft record {
    readonly "playerLeft" 'type = "playerLeft";
    string player;
};

service /ws on new websocket:Listener(8000) {
    resource function get game() returns websocket:Service|websocket:UpgradeError {
        // The server can accept a WebSocket connection by returning a `websocket:Service`.
        return new GameServer();
    }
}

service class GameServer {
    *websocket:Service;

    remote function onOpen(websocket:Caller caller) returns error? {
        if connectionsMap.length() >= 2 {
            check caller->writeMessage("Only two players are allowed");
            check caller->close();
        } else {
            FirstMessage welcomeMsg;
            string sign = connectionsMap.hasKey(SIGN_X) ? SIGN_O: SIGN_X;
            if started {
                welcomeMsg = { 'type: "state", success : true, sign : sign, next : next, "squares": squares, "winner": winner};
            } else {
                welcomeMsg = { 'type: "start", success : true, sign : sign, next : SIGN_X};
            }
            caller.setAttribute(USER_SIGN, sign);
            check caller->writeMessage(welcomeMsg);
            lock {
                connectionsMap[sign] = caller;
            }
        }
    }

    remote function onMessage(websocket:Caller caller, int squareNumber) returns error? {
        string sign = check getUserSign(caller, USER_SIGN);
        lock {
            squares[squareNumber] = sign;
            started = true;
        }

        UserMove msg = {
            'type: "move",
            move: squareNumber,
            next: sign == SIGN_X ? SIGN_O: SIGN_X,
            player: sign
        };
        check broadcast(msg);
        string? calcWinner = calculateWinner();
        if calcWinner is string {
            Winner winnerMessage = {winner: calcWinner};
            check broadcast(winnerMessage);
            winner = calcWinner;
        }
    }

    remote function onClose(websocket:Caller caller, int statusCode, string reason) returns error? {
        lock {
            _ = connectionsMap.remove(check getUserSign(caller, USER_SIGN));
        }
        PlayerLeft msg = {player: check getUserSign(caller, USER_SIGN)};
        check broadcast(msg);
    }
}

// Function to perform the broadcasting of messages.
function broadcast(string|PlayerLeft|UserMove|Winner message) returns error? {
    foreach websocket:Caller con in connectionsMap {
        websocket:Error? err = con->writeMessage(message);
        if err is websocket:Error {
            io:println("Error sending message to the :" + check getUserSign(con, USER_SIGN) +
                        ". Reason: " + err.message());
        }
    }
}

function getUserSign(websocket:Caller ep, string key) returns string|error {
    return <string> check ep.getAttribute(key);
}

function calculateWinner() returns string? {
    int[][] lines = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]];
    foreach int[] i in lines {
        int[] block = i;
        int a = block[0];
        int b = block[1];
        int c = block[2];
        if squares[a] == squares[b] && squares[a] == squares[c] {
            return squares[a];
        }
    }
}

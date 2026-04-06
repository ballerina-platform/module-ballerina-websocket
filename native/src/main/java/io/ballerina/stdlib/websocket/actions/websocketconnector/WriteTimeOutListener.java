/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.websocket.actions.websocketconnector;

import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketWriteTimeOutListener;
import io.ballerina.stdlib.websocket.WebSocketUtil;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class for notifying the write timeout to the client.
 */
public class WriteTimeOutListener implements WebSocketWriteTimeOutListener {

    private CompletableFuture<Object> balFuture;
    private AtomicBoolean futureCompleted;

    public WriteTimeOutListener(CompletableFuture<Object> balFuture, AtomicBoolean futureCompleted) {
        this.balFuture = balFuture;
        this.futureCompleted = futureCompleted;
    }

    public void onTimeout(Throwable error) {
        if (!futureCompleted.get()) {
            balFuture.complete(WebSocketUtil.createErrorByType(error));
            futureCompleted.set(true);
        }
    }
}

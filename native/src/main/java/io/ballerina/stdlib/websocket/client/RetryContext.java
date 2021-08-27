/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websocket.client;

/**
 * Represents retry config.
 */
public class RetryContext {
    private int interval = 0;
    private Double backOfFactor = 0.0;
    private int maxInterval = 0;
    private int maxAttempts = 0;
    private int reconnectAttempts = 0;
    private boolean firstConnectionMadeSuccessfully = false;

    public int getInterval() {
        return interval;
    }

    public final void setInterval(int interval) {
        this.interval = interval;
    }

    public Double getBackOfFactor() {
        return backOfFactor;
    }

    public void setBackOfFactor(Double backOfFactor) {
        this.backOfFactor = backOfFactor;
    }

    public int getMaxInterval() {
        return maxInterval;
    }

    public void setMaxInterval(int maxInterval) {
        this.maxInterval = maxInterval;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void setReconnectAttempts(int reconnectAttempts) {
        this.reconnectAttempts = reconnectAttempts;
    }

    public boolean isFirstConnectionMadeSuccessfully() {
        return firstConnectionMadeSuccessfully;
    }

    public void setFirstConnectionMadeSuccessfully() {
        this.firstConnectionMadeSuccessfully = true;
    }
}

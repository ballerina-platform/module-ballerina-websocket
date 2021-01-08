/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.net.websocket;

/**
 * Defines enum for Websocket Error types. This enum stores Error type name and corresponding reason string.
 */
public enum WebsocketErrorType {
    GENERIC_CLIENT_ERROR("GenericClientError", "{ballerina/websocket}GenericClientError"), GENERIC_LISTENER_ERROR(
            "GenericListenerError", "{ballerina/websocket}GenericListenerError"), LISTENER_STARTUP_FAILURE(
            "ListenerError", "{ballerina/websocket}ListenerStartupError"), READING_INBOUND_TEXT_FAILED(
            "ReadingInboundTextError", "{ballerina/websocket}ReadingInboundTextFailed"), READING_INBOUND_BINARY_FAILED(
            "ReadingInboundBinaryError", "{ballerina/websocket}ReadingInboundBinaryFailed");

    private final String errorName;
    private final String reason;

    WebsocketErrorType(String errorName, String reason) {
        this.errorName = errorName;
        this.reason = reason;
    }

    /**
     * Returns the name of the error type, which is defined in the ballerina websocket errors.
     *
     * @return the name of the error type as a String
     */
    public String getErrorName() {
        return errorName;
    }

    /**
     * Returns the reason string of the error, as defined in the ballerina errors.
     *
     * @return the reason constant value of the error, as a String
     */
    public String getReason() {
        return reason;
    }
}

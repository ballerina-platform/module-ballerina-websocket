/*
 * Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;

import java.util.concurrent.CompletableFuture;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.stdlib.websocket.WebSocketConstants.BALLERINA_ORG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PACKAGE_WEBSOCKET;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_BINARY_MESSAGE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_CLOSE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_ERROR;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_IDLE_TIMEOUT;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_OPEN;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_PING;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_PONG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.RESOURCE_NAME_UPGRADE;

/**
 * This class will hold module related utility functions.
 */
public class ModuleUtils {

    private static Module websocketModule;
    private static String packageIdentifier;
    private static StrandMetadata onOpenMetaData;
    private static StrandMetadata onTextMetaData;
    private static StrandMetadata onBinaryMetaData;
    private static StrandMetadata onPingMetaData;
    private static StrandMetadata onPongMetaData;
    private static StrandMetadata onCloseMetaData;
    private static StrandMetadata onErrorMetaData;
    private static StrandMetadata onTimeoutMetaData;
    private static StrandMetadata onUpgradeMetaData;

    private ModuleUtils() {}

    public static void setModule(Environment env) {
        websocketModule = env.getCurrentModule();
        packageIdentifier = WebSocketConstants.PACKAGE + ORG_NAME_SEPARATOR + WebSocketConstants.PROTOCOL_WEBSOCKET
                + WebSocketConstants.SEPARATOR + websocketModule.getVersion();
        onOpenMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_OPEN);
        onTextMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_TEXT_MESSAGE);
        onBinaryMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_BINARY_MESSAGE);
        onPingMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_PING);
        onPongMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_PONG);
        onCloseMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_CLOSE);
        onErrorMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_ERROR);
        onTimeoutMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_ON_IDLE_TIMEOUT);
        onUpgradeMetaData = new StrandMetadata(BALLERINA_ORG, PACKAGE_WEBSOCKET,
                ModuleUtils.getWebsocketModule().getVersion(), RESOURCE_NAME_UPGRADE);
    }

    public static Module getWebsocketModule() {
        return websocketModule;
    }

    public static String getPackageIdentifier() {
        return packageIdentifier;
    }

    public static StrandMetadata getOnOpenMetaData() {
        return onOpenMetaData;
    }

    public static StrandMetadata getOnTextMetaData() {
        return onTextMetaData;
    }

    public static StrandMetadata getOnBinaryMetaData() {
        return onBinaryMetaData;
    }

    public static StrandMetadata getOnPingMetaData() {
        return onPingMetaData;
    }

    public static StrandMetadata getOnPongMetaData() {
        return onPongMetaData;
    }

    public static StrandMetadata getOnCloseMetaData() {
        return onCloseMetaData;
    }

    public static StrandMetadata getOnErrorMetaData() {
        return onErrorMetaData;
    }

    public static StrandMetadata getOnTimeoutMetaData() {
        return onTimeoutMetaData;
    }

    public static StrandMetadata getOnUpgradeMetaData() {
        return onUpgradeMetaData;
    }

    public static Object getResult(CompletableFuture<Object> balFuture) {
        try {
            return balFuture.get();
        } catch (Throwable throwable) {
            throw ErrorCreator.createError(throwable);
        }
    }
}

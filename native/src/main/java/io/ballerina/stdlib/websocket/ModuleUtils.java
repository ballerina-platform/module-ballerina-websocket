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
import io.ballerina.runtime.api.creators.ErrorCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.stdlib.websocket.WebSocketConstants.BALLERINA_ORG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PACKAGE_WEBSOCKET;

/**
 * This class will hold module related utility functions.
 */
public class ModuleUtils {

    private static Module websocketModule;
    private static String packageIdentifier;

    private ModuleUtils() {}

    public static void setModule(Environment env) {
        websocketModule = env.getCurrentModule();
        packageIdentifier = WebSocketConstants.PACKAGE + ORG_NAME_SEPARATOR + WebSocketConstants.PROTOCOL_WEBSOCKET
                + WebSocketConstants.SEPARATOR + websocketModule.getMajorVersion();
    }

    public static Module getWebsocketModule() {
        return websocketModule;
    }

    public static String getPackageIdentifier() {
        return packageIdentifier;
    }

    public static Map<String, Object> getProperties(String resourceName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("moduleOrg", BALLERINA_ORG);
        properties.put("moduleName", PACKAGE_WEBSOCKET);
        properties.put("moduleVersion", ModuleUtils.getWebsocketModule().getMajorVersion());
        properties.put("parentFunctionName", resourceName);
        return properties;
    }

    public static Object getResult(CompletableFuture<Object> balFuture) {
        try {
            return balFuture.get();
        } catch (Throwable throwable) {
            throw ErrorCreator.createError(throwable);
        }
    }
}

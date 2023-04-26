/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.serviceendpoint;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.server.WebSocketServerService;
import io.ballerina.stdlib.websocket.server.WebSocketServicesRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Register a service to the listener.
 *
 */
public class Register extends AbstractWebsocketNativeFunction {
    public static Object register(Environment env, BObject serviceEndpoint, BObject service,
            Object serviceName) {

        WebSocketServicesRegistry webSocketServicesRegistry = getWebSocketServicesRegistry(serviceEndpoint);
        Runtime runtime = env.getRuntime();
        String basePath = getBasePath(serviceName);

        MethodType[] resourceList = ((ServiceType) TypeUtils.getType(service)).getResourceMethods();
        ResourceMethodType resource = (ResourceMethodType) resourceList[0];
        resource.getAccessor();

        try {
            if (resourceList.length == 1 && ((ResourceMethodType) resourceList[0]).getAccessor()
                    .equals(WebSocketConstants.GET)) {
                webSocketServicesRegistry.registerService(new WebSocketServerService(service, runtime, basePath));
            }
        } catch (BError ex) {
            return ex;
        }
        return null;
    }

    private static String getBasePath(Object serviceName) {
        if (serviceName instanceof BArray) {
            List<String> strings = Arrays.stream(((BArray) serviceName).getStringArray()).map(
                    HttpUtil::unescapeAndEncodeValue).collect(Collectors.toList());
            String basePath = String.join(WebSocketConstants.BACK_SLASH, strings);
            return HttpUtil.sanitizeBasePath(basePath);
        } else if (serviceName instanceof BString) {
            String basePath = ((BString) serviceName).getValue();
            return HttpUtil.sanitizeBasePath(basePath);
        } else {
            return HttpConstants.DEFAULT_BASE_PATH;
        }
    }

    private Register() {}
}

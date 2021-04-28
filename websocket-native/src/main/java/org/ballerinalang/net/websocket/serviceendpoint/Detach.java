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


package org.ballerinalang.net.websocket.serviceendpoint;

import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.net.websocket.ModuleUtils;
import org.ballerinalang.net.websocket.server.WebSocketServicesRegistry;

import static org.ballerinalang.net.websocket.WebSocketConstants.SEPARATOR;
import static org.ballerinalang.net.websocket.WebSocketConstants.WEBSOCKET_CALLER;

/**
 * Disengage a service from the listener.
 *
 */
public class Detach extends AbstractWebsocketNativeFunction {
    public static final String WEBSOCKET_CALLER_NAME =
            ModuleUtils.getPackageIdentifier() + SEPARATOR + WEBSOCKET_CALLER;

    public static Object detach(BObject serviceEndpoint, BObject serviceObj) {
        WebSocketServicesRegistry webSocketServicesRegistry = getWebSocketServicesRegistry(serviceEndpoint);
        Type param;
        MethodType[] resourceList = serviceObj.getType().getMethods();
        if (resourceList.length > 0 && (param = resourceList[0].getParameterTypes()[0]) != null) {
            String callerType = param.getQualifiedName();
            if (WEBSOCKET_CALLER_NAME.equals(callerType)) {
                return webSocketServicesRegistry.unRegisterService(serviceObj);
            }
        }
        return null;
    }
}

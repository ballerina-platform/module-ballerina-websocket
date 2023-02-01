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

import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.websocket.ModuleUtils;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.server.WebSocketServicesRegistry;

/**
 * Disengage a service from the listener.
 *
 */
public class Detach extends AbstractWebsocketNativeFunction {
    public static final String WEBSOCKET_CALLER_NAME =
            ModuleUtils.getPackageIdentifier() + WebSocketConstants.SEPARATOR + WebSocketConstants.WEBSOCKET_CALLER;

    public static Object detach(BObject serviceEndpoint, BObject serviceObj) {
        WebSocketServicesRegistry webSocketServicesRegistry = getWebSocketServicesRegistry(serviceEndpoint);
        Type param;
        ObjectType serviceObjType = (ObjectType) TypeUtils.getReferredType(serviceObj.getType());
        MethodType[] resourceList = serviceObjType.getMethods();
        if (resourceList.length > 0 && (param = resourceList[0].getParameterTypes()[0]) != null) {
            String callerType = param.getQualifiedName();
            if (WEBSOCKET_CALLER_NAME.equals(callerType)) {
                return webSocketServicesRegistry.unRegisterService(serviceObj);
            }
        }
        return null;
    }

    private Detach() {}
}

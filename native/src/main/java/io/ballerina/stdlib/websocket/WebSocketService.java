/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;
import static io.ballerina.stdlib.websocket.WebSocketConstants.ANNOTATION_ATTR_DISPATCHER_VALUE;
import static io.ballerina.stdlib.websocket.WebSocketConstants.UNCHECKED;

/**
 * WebSocket service for service dispatching.
 */
public class WebSocketService {

    protected final BObject service;
    protected Runtime runtime;
    private final Map<String, MethodType> resourcesMap = new ConcurrentHashMap<>();
    private Map<String, Object> wsServices = new ConcurrentHashMap<>();
    private Map<String, Map<String, MethodType>> wsServicesDispatchingFunctions = new ConcurrentHashMap<>();

    public WebSocketService(Runtime runtime) {
        this.runtime = runtime;
        service = null;
    }

    public WebSocketService(BObject service, Runtime runtime) {
        this.runtime = runtime;
        this.service = service;
        populateResourcesMap(service);
    }

    private void populateResourcesMap(BObject service) {
        ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
        for (MethodType resource : serviceType.getMethods()) {
            resourcesMap.put(resource.getName(), resource);
        }
    }

    private Map<String, MethodType> getDispatchingFunctionMap(Object dispatchingService) {
        Map<String, MethodType> dispatchingFunctions = new ConcurrentHashMap<>();
        Set<String> seenRemoteFunctionNames = new HashSet<>();
        MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType())).getMethods();
        // 1. DispatcherConfig values have the highest priority
        for (MethodType remoteFunc : remoteFunctions) {
            Optional<String> dispatchingValue = getAnnotationDispatchingValue(remoteFunc);
            if (dispatchingValue.isPresent()) {
                dispatchingFunctions.put(dispatchingValue.get(), remoteFunc);
                seenRemoteFunctionNames.add(remoteFunc.getName());
            }
        }
        // 2. Custom remote function names have the next priority
        for (MethodType remoteFunc : remoteFunctions) {
            if (!seenRemoteFunctionNames.contains(remoteFunc.getName())) {
                dispatchingFunctions.put(remoteFunc.getName(), remoteFunc);
            }
        }
        return dispatchingFunctions;
    }

    @SuppressWarnings(UNCHECKED)
    public static Optional<String> getAnnotationDispatchingValue(MethodType remoteFunc) {
        BMap<BString, Object> annotations = (BMap<BString, Object>) remoteFunc.getAnnotation(fromString(
                ModuleUtils.getPackageIdentifier() + ":" + WebSocketConstants.WEBSOCKET_DISPATCHER_CONFIG_ANNOTATION));
        if (annotations != null && annotations.containsKey(fromString(ANNOTATION_ATTR_DISPATCHER_VALUE))) {
            String dispatchingValue = annotations.
                    getStringValue(fromString(ANNOTATION_ATTR_DISPATCHER_VALUE)).getValue();
            return Optional.of(dispatchingValue);
        }
        return Optional.empty();
    }

    public MethodType getResourceByName(String resourceName) {
        return resourcesMap.get(resourceName);
    }

    public BObject getBalService() {
        return service;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public void addWsService(String channelId, Object dispatchingService) {
        this.wsServices.put(channelId, dispatchingService);
        this.wsServicesDispatchingFunctions.put(channelId, getDispatchingFunctionMap(dispatchingService));
    }

    public Object getWsService(String key) {
        return this.wsServices.get(key);
    }

    public Map<String, MethodType> getDispatchingFunctions(String key) {
        return this.wsServicesDispatchingFunctions.get(key);
    }
}

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
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket service for service dispatching.
 */
public class WebSocketService {

    protected final BObject service;
    protected Runtime runtime;
    private final Map<String, MethodType> resourcesMap = new ConcurrentHashMap<>();
    private Map<String, Object> wsServices = new ConcurrentHashMap<>();

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
        ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(service.getType());
        for (MethodType resource : serviceType.getMethods()) {
            resourcesMap.put(resource.getName(), resource);
        }
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
    }

    public Object getWsService(String key) {
        return this.wsServices.get(key);
    }
}

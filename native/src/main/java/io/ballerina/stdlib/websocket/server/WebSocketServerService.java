/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.websocket.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.websocket.ModuleUtils;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketService;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.ballerina.runtime.api.utils.StringUtils.fromString;
import static io.ballerina.stdlib.websocket.WebSocketConstants.ANNOTATION_ATTR_CONNECTION_CLOSURE_TIMEOUT;
import static io.ballerina.stdlib.websocket.WebSocketConstants.ANNOTATION_ATTR_DISPATCHER_KEY;
import static io.ballerina.stdlib.websocket.WebSocketConstants.ANNOTATION_ATTR_VALIDATION_ENABLED;
import static io.ballerina.stdlib.websocket.WebSocketConstants.UNCHECKED;
import static io.ballerina.stdlib.websocket.WebSocketConstants.WEBSOCKET_ANNOTATION_CONFIGURATION;
import static io.ballerina.stdlib.websocket.WebSocketConstants.WEBSOCKET_ANNOTATION_CONFIGURATION_RECORD;

/**
 * WebSocket service for service dispatching.
 */
public class WebSocketServerService extends WebSocketService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServerService.class);

    private String[] negotiableSubProtocols = null;
    private String basePath;
    private int maxFrameSize = WebSocketConstants.DEFAULT_MAX_FRAME_SIZE;
    private int idleTimeoutInSeconds = 0;
    private boolean enableValidation = true;
    private String dispatchingKey = null;
    private int connectionClosureTimeout = getDefaultConnectionClosureTimeout();

    public WebSocketServerService(BObject service, Runtime runtime, String basePath) {
        super(service, runtime);
        populateConfigs(basePath);
    }

    public static int getDefaultConnectionClosureTimeout() {
        try {
            BMap<BString, Object> configAnnotation = ValueCreator
                    .createRecordValue(ModuleUtils.getWebsocketModule(), WEBSOCKET_ANNOTATION_CONFIGURATION_RECORD);
            return WebSocketUtil.findTimeoutInSeconds(configAnnotation,
                    fromString(ANNOTATION_ATTR_CONNECTION_CLOSURE_TIMEOUT));
        } catch (Exception e) {
            log.error("Unable to get default connection closure timeout", e);
        }
        return 0;
    }

    private void populateConfigs(String basePath) {
        BMap<BString, Object> configAnnotation = getServiceConfigAnnotation();
        if (configAnnotation != null) {
            negotiableSubProtocols = WebSocketUtil.findNegotiableSubProtocols(configAnnotation);
            idleTimeoutInSeconds = WebSocketUtil.findTimeoutInSeconds(configAnnotation,
                    WebSocketConstants.ANNOTATION_ATTR_IDLE_TIMEOUT, 0);
            connectionClosureTimeout = WebSocketUtil.findTimeoutInSeconds(configAnnotation,
                    fromString(ANNOTATION_ATTR_CONNECTION_CLOSURE_TIMEOUT));
            maxFrameSize = WebSocketUtil.findMaxFrameSize(configAnnotation);
            enableValidation = configAnnotation.getBooleanValue(ANNOTATION_ATTR_VALIDATION_ENABLED);
            if (configAnnotation.getStringValue(ANNOTATION_ATTR_DISPATCHER_KEY) != null) {
                dispatchingKey = configAnnotation.getStringValue(ANNOTATION_ATTR_DISPATCHER_KEY).getValue();
            }
        }
        service.addNativeData(WebSocketConstants.ANNOTATION_ATTR_MAX_FRAME_SIZE.toString(), maxFrameSize);
        service.addNativeData(ANNOTATION_ATTR_VALIDATION_ENABLED.toString(), enableValidation);
        // This will be overridden if there is an upgrade path
        setBasePathToServiceObj(basePath);
    }

    @SuppressWarnings(UNCHECKED)
    private BMap<BString, Object> getServiceConfigAnnotation() {
        ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
        return (BMap<BString, Object>) serviceType.getAnnotation(fromString(
                ModuleUtils.getPackageIdentifier() + ":" + WEBSOCKET_ANNOTATION_CONFIGURATION));
    }

    public String[] getNegotiableSubProtocols() {
        if (negotiableSubProtocols == null) {
            return new String[0];
        }
        return negotiableSubProtocols.clone();
    }

    public int getIdleTimeoutInSeconds() {
        return idleTimeoutInSeconds;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setBasePathToServiceObj(String basePath) {
        service.addNativeData(WebSocketConstants.NATIVE_DATA_BASE_PATH, basePath);
        this.basePath = basePath;
    }

    public String getDispatchingKey() {
        return dispatchingKey;
    }

    public String getBasePath() {
        return basePath;
    }

    public int getConnectionClosureTimeout() {
        return connectionClosureTimeout;
    }
}

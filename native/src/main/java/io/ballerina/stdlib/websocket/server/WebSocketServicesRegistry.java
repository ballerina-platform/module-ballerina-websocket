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

package io.ballerina.stdlib.websocket.server;

import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.http.api.HttpResourceArguments;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketHandshaker;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketMessage;
import io.ballerina.stdlib.http.uri.URITemplate;
import io.ballerina.stdlib.http.uri.URITemplateException;
import io.ballerina.stdlib.http.uri.parser.Literal;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Store all the WebSocket serviceEndpointsTemplate here.
 */
public class WebSocketServicesRegistry {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServicesRegistry.class);
    private URITemplate<WebSocketServerService, WebSocketMessage> uriTemplate;
    Map<String, WebSocketServerService> servicesByBasePath = new ConcurrentHashMap<>();
    List<String> sortedServiceURIs = new CopyOnWriteArrayList<>();

    public WebSocketServicesRegistry() {
        try {
            uriTemplate = new URITemplate<>(new Literal<>(new WebSocketDataElement(), WebSocketConstants.BACK_SLASH));
        } catch (URITemplateException e) {
            // Ignore as it won't be thrown because token length is not zero.
        }
    }

    public void registerService(WebSocketServerService service) {
        String basePath = service.getBasePath();
        try {
            basePath = URLDecoder.decode(basePath, StandardCharsets.UTF_8.name());
            uriTemplate.parse(basePath, service, new WebSocketDataElementFactory());
            servicesByBasePath.put(basePath, service);
            sortedServiceURIs.add(basePath);
            sortedServiceURIs.sort((basePath1, basePath2) -> basePath2.length() - basePath1.length());
        } catch (URITemplateException | UnsupportedEncodingException e) {
            logger.error("Error when registering service", e);
            throw WebSocketUtil.getWebSocketError("", e, WebSocketConstants.ErrorCode.Error.
                    errorCode(), null);
        }
        logger.info("WebSocketService deployed with context {}", basePath);
    }

    public WebSocketServerService findMatching(String path, HttpResourceArguments pathParams,
            WebSocketHandshaker webSocketHandshaker) {
        return uriTemplate.matches(path, pathParams, webSocketHandshaker);
    }

    public BError unRegisterService(BObject serviceObj) {
        try {
            String basePath = (String) serviceObj.getNativeData(WebSocketConstants.NATIVE_DATA_BASE_PATH);
            if (basePath == null) {
                throw WebSocketUtil.getWebSocketError("Cannot detach service. Service has not been registered",
                        null, WebSocketConstants.ErrorCode.Error.errorCode(), null);
            }
            uriTemplate.parse(basePath, null, new WebSocketDataElementFactory());
            serviceObj.addNativeData(WebSocketConstants.NATIVE_DATA_BASE_PATH, null);
        } catch (URITemplateException | UnsupportedEncodingException e) {
            logger.error("Error when unRegistering service", e);
            return WebSocketUtil.getWebSocketError("", e, WebSocketConstants.ErrorCode.Error.
                    errorCode(), null);
        } catch (BError e) {
            return e;
        }
        return null;
    }

    Map<String, WebSocketServerService> getServicesByBasePath() {
        return servicesByBasePath;
    }

    List<String> getSortedServiceURIs() {
        return sortedServiceURIs;
    }

    String findTheMostSpecificBasePath(String requestURIPath, Map<String, WebSocketServerService> services,
            List<String> sortedServiceURIs) {
        for (Object key : sortedServiceURIs) {
            if (!requestURIPath.toLowerCase(Locale.getDefault()).contains(
                    key.toString().toLowerCase(Locale.getDefault()))) {
                continue;
            }
            if (requestURIPath.length() <= key.toString().length()) {
                return key.toString();
            }
            if (requestURIPath.startsWith(key.toString().concat(WebSocketConstants.BACK_SLASH))) {
                return key.toString();
            }
        }
        if (services.containsKey(HttpConstants.DEFAULT_BASE_PATH)) {
            return HttpConstants.DEFAULT_BASE_PATH;
        }
        return null;
    }
}

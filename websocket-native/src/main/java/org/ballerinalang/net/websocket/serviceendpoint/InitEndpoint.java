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

import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.net.http.HttpConnectionManager;
import org.ballerinalang.net.http.HttpErrorType;
import org.ballerinalang.net.transport.contract.ServerConnector;
import org.ballerinalang.net.transport.contract.config.ListenerConfiguration;

import static org.ballerinalang.net.http.HttpUtil.getListenerConfig;
import static org.ballerinalang.net.websocket.WebSocketConstants.ENDPOINT_CONFIG_PORT;
import static org.ballerinalang.net.websocket.WebSocketConstants.HTTP_SERVER_CONNECTOR;
import static org.ballerinalang.net.websocket.WebSocketConstants.SERVICE_ENDPOINT_CONFIG;
import static org.ballerinalang.net.websocket.WebSocketUtil.createWebsocketError;

/**
 * Initialize the Websocket listener.
 *
 */
public class InitEndpoint extends AbstractWebsocketNativeFunction {
    public static Object initEndpoint(BObject serviceEndpoint) {
        try {
            // Creating server connector
            BMap serviceEndpointConfig = serviceEndpoint.getMapValue(SERVICE_ENDPOINT_CONFIG);
            long port = serviceEndpoint.getIntValue(ENDPOINT_CONFIG_PORT);
            ListenerConfiguration listenerConfiguration = getListenerConfig(port, serviceEndpointConfig);
            ServerConnector httpServerConnector =
                    HttpConnectionManager.getInstance().createHttpServerConnector(listenerConfiguration);
            serviceEndpoint.addNativeData(HTTP_SERVER_CONNECTOR, httpServerConnector);

            //Adding service registries to native data
            resetRegistry(serviceEndpoint);
            return null;
        } catch (BError errorValue) {
            return errorValue;
        } catch (Exception e) {
            return createWebsocketError(e.getMessage(), HttpErrorType.GENERIC_LISTENER_ERROR);
        }
    }
}

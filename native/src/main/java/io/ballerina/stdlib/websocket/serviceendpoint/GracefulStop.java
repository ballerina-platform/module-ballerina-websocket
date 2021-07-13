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

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.websocket.WebSocketConstants;
import io.ballerina.stdlib.websocket.WebSocketUtil;

/**
 * Stop the listener immediately and close the connection.
 *
 */
public class GracefulStop extends AbstractWebsocketNativeFunction {
    public static Object gracefulStop(BObject serverEndpoint) {
        try {
            getServerConnector(serverEndpoint).stop();
            serverEndpoint.addNativeData(HttpConstants.CONNECTOR_STARTED, false);
            resetRegistry(serverEndpoint);
        } catch (Exception ex) {
            return WebSocketUtil
                    .createWebsocketError(ex.getMessage(), WebSocketConstants.ErrorCode.Error);
        }
        return null;
    }

    private GracefulStop() {}
}

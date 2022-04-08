/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websocket;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BValue;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.stdlib.http.api.HttpConstants;
import io.ballerina.stdlib.http.api.HttpUtil;
import io.ballerina.stdlib.http.api.ValueCreatorUtils;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketBinaryMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketCloseMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnection;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnectorException;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketControlMessage;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketControlSignal;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketHandshaker;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketTextMessage;
import io.ballerina.stdlib.http.transport.message.HttpCarbonRequest;
import io.ballerina.stdlib.http.uri.URIUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil;
import io.ballerina.stdlib.websocket.observability.WebSocketObserverContext;
import io.ballerina.stdlib.websocket.server.OnUpgradeResourceCallback;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionInfo;
import io.ballerina.stdlib.websocket.server.WebSocketConnectionManager;
import io.ballerina.stdlib.websocket.server.WebSocketServerService;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.http.HttpHeaders;
import org.ballerinalang.langlib.value.CloneWithType;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.runtime.api.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.TypeTags.ERROR_TAG;
import static io.ballerina.runtime.api.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.TypeTags.INT_TAG;
import static io.ballerina.runtime.api.TypeTags.OBJECT_TYPE_TAG;
import static io.ballerina.runtime.api.TypeTags.STRING_TAG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.HEADER_ANNOTATION;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PARAM_ANNOT_PREFIX;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.ERROR_TYPE_MESSAGE_RECEIVED;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.ERROR_TYPE_RESOURCE_INVOCATION;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.MESSAGE_TYPE_BINARY;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.MESSAGE_TYPE_CLOSE;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.MESSAGE_TYPE_PING;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.MESSAGE_TYPE_PONG;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityConstants.MESSAGE_TYPE_TEXT;
import static io.ballerina.stdlib.websocket.observability.WebSocketObservabilityUtil.observeError;

/**
 * {@code WebSocketDispatcher} This is the web socket request dispatcher implementation which finds best matching
 * resource for incoming web socket request.
 *
 * @since 0.94
 */
public class WebSocketResourceDispatcher {
    private static final Logger log = LoggerFactory.getLogger(WebSocketResourceDispatcher.class);
    public static final MapType MAP_TYPE = TypeCreator.createMapType(PredefinedTypes.TYPE_JSON);

    private WebSocketResourceDispatcher() {
    }

    public static void dispatchUpgrade(WebSocketHandshaker webSocketHandshaker, WebSocketServerService wsService,
            WebSocketConnectionManager connectionManager) {
        ResourceMethodType resourceFunction = ((ServiceType) wsService.getBalService().getType())
                .getResourceMethods()[0];
        String[] resourceParams = resourceFunction.getResourcePath();

        BObject inRequest = ValueCreatorUtils.createRequestObject();
        BObject inRequestEntity = ValueCreatorUtils.createEntityObject();
        HttpCarbonRequest httpCarbonMessage = webSocketHandshaker.getHttpCarbonRequest();
        String errMsg = "No resource found for path " + httpCarbonMessage.getRequestUrl();
        String subPath = (String) httpCarbonMessage.getProperty(HttpConstants.SUB_PATH);
        String[] subPaths = new String[0];
        ArrayList<String> pathParamArr = new ArrayList<>();
        if (!subPath.isEmpty()) {
            subPath = sanitizeSubPath(subPath).substring(1);
            subPaths = subPath.split(WebSocketConstants.BACK_SLASH);
        }
        if (!resourceParams[0].equals(".")) {
            if (resourceParams.length != subPaths.length) {
                webSocketHandshaker.cancelHandshake(404, errMsg);
                return;
            }
            int i = 0;
            for (String resourceParam : resourceParams) {
                if (resourceParam.equals("*")) {
                    pathParamArr.add(subPaths[i]);
                } else if (!resourceParam.equals(subPaths[i])) {
                    webSocketHandshaker.cancelHandshake(404, errMsg);
                    return;
                }
                i++;
            }
        }
        Type[] parameterTypes = resourceFunction.getParameterTypes();
        Map<String, HeaderParam> allHeaderParams = new HashMap<>();
        Map<String, QueryParam> allQueryParams = new HashMap<>();
        for (int index = pathParamArr.size(); index < parameterTypes.length; index++) {
            try {
                String paramName = resourceFunction.getParamNames()[index];
                String paramType = resourceFunction.getParameterTypes()[index].getName();
                BMap annotations = (BMap) resourceFunction.getAnnotation(
                        StringUtils.fromString(PARAM_ANNOT_PREFIX + paramName));
                if (annotations == null && paramType.equals(HttpConstants.REQUEST)) {
                    continue;
                } else if (annotations != null) {
                    Object[] annotationsKeys = annotations.getKeys();
                    for (Object objKey : annotationsKeys) {
                        String key = ((BString) objKey).getValue();
                        if (key.contains(HEADER_ANNOTATION)) {
                            Type parameterType = resourceFunction.getParameterTypes()[index];
                            HeaderParam headerParam = new HeaderParam();
                            BMap mapValue = annotations.getMapValue(StringUtils.fromString(
                                    WebSocketConstants.BALLERINA_HTTP_HEADER));
                            Object headerName = mapValue.get(HttpConstants.ANN_FIELD_NAME);
                            if (headerName instanceof BString) {
                                String value = ((BString) headerName).getValue();
                                headerParam.setHeaderName(value);
                            } else {
                                // if the name field is not stated, use the param token as header key
                                headerParam.setHeaderName(paramName);
                            }
                            allHeaderParams.put(paramName, headerParam);
                            headerParam.init(parameterType);
                        }
                    }
                } else {
                    Type parameterType = resourceFunction.getParameterTypes()[index];
                    validateQueryParam(index, resourceFunction, parameterType, allQueryParams);
                }
            } catch (WebSocketConnectorException e) {
                webSocketHandshaker.cancelHandshake(404, e.getMessage());
            }
        }

        HttpUtil.populateInboundRequest(inRequest, inRequestEntity, httpCarbonMessage);
        Object[] bValues = new Object[parameterTypes.length * 2];
        int index = 0;
        int pathParamIndex = 0;
        int paramIndex = 0;
        try {
            BMap<BString, Object> urlQueryParams = getQueryParams(httpCarbonMessage.getProperty(
                    HttpConstants.RAW_QUERY_STR));
            for (Type param : parameterTypes) {
                String typeName = param.getName();
                String paramName = resourceFunction.getParamNames()[paramIndex];
                if (allHeaderParams.get(paramName) != null) {
                    HeaderParam headerParam = allHeaderParams.get(paramName);
                    HttpHeaders httpHeaders = httpCarbonMessage.getHeaders();
                    String token = headerParam.getHeaderName();
                    List<String> headerValues = httpHeaders.getAll(token);
                    if (headerValues.isEmpty()) {
                        if (headerParam.isNilable()) {
                            index = createBvaluesForNillable(bValues, index);
                            continue;
                        } else {
                            webSocketHandshaker.cancelHandshake(404, errMsg);
                        }
                    }
                    if (headerParam.getTypeTag() == ARRAY_TAG) {
                        String[] headerArray = headerValues.toArray(new String[0]);
                        bValues[index++] = StringUtils.fromStringArray(headerArray);
                    } else {
                        bValues[index++] = StringUtils.fromString(headerValues.get(0));
                    }
                    bValues[index++] = true;
                    paramIndex++;
                    continue;
                }
                if (pathParamArr.size() > paramIndex) {
                    switch (typeName) {
                        case WebSocketConstants.PARAM_TYPE_STRING:
                            bValues[index++] = StringUtils.fromString(pathParamArr.get(pathParamIndex++));
                            bValues[index++] = true;
                            break;
                        case WebSocketConstants.PARAM_TYPE_INT:
                            bValues[index++] = Long.parseLong(pathParamArr.get(pathParamIndex++));
                            bValues[index++] = true;
                            break;
                        case WebSocketConstants.PARAM_TYPE_FLOAT:
                            bValues[index++] = Double.parseDouble(pathParamArr.get(pathParamIndex++));
                            bValues[index++] = true;
                            break;
                        case WebSocketConstants.PARAM_TYPE_BOOLEAN:
                            bValues[index++] = Boolean.parseBoolean(pathParamArr.get(pathParamIndex++));
                            bValues[index++] = true;
                            break;
                        default:
                            break;
                    }
                } else {
                    if (typeName.equals(HttpConstants.REQUEST)) {
                        bValues[index++] = inRequest;
                        bValues[index++] = true;
                    } else {
                        Object queryValue = urlQueryParams.get(StringUtils.fromString(paramName));
                        QueryParam queryParam = allQueryParams.get(paramName);
                        BArray queryValueArr = (BArray) queryValue;
                        String qParamTypeName = queryParam.getType().getName();
                        switch (qParamTypeName) {
                            case WebSocketConstants.PARAM_TYPE_STRING:
                                if (queryValue == null) {
                                    if (queryParam.isNilable()) {
                                        index = createBvaluesForNillable(bValues, index);
                                        break;
                                    } else {
                                        reportError(paramName);
                                    }
                                }
                                bValues[index++] = StringUtils.fromString(String.valueOf((queryValueArr)
                                        .getBString(0).getValue()));
                                bValues[index++] = true;
                                break;
                            case WebSocketConstants.PARAM_TYPE_INT:
                                if (queryValue == null) {
                                    if (queryParam.isNilable()) {
                                        index = createBvaluesForNillable(bValues, index);
                                        break;
                                    } else {
                                        reportError(paramName);
                                    }
                                }
                                bValues[index++] = Long.parseLong(String.valueOf((queryValueArr)
                                        .getBString(0).getValue()));
                                bValues[index++] = true;
                                break;
                            case WebSocketConstants.PARAM_TYPE_BOOLEAN:
                                if (queryValue == null) {
                                    if (queryParam.isNilable()) {
                                        index = createBvaluesForNillable(bValues, index);
                                        break;
                                    } else {
                                        reportError(paramName);
                                    }
                                }
                                bValues[index++] = Boolean.parseBoolean(String.valueOf((queryValueArr)
                                        .getBString(0).getValue()));
                                bValues[index++] = true;
                                break;
                            case WebSocketConstants.PARAM_TYPE_FLOAT:
                                if (queryValue == null) {
                                    if (queryParam.isNilable()) {
                                        index = createBvaluesForNillable(bValues, index);
                                        break;
                                    } else {
                                        reportError(paramName);
                                    }
                                }
                                bValues[index++] = Double.parseDouble(String.valueOf((queryValueArr)
                                        .getBString(0).getValue()));
                                bValues[index++] = true;
                                break;
                            case WebSocketConstants.PARAM_TYPE_DECIMAL:
                                if (queryValue == null) {
                                    if (queryParam.isNilable()) {
                                        index = createBvaluesForNillable(bValues, index);
                                        break;
                                    } else {
                                        reportError(paramName);
                                    }
                                }
                                bValues[index++] = ValueCreator.createDecimalValue((String.valueOf((queryValueArr)
                                        .getBString(0).getValue())));
                                bValues[index++] = true;
                                break;
                            default:
                                break;
                        }
                    }
                }
                paramIndex++;
            }
        } catch (NumberFormatException | WebSocketConnectorException e) {
            webSocketHandshaker.cancelHandshake(404, errMsg);
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put(HttpConstants.INBOUND_MESSAGE, httpCarbonMessage);
        BObject balservice = wsService.getBalService();
        String function = resourceFunction.getName();
        if (isIsolated(balservice, function)) {
            wsService.getRuntime().invokeMethodAsyncConcurrently(balservice, function, null,
                    ModuleUtils.getOnUpgradeMetaData(),
                    new OnUpgradeResourceCallback(webSocketHandshaker, wsService,
                            connectionManager),
                    properties, PredefinedTypes.TYPE_ANY, bValues);
        } else {
            wsService.getRuntime().invokeMethodAsyncSequentially(balservice, function, null,
                    ModuleUtils.getOnUpgradeMetaData(),
                    new OnUpgradeResourceCallback(webSocketHandshaker, wsService,
                            connectionManager),
                    properties, PredefinedTypes.TYPE_ANY, bValues);
        }
    }

    private static int createBvaluesForNillable(Object[] bValues, int index) {
        bValues[index++] = null;
        bValues[index++] = true;
        return index;
    }

    private static void reportError(String paramName) throws WebSocketConnectorException {
        throw new WebSocketConnectorException("No query param value found for '" + paramName + "'");
    }

    public static BMap<BString, Object> getQueryParams(Object rawQueryString) throws WebSocketConnectorException {
        BMap<BString, Object> queryParams = ValueCreator.createMapValue(MAP_TYPE);

        if (rawQueryString != null) {
            try {
                URIUtil.populateQueryParamMap((String) rawQueryString, queryParams);
            } catch (UnsupportedEncodingException e) {
                throw new WebSocketConnectorException("Error while retrieving query param from message: "
                        + e.getMessage());
            }
        }
        return queryParams;
    }

    private static String sanitizeSubPath(String subPath) {
        if (WebSocketConstants.BACK_SLASH.equals(subPath)) {
            return subPath;
        }
        if (!subPath.startsWith(WebSocketConstants.BACK_SLASH)) {
            subPath = HttpConstants.DEFAULT_BASE_PATH + subPath;
        }
        subPath = subPath.endsWith(WebSocketConstants.BACK_SLASH) ?
                subPath.substring(0, subPath.length() - 1) : subPath;
        return subPath;
    }

    private static void validateQueryParam(int index, ResourceMethodType balResource, Type parameterType,
                                           Map<String, QueryParam> allQueryParams) throws WebSocketConnectorException {
        if (parameterType instanceof UnionType) {
            List<Type> memberTypes = ((UnionType) parameterType).getMemberTypes();
            int size = memberTypes.size();
            if (size > 2 || !parameterType.isNilable()) {
                throw new WebSocketConnectorException("Invalid query param type '" + parameterType.getName());
            }
            for (Type type : memberTypes) {
                if (type.getTag() == TypeTags.NULL_TAG) {
                    continue;
                }
                QueryParam queryParam = new QueryParam(type,  true);
                allQueryParams.put(balResource.getParamNames()[index], queryParam);
                break;
            }
        } else {
            QueryParam queryParam = new QueryParam(parameterType, false);
            allQueryParams.put(balResource.getParamNames()[index], queryParam);
        }
    }

    public static void dispatchOnOpen(WebSocketConnection webSocketConnection, BObject webSocketCaller,
            WebSocketServerService wsService) {
        MethodType onOpenResource = null;
        Object dispatchingService = wsService.getWsService(webSocketConnection.getChannelId());
        MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService)
                .getType())).getMethods();
        BObject balService = (BObject) dispatchingService;
        for (MethodType remoteFunc : remoteFunctions) {
            if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_OPEN)) {
                onOpenResource = remoteFunc;
                break;
            }
        }
        if (onOpenResource != null) {
            executeOnOpenResource(wsService, balService, onOpenResource, webSocketCaller, webSocketConnection);
        } else {
            webSocketConnection.readNextFrame();
        }
    }

    private static void executeOnOpenResource(WebSocketService wsService, BObject balService, MethodType onOpenResource,
            BObject webSocketEndpoint, WebSocketConnection webSocketConnection) {
        Type[] parameterTypes = onOpenResource.getParameterTypes();
        Object[] bValues = new Object[parameterTypes.length * 2];
        if (parameterTypes.length > 0) {
            bValues[0] = webSocketEndpoint;
            bValues[1] = true;
        }
        WebSocketConnectionInfo connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection,
                webSocketEndpoint);
        try {
            executeResource(wsService, balService, new WebSocketResourceCallback(connectionInfo,
                            WebSocketConstants.RESOURCE_NAME_ON_OPEN),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_OPEN, ModuleUtils.getOnOpenMetaData());
        } catch (IllegalAccessException e) {
            observeError(connectionInfo, ERROR_TYPE_RESOURCE_INVOCATION,
                    WebSocketConstants.RESOURCE_NAME_ON_OPEN, e.getMessage());
        }
    }

    public static void dispatchOnText(WebSocketConnectionInfo connectionInfo, WebSocketTextMessage textMessage) {
        WebSocketObservabilityUtil.observeOnMessage(MESSAGE_TYPE_TEXT, connectionInfo);
        try {
            WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
            WebSocketService wsService = connectionInfo.getService();
            MethodType onTextMessageResource = null;
            BObject balservice;
            BObject wsEndpoint = connectionInfo.getWebSocketEndpoint();
            Object dispatchingService = wsService.getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            balservice = (BObject) dispatchingService;
            MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType())).getMethods();
            for (MethodType remoteFunc : remoteFunctions) {
                if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE)) {
                    onTextMessageResource = remoteFunc;
                    break;
                }
            }
            if (onTextMessageResource == null) {
                webSocketConnection.readNextFrame();
                return;
            }
            Type[] parameterTypes = onTextMessageResource.getParameterTypes();
            Object[] bValues = new Object[parameterTypes.length * 2];

            boolean finalFragment = textMessage.isFinalFragment();
            WebSocketConnectionInfo.StringAggregator stringAggregator = connectionInfo
                    .createIfNullAndGetStringAggregator();
            if (finalFragment) {
                stringAggregator.appendAggregateString(textMessage.getText());
                int index = 0;
                try {
                    for (Type param : parameterTypes) {
                        int typeTag = param.getTag();
                        switch (typeTag) {
                            case OBJECT_TYPE_TAG:
                                bValues[index++] = wsEndpoint;
                                bValues[index++] = true;
                                break;
                            case STRING_TAG:
                                bValues[index++] = StringUtils.fromString(stringAggregator.getAggregateString());
                                bValues[index++] = true;
                                break;
                            case TypeTags.XML_TAG:
                                BXml bxml = XmlUtils.parse(stringAggregator.getAggregateString());
                                bValues[index++] = bxml;
                                bValues[index++] = true;
                                break;
                            case TypeTags.TABLE_TAG:
                                Object table = FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(
                                        stringAggregator.getAggregateString()),
                                        ValueCreator.createTypedescValue(param));
                                bValues[index++] = table;
                                bValues[index++] = true;
                                break;
                            default:
                                Object value = CloneWithType.convert(param, JsonUtils.parse(
                                        stringAggregator.getAggregateString()));
                                if (value instanceof BError) {
                                    sendDataBindingError(webSocketConnection, ((BError) value).getMessage());
                                    return;
                                }
                                bValues[index++] = value;
                                bValues[index++] = true;
                                break;
                        }
                    }
                } catch (BError | NumberFormatException error) {
                    sendDataBindingError(webSocketConnection, error.getMessage());
                    return;
                }
                executeResource(wsService, balservice,
                        new WebSocketResourceCallback(connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE),
                        bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE,
                        ModuleUtils.getOnTextMetaData());
                stringAggregator.resetAggregateString();
            } else {
                stringAggregator.appendAggregateString(textMessage.getText());
                webSocketConnection.readNextFrame();
            }
        } catch (IllegalAccessException e) {
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_TEXT, e.getMessage());
        }
    }

    private static void sendDataBindingError(WebSocketConnection webSocketConnection, String errorMessage) {
        if (errorMessage.length() > 100) {
            errorMessage = errorMessage.substring(0, 80) + "...";
        }
        webSocketConnection.terminateConnection(1003,
                String.format("data binding failed: %s", errorMessage));
    }

    public static void dispatchOnBinary(WebSocketConnectionInfo connectionInfo, WebSocketBinaryMessage binaryMessage) {
        WebSocketObservabilityUtil.observeOnMessage(MESSAGE_TYPE_BINARY,
                connectionInfo);
        try {
            WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
            WebSocketService wsService = connectionInfo.getService();
            MethodType onBinaryMessageResource = null;
            BObject balservice;
            BObject wsEndpoint = connectionInfo.getWebSocketEndpoint();
            Object dispatchingService = wsService.getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            balservice = (BObject) dispatchingService;
            MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType())).getMethods();
            for (MethodType remoteFunc : remoteFunctions) {
                if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_BINARY_MESSAGE)) {
                    onBinaryMessageResource = remoteFunc;
                    break;
                }
            }
            if (onBinaryMessageResource == null) {
                webSocketConnection.readNextFrame();
                return;
            }
            boolean finalFragment = binaryMessage.isFinalFragment();
            Type[] paramDetails = onBinaryMessageResource.getParameterTypes();
            Object[] bValues = new Object[paramDetails.length * 2];
            WebSocketConnectionInfo.ByteArrAggregator byteAggregator = connectionInfo
                    .createIfNullAndGetByteArrAggregator();
            if (finalFragment) {
                byteAggregator.appendAggregateArr(binaryMessage.getByteArray());
                createBvaluesForBarray(wsEndpoint, paramDetails, bValues, byteAggregator.getAggregateByteArr());
                executeResource(wsService, balservice, new WebSocketResourceCallback(connectionInfo,
                        WebSocketConstants.RESOURCE_NAME_ON_BINARY_MESSAGE), bValues, connectionInfo,
                        WebSocketConstants.RESOURCE_NAME_ON_BINARY_MESSAGE, ModuleUtils.getOnBinaryMetaData());
                byteAggregator.resetAggregateByteArr();
            } else {
                byteAggregator.appendAggregateArr(binaryMessage.getByteArray());
                webSocketConnection.readNextFrame();
            }
        } catch (IllegalAccessException | IOException e) {
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_BINARY, e.getMessage());
        }
    }

    public static void dispatchOnPingOnPong(WebSocketConnectionInfo connectionInfo,
            WebSocketControlMessage controlMessage, boolean server) {
        if (controlMessage.getControlSignal() == WebSocketControlSignal.PING) {
            WebSocketResourceDispatcher.dispatchOnPing(connectionInfo, controlMessage, server);
        } else if (controlMessage.getControlSignal() == WebSocketControlSignal.PONG) {
            WebSocketResourceDispatcher.dispatchOnPong(connectionInfo, controlMessage, server);
        }
    }

    private static void dispatchOnPing(WebSocketConnectionInfo connectionInfo, WebSocketControlMessage controlMessage,
            boolean server) {
        WebSocketObservabilityUtil.observeOnMessage(MESSAGE_TYPE_PING, connectionInfo);
        try {
            WebSocketService wsService = connectionInfo.getService();
            MethodType onPingMessageResource = null;
            BObject balservice = null;
            if (server) {
                Object dispatchingService = wsService
                        .getWsService(connectionInfo.getWebSocketConnection().getChannelId());
                balservice = (BObject) dispatchingService;
                MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType()))
                        .getMethods();
                for (MethodType remoteFunc : remoteFunctions) {
                    if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_PING)) {
                        onPingMessageResource = remoteFunc;
                        break;
                    }
                }
            } else {
                balservice = wsService.getBalService();
                onPingMessageResource = wsService.getResourceByName(WebSocketConstants.RESOURCE_NAME_ON_PING);
            }
            if (onPingMessageResource == null) {
                pongAutomatically(controlMessage);
                return;
            }
            Type[] paramTypes = onPingMessageResource.getParameterTypes();
            Object[] bValues = new Object[paramTypes.length * 2];
            createBvaluesForBarray(connectionInfo.getWebSocketEndpoint(), paramTypes, bValues,
                    controlMessage.getByteArray());
            executeResource(wsService, balservice, new WebSocketResourceCallback(
                            connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PING),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PING, ModuleUtils.getOnPingMetaData());
        } catch (Exception e) {
            //Observe error
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_PING, e.getMessage());
        }
    }

    private static void createBvaluesForBarray(BObject wsEndpoint, Type[] paramTypes, Object[] bValues,
            byte[] byteArray) {
        int index = 0;
        for (Type param : paramTypes) {
            int typeName = param.getTag();
            switch (typeName) {
            case OBJECT_TYPE_TAG:
                bValues[index++] = wsEndpoint;
                bValues[index++] = true;
                break;
            case ARRAY_TAG:
                bValues[index++] = ValueCreator.createArrayValue(byteArray);
                bValues[index++] = true;
                break;
            case INTERSECTION_TAG:
                bValues[index++] = ValueCreator.createReadonlyArrayValue(byteArray);
                bValues[index++] = true;
                break;
            default:
                break;
            }
        }
    }

    private static void dispatchOnPong(WebSocketConnectionInfo connectionInfo, WebSocketControlMessage controlMessage,
            boolean server) {
        WebSocketObservabilityUtil.observeOnMessage(MESSAGE_TYPE_PONG, connectionInfo);
        try {
            WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
            WebSocketService wsService = connectionInfo.getService();
            MethodType onPongMessageResource = null;
            BObject balservice;
            if (server) {
                Object dispatchingService = wsService
                        .getWsService(connectionInfo.getWebSocketConnection().getChannelId());
                balservice = (BObject) dispatchingService;
                MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService)
                        .getType())).getMethods();
                for (MethodType remoteFunc : remoteFunctions) {
                    if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_PONG)) {
                        onPongMessageResource = remoteFunc;
                        break;
                    }
                }
            } else {
                balservice = wsService.getBalService();
                onPongMessageResource = wsService.getResourceByName(WebSocketConstants.RESOURCE_NAME_ON_PONG);
            }
            if (onPongMessageResource == null) {
                webSocketConnection.readNextFrame();
                return;
            }
            Type[] paramDetails = onPongMessageResource.getParameterTypes();
            Object[] bValues = new Object[paramDetails.length * 2];
            createBvaluesForBarray(connectionInfo.getWebSocketEndpoint(), paramDetails, bValues,
                    controlMessage.getByteArray());
            executeResource(wsService, balservice, new WebSocketResourceCallback(
                            connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PONG),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PONG, ModuleUtils.getOnPongMetaData());
        } catch (Exception e) {
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_PONG, e.getMessage());
        }
    }

    public static void dispatchOnClose(WebSocketConnectionInfo connectionInfo, WebSocketCloseMessage closeMessage,
            boolean server) {
        WebSocketObservabilityUtil.observeOnMessage(MESSAGE_TYPE_CLOSE, connectionInfo);
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
            WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
            WebSocketService wsService = connectionInfo.getService();
            MethodType onCloseResource = null;
            int closeCode = closeMessage.getCloseCode();
            String closeReason = closeMessage.getCloseReason();
            BObject balservice = null;
            if (server) {
                Object dispatchingService = wsService
                        .getWsService(connectionInfo.getWebSocketConnection().getChannelId());
                balservice = (BObject) dispatchingService;
                MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService)
                        .getType())).getMethods();
                for (MethodType remoteFunc : remoteFunctions) {
                    if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_CLOSE)) {
                        onCloseResource = remoteFunc;
                        break;
                    }
                }
            } else {
                balservice = wsService.getBalService();
                onCloseResource = wsService.getResourceByName(WebSocketConstants.RESOURCE_NAME_ON_CLOSE);
            }
            if (onCloseResource == null) {
                finishConnectionClosureIfOpen(webSocketConnection, closeCode, connectionInfo);
                return;
            }

            Type[] paramDetails = onCloseResource.getParameterTypes();
            Object[] bValues = new Object[paramDetails.length * 2];
            int index = 0;
            for (Type param : paramDetails) {
                int typeName = param.getTag();
                switch (typeName) {
                case OBJECT_TYPE_TAG:
                    bValues[index++] = connectionInfo.getWebSocketEndpoint();
                    bValues[index++] = true;
                    break;
                case STRING_TAG:
                    bValues[index++] =
                            closeReason == null ? StringUtils.fromString("") : StringUtils.fromString(closeReason);
                    bValues[index++] = true;
                    break;
                case INT_TAG:
                    bValues[index++] = closeCode;
                    bValues[index++] = true;
                    break;
                default:
                    break;
                }
            }
            Callback onCloseCallback = new Callback() {
                @Override
                public void notifySuccess(Object result) {
                    finishConnectionClosureIfOpen(webSocketConnection, closeCode, connectionInfo);
                }

                @Override
                public void notifyFailure(BError error) {
                    error.printStackTrace();
                    finishConnectionClosureIfOpen(webSocketConnection, closeCode, connectionInfo);
                    //Observe error
                    observeError(connectionInfo, ERROR_TYPE_RESOURCE_INVOCATION,
                            WebSocketConstants.RESOURCE_NAME_ON_CLOSE, error.getMessage());
                }
            };
            executeResource(wsService, balservice, onCloseCallback, bValues, connectionInfo,
                    WebSocketConstants.RESOURCE_NAME_ON_CLOSE, ModuleUtils.getOnCloseMetaData());
        } catch (Exception e) {
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_CLOSE, e.getMessage());
        }
    }

    public static void finishConnectionClosureIfOpen(WebSocketConnection webSocketConnection, int closeCode,
            WebSocketConnectionInfo connectionInfo) {
        if (webSocketConnection.isOpen()) {
            ChannelFuture finishFuture;
            if (closeCode == WebSocketConstants.STATUS_CODE_FOR_NO_STATUS_CODE_PRESENT) {
                finishFuture = webSocketConnection.finishConnectionClosure();
            } else {
                finishFuture = webSocketConnection.finishConnectionClosure(closeCode, null);
            }
            finishFuture.addListener(closeFuture -> WebSocketUtil.setListenerOpenField(connectionInfo));
        }
    }

    public static void dispatchOnError(WebSocketConnectionInfo connectionInfo, Throwable throwable, boolean server) {
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        }
        WebSocketService webSocketService = connectionInfo.getService();
        MethodType onErrorResource = null;
        if (isUnexpectedError(throwable)) {
            log.error("Unexpected error", throwable);
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_TEXT, "Unexpected error");
        }
        BObject balservice = null;
        if (server) {
            Object dispatchingService;
            try {
                dispatchingService = webSocketService
                        .getWsService(connectionInfo.getWebSocketConnection().getChannelId());
                balservice = (BObject) dispatchingService;
                MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService)
                        .getType())).getMethods();
                for (MethodType remoteFunc : remoteFunctions) {
                    if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_ERROR)) {
                        onErrorResource = remoteFunc;
                        break;
                    }
                }
            } catch (IllegalAccessException ex) {
                connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
            }
        }
        if (onErrorResource == null) {
            ErrorCreator.createError(throwable.getCause()).printStackTrace();
            return;
        }

        Type[] paramDetails = onErrorResource.getParameterTypes();
        Object[] bValues = new Object[paramDetails.length * 2];

        int index = 0;
        for (Type param : paramDetails) {
            int typeName = param.getTag();
            switch (typeName) {
            case OBJECT_TYPE_TAG:
                bValues[index++] = connectionInfo.getWebSocketEndpoint();
                bValues[index++] = true;
                break;
            case ERROR_TAG:
                bValues[index++] = WebSocketUtil.createErrorByType(throwable);
                bValues[index++] = true;
                break;
            default:
                break;
            }
        }
        Callback onErrorCallback = new Callback() {
            @Override
            public void notifySuccess(Object result) {
                // Do nothing.
            }

            @Override
            public void notifyFailure(BError error) {
                error.printStackTrace();
                observeError(connectionInfo, ERROR_TYPE_RESOURCE_INVOCATION, WebSocketConstants.RESOURCE_NAME_ON_ERROR,
                        error.getMessage());
            }
        };
        executeResource(webSocketService, balservice, onErrorCallback, bValues, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_ON_ERROR, ModuleUtils.getOnErrorMetaData());
    }

    private static boolean isUnexpectedError(Throwable throwable) {
        return !(throwable instanceof CorruptedFrameException);
    }

    public static void dispatchOnIdleTimeout(WebSocketConnectionInfo connectionInfo) {
        try {
            WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
            WebSocketService wsService = connectionInfo.getService();
            MethodType onIdleTimeoutResource = null;
            Object dispatchingService = wsService.getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            BObject balservice = (BObject) dispatchingService;
            MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType())).getMethods();
            for (MethodType remoteFunc : remoteFunctions) {
                if (remoteFunc.getName().equals(WebSocketConstants.RESOURCE_NAME_ON_IDLE_TIMEOUT)) {
                    onIdleTimeoutResource = remoteFunc;
                    break;
                }
            }
            if (onIdleTimeoutResource == null) {
                return;
            }
            Type[] paramDetails = onIdleTimeoutResource.getParameterTypes();
            Object[] bValues = new Object[paramDetails.length * 2];
            if (paramDetails.length > 0) {
                bValues[0] = connectionInfo.getWebSocketEndpoint();
                bValues[1] = true;
            }
            Callback onIdleTimeoutCallback = new Callback() {
                @Override
                public void notifySuccess(Object result) {
                    // Do nothing.
                }

                @Override
                public void notifyFailure(BError error) {
                    error.printStackTrace();
                    WebSocketUtil.closeDuringUnexpectedCondition(webSocketConnection);
                }
            };
            executeResource(wsService, balservice, onIdleTimeoutCallback, bValues, connectionInfo,
                    WebSocketConstants.RESOURCE_NAME_ON_IDLE_TIMEOUT, ModuleUtils.getOnTimeoutMetaData());
        } catch (Exception e) {
            log.error("Error on idle timeout", e);
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_TEXT, e.getMessage());
        }
    }

    private static void pongAutomatically(WebSocketControlMessage controlMessage) {
        WebSocketConnection webSocketConnection = controlMessage.getWebSocketConnection();
        webSocketConnection.pong(controlMessage.getByteBuffer()).addListener(future -> {
            Throwable cause = future.cause();
            if (!future.isSuccess() && cause != null) {
                ErrorCreator.createError(cause).printStackTrace();
            }
            webSocketConnection.readNextFrame();
        });
    }

    private static void executeResource(WebSocketService wsService, BObject balservice, Callback callback,
            Object[] bValues, WebSocketConnectionInfo connectionInfo, String resource, StrandMetadata metaData) {
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            WebSocketObserverContext observerContext = new WebSocketObserverContext(connectionInfo);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            if (isIsolated(balservice, resource)) {
                wsService.getRuntime().invokeMethodAsyncConcurrently(balservice, resource, null, metaData, callback,
                        properties, PredefinedTypes.TYPE_ANY, bValues);
            } else {
                wsService.getRuntime().invokeMethodAsyncSequentially(balservice, resource, null, metaData, callback,
                        properties, PredefinedTypes.TYPE_ANY, bValues);
            }
        } else {
            if (isIsolated(balservice, resource)) {
                wsService.getRuntime().invokeMethodAsyncConcurrently(balservice, resource, null, metaData, callback,
                        null, PredefinedTypes.TYPE_ANY, bValues);
            } else {
                wsService.getRuntime().invokeMethodAsyncSequentially(balservice, resource, null, metaData, callback,
                        null, PredefinedTypes.TYPE_ANY, bValues);
            }
        }
        WebSocketObservabilityUtil.observeResourceInvocation(connectionInfo, resource);
    }

    private static boolean isIsolated(BObject serviceObj, String remoteMethod) {
        return serviceObj.getType().isIsolated() && serviceObj.getType().isIsolated(remoteMethod);
    }
}

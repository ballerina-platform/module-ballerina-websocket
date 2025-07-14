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

import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MapType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.RemoteMethodType;
import io.ballerina.runtime.api.types.ResourceMethodType;
import io.ballerina.runtime.api.types.ServiceType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.ValueUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BValue;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.stdlib.constraint.Constraints;
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
import org.ballerinalang.langlib.value.CloneReadOnly;
import org.ballerinalang.langlib.value.FromJsonString;
import org.ballerinalang.langlib.value.FromJsonStringWithType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.runtime.api.types.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.types.TypeTags.BYTE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.ERROR_TAG;
import static io.ballerina.runtime.api.types.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.types.TypeTags.INT_TAG;
import static io.ballerina.runtime.api.types.TypeTags.NULL_TAG;
import static io.ballerina.runtime.api.types.TypeTags.OBJECT_TYPE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.STRING_TAG;
import static io.ballerina.stdlib.websocket.WebSocketConstants.CONSTRAINT_VALIDATION;
import static io.ballerina.stdlib.websocket.WebSocketConstants.HEADER_ANNOTATION;
import static io.ballerina.stdlib.websocket.WebSocketConstants.PARAM_ANNOT_PREFIX;
import static io.ballerina.stdlib.websocket.WebSocketResourceCallback.isCloseFrameRecord;
import static io.ballerina.stdlib.websocket.WebSocketResourceCallback.sendCloseFrame;
import static io.ballerina.stdlib.websocket.WebSocketUtil.getBString;
import static io.ballerina.stdlib.websocket.WebSocketUtil.hasByteArrayType;
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
        ResourceMethodType resourceFunction = ((ServiceType) TypeUtils.getType(wsService.getBalService()))
                .getResourceMethods()[0];
        String[] resourcePath = resourceFunction.getResourcePath();
        List<String> resourceParams = Arrays.stream(resourcePath)
            .map(value ->
                (value.equals(WebSocketConstants.REST_PARAM_IDENTIFIER) ||
                 value.equals(WebSocketConstants.PATH_PARAM_IDENTIFIER))
                    ? value
                    : HttpUtil.unescapeAndEncodeValue(value)
            )
            .toList();

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
        if (!resourceParams.get(0).equals(WebSocketConstants.REST_PARAM_IDENTIFIER)) {
            if (resourceParams.size() != subPaths.length) {
                webSocketHandshaker.cancelHandshake(404, errMsg);
                return;
            }
            int i = 0;
            for (String resourceParam : resourceParams) {
                if (resourceParam.equals(WebSocketConstants.PATH_PARAM_IDENTIFIER)) {
                    pathParamArr.add(subPaths[i]);
                } else if (!resourceParam.equals(subPaths[i])) {
                    webSocketHandshaker.cancelHandshake(404, errMsg);
                    return;
                }
                i++;
            }
        }
        Parameter[] parameters = resourceFunction.getParameters();
        Map<String, HeaderParam> allHeaderParams = new HashMap<>();
        Map<String, QueryParam> allQueryParams = new HashMap<>();
        for (int index = pathParamArr.size(); index < parameters.length; index++) {
            try {
                String paramName = resourceFunction.getParameters()[index].name;
                String paramType = resourceFunction.getParameters()[index].type.getName();
                BMap annotations = (BMap) resourceFunction.getAnnotation(
                        StringUtils.fromString(PARAM_ANNOT_PREFIX + paramName));
                if (annotations == null && paramType.equals(HttpConstants.REQUEST)) {
                    continue;
                } else if (annotations != null) {
                    Object[] annotationsKeys = annotations.getKeys();
                    for (Object objKey : annotationsKeys) {
                        String key = ((BString) objKey).getValue();
                        if (key.contains(HEADER_ANNOTATION)) {
                            Type parameterType = resourceFunction.getParameters()[index].type;
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
                    Type parameterType = resourceFunction.getParameters()[index].type;
                    validateQueryParam(index, resourceFunction, parameterType, allQueryParams);
                }
            } catch (WebSocketConnectorException e) {
                webSocketHandshaker.cancelHandshake(404, e.getMessage());
            }
        }

        HttpUtil.populateInboundRequest(inRequest, inRequestEntity, httpCarbonMessage);
        Object[] bValues = new Object[parameters.length];
        int index = 0;
        int pathParamIndex = 0;
        int paramIndex = 0;
        try {
            BMap<BString, Object> urlQueryParams = getQueryParams(httpCarbonMessage.getProperty(
                    HttpConstants.RAW_QUERY_STR));
            for (Parameter param : parameters) {
                String typeName = param.type.getName();
                String paramName = param.name;
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
                        bValues[index++] = StringUtils.fromString(headerValues.getFirst());
                    }
                    paramIndex++;
                    continue;
                }
                if (pathParamArr.size() > paramIndex) {
                    switch (typeName) {
                        case WebSocketConstants.PARAM_TYPE_STRING:
                            bValues[index++] = StringUtils.fromString(pathParamArr.get(pathParamIndex++));
                            break;
                        case WebSocketConstants.PARAM_TYPE_INT:
                            bValues[index++] = Long.parseLong(pathParamArr.get(pathParamIndex++));
                            break;
                        case WebSocketConstants.PARAM_TYPE_FLOAT:
                            bValues[index++] = Double.parseDouble(pathParamArr.get(pathParamIndex++));
                            break;
                        case WebSocketConstants.PARAM_TYPE_BOOLEAN:
                            bValues[index++] = Boolean.parseBoolean(pathParamArr.get(pathParamIndex++));
                            break;
                        default:
                            break;
                    }
                } else {
                    if (typeName.equals(HttpConstants.REQUEST)) {
                        bValues[index++] = inRequest;
                    } else {
                        Object queryValue = urlQueryParams.get(StringUtils.fromString(paramName));
                        QueryParam queryParam = allQueryParams.get(paramName);
                        BArray queryValueArr = (BArray) queryValue;
                        Type qParamType = queryParam.getType();
                        if (queryValue == null) {
                            if (queryParam.isNilable()) {
                                index = createBvaluesForNillable(bValues, index);
                            } else {
                                reportQueryParamError(webSocketHandshaker, paramName);
                                return;
                            }
                        } else {
                            if (qParamType.getTag() == STRING_TAG) {
                                bValues[index++] = queryValueArr.getBString(0);
                            } else {
                                bValues[index++] = FromJsonStringWithType.fromJsonStringWithType(queryValueArr
                                        .getBString(0), ValueCreator.createTypedescValue(qParamType));
                            }
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
        OnUpgradeResourceCallback handler = new OnUpgradeResourceCallback(webSocketHandshaker, wsService,
                connectionManager);
        Thread.startVirtualThread(() -> {
            Object result;
            StrandMetadata strandMetadata = new StrandMetadata(isIsolated(balservice, function), properties);
            try {
                result = wsService.getRuntime().callMethod(balservice, function, strandMetadata, bValues);
                handler.notifySuccess(result);
            } catch (BError bError) {
                handler.notifyFailure(bError);
            }
        });
    }

    private static int createBvaluesForNillable(Object[] bValues, int index) {
        bValues[index++] = null;
        return index;
    }

    private static void reportQueryParamError(WebSocketHandshaker webSocketHandshaker, String paramName)
            throws WebSocketConnectorException {
        webSocketHandshaker.cancelHandshake(400, String.format("No query param value found for: %s", paramName));
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
                if (type.getTag() == NULL_TAG) {
                    continue;
                }
                QueryParam queryParam = new QueryParam(type,  true);
                allQueryParams.put(balResource.getParameters()[index].name, queryParam);
                break;
            }
        } else {
            QueryParam queryParam = new QueryParam(parameterType, false);
            allQueryParams.put(balResource.getParameters()[index].name, queryParam);
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
        Parameter[] parameters = onOpenResource.getParameters();
        Object[] bValues = new Object[parameters.length];
        if (parameters.length > 0) {
            bValues[0] = webSocketEndpoint;
        }
        WebSocketConnectionInfo connectionInfo = new WebSocketConnectionInfo(wsService, webSocketConnection,
                webSocketEndpoint);
        try {
            executeResource(wsService, balService, new WebSocketResourceCallback(connectionInfo,
                            WebSocketConstants.RESOURCE_NAME_ON_OPEN, wsService.getRuntime()),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_OPEN);
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
            WebSocketConnectionInfo.StringAggregator stringAggregator = connectionInfo
                    .createIfNullAndGetStringAggregator();
            stringAggregator.appendAggregateString(textMessage.getText());
            boolean finalFragment = textMessage.isFinalFragment();
            if (!finalFragment) {
                webSocketConnection.readNextFrame();
                return;
            }
            String dispatchingKey = ((WebSocketServerService) wsService).getDispatchingKey();
            Optional<String> dispatchingValue = getDispatchingValue(dispatchingKey, stringAggregator);
            Optional<String> customRemoteMethodName = dispatchingValue
                    .map(WebSocketResourceDispatcher::createCustomRemoteFunction);
            MethodType onTextMessageResource = null;
            BObject wsEndpoint = connectionInfo.getWebSocketEndpoint();
            Object dispatchingService = wsService.getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            Map<String, RemoteMethodType> dispatchingFunctions = wsService
                    .getDispatchingFunctions(connectionInfo.getWebSocketConnection().getChannelId());
            if (dispatchingValue.isPresent() && dispatchingFunctions.containsKey(dispatchingValue.get())) {
                onTextMessageResource = dispatchingFunctions.get(dispatchingValue.get());
            } else if (customRemoteMethodName.isPresent()
                    && dispatchingFunctions.containsKey(customRemoteMethodName.get())) {
                onTextMessageResource = dispatchingFunctions.get(customRemoteMethodName.get());
            } else if (dispatchingFunctions.containsKey(WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE)) {
                onTextMessageResource = dispatchingFunctions.get(WebSocketConstants.RESOURCE_NAME_ON_TEXT_MESSAGE);
            } else if (dispatchingFunctions.containsKey(WebSocketConstants.RESOURCE_NAME_ON_MESSAGE)) {
                onTextMessageResource = dispatchingFunctions.get(WebSocketConstants.RESOURCE_NAME_ON_MESSAGE);
            }
            boolean hasOnError = dispatchingFunctions.containsKey(WebSocketConstants.RESOURCE_NAME_ON_ERROR);
            String errorMethodName = null;
            boolean hasOnCustomError = false;
            if (onTextMessageResource != null) {
                errorMethodName = onTextMessageResource.getName() + "Error";
                hasOnCustomError = dispatchingFunctions.containsKey(errorMethodName);
            } else if (customRemoteMethodName.isPresent()) {
                errorMethodName = customRemoteMethodName.get() + "Error";
                hasOnCustomError = dispatchingFunctions.containsKey(errorMethodName);
            }
            if (onTextMessageResource == null) {
                stringAggregator.resetAggregateString();
                webSocketConnection.readNextFrame();
                return;
            }
            boolean validationEnabled = (boolean) wsService.getBalService().getNativeData(CONSTRAINT_VALIDATION);
            Parameter[] parameters = onTextMessageResource.getParameters();
            Object[] bValues = new Object[parameters.length];

            int index = 0;
            try {
                for (Parameter param : parameters) {
                    int typeTag = TypeUtils.getReferredType(param.type).getTag();
                    boolean readOnly = false;
                    Type paramType = param.type;
                    if (typeTag == INTERSECTION_TAG) {
                        List<Type> memberTypes = ((IntersectionType) param.type).getConstituentTypes();
                        if (invalidInputParams(webSocketConnection, param.type, memberTypes)) {
                            return;
                        }
                        readOnly = true;
                        for (Type type : memberTypes) {
                            if (type.getTag() == TypeTags.READONLY_TAG) {
                                continue;
                            }
                            paramType = type;
                            typeTag = type.getTag();
                            break;
                        }
                    }
                    Object bValue = getBvaluesForTextMessage(paramType, typeTag, wsEndpoint, stringAggregator);
                    if (bValue instanceof BError bError) {
                        handleError(connectionInfo, bError, hasOnCustomError, errorMethodName, hasOnError);
                        stringAggregator.resetAggregateString();
                        return;
                    }
                    if (readOnly) {
                        bValue = CloneReadOnly.cloneReadOnly(bValue);
                    }
                    if (typeTag != OBJECT_TYPE_TAG && validationEnabled) {
                        Object validationResult = Constraints.validate(bValue,
                                ValueCreator.createTypedescValue(paramType));
                        if (validationResult instanceof BError) {
                            BError validationErr = WebSocketUtil.createWebsocketErrorWithCause(
                                    String.format("data validation failed: %s", validationResult),
                                    WebSocketConstants.ErrorCode.PayloadValidationError, (BError) validationResult);
                            dispatchOnError(connectionInfo, validationErr, true);
                            stringAggregator.resetAggregateString();
                            return;
                        }
                    }
                    bValues[index++] = bValue;
                }
            } catch (BError error) {
                handleError(connectionInfo, error, hasOnCustomError, errorMethodName, hasOnError);
                stringAggregator.resetAggregateString();
                return;
            }
            executeResource(wsService, (BObject) dispatchingService,
                    new WebSocketResourceCallback(connectionInfo, onTextMessageResource.getName(),
                            wsService.getRuntime()), bValues, connectionInfo, onTextMessageResource.getName());
            stringAggregator.resetAggregateString();
        } catch (IllegalAccessException e) {
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_TEXT, e.getMessage());
        }
    }

    private static Object getBvaluesForTextMessage(Type param, int typeTag, BObject wsEndpoint,
                                                   WebSocketConnectionInfo.StringAggregator stringAggregator) {
        Object bValue;
        switch (typeTag) {
            case OBJECT_TYPE_TAG:
                bValue = wsEndpoint;
                break;
            case STRING_TAG:
                bValue = StringUtils.fromString(stringAggregator.getAggregateString());
                break;
            case TypeTags.XML_TAG:
                bValue = XmlUtils.parse(stringAggregator.getAggregateString());
                break;
            case TypeTags.RECORD_TYPE_TAG:
                bValue = ValueUtils.convert(JsonUtils.parse(stringAggregator.getAggregateString()),
                        param);
                break;
            case TypeTags.UNION_TAG:
                if (WebSocketUtil.hasStringType(param)) {
                    bValue = ValueUtils.convert(
                            StringUtils.fromString(stringAggregator.getAggregateString()), param);
                    break;
                }
                // fall through
            default:
                bValue = FromJsonStringWithType.fromJsonStringWithType(StringUtils.fromString(
                        stringAggregator.getAggregateString()),
                        ValueCreator.createTypedescValue(param));
                break;
        }
        return bValue;
    }

    private static void handleError(WebSocketConnectionInfo connectionInfo, BError error, boolean hasOnCustomError,
                                    String errorMethodName, boolean hasOnError) throws IllegalAccessException {
        if (hasOnCustomError) {
            dispatchOnCustomError(connectionInfo, error, errorMethodName);
        } else {
            handleDataBindingError(connectionInfo, hasOnError, error);
        }
    }

    private static void handleDataBindingError(WebSocketConnectionInfo connectionInfo,
                                               boolean hasOnError, BError bValue) throws IllegalAccessException {
        if (hasOnError) {
            dispatchOnError(connectionInfo, bValue, true);
        } else {
            sendDataBindingError(connectionInfo.getWebSocketConnection(), bValue.getMessage());
        }
    }

    private static Optional<String> getDispatchingValue(String dispatchingKey,
                                                              WebSocketConnectionInfo.StringAggregator
                                                                      stringAggregator) {
        return Optional.ofNullable(dispatchingKey)
                .flatMap(key -> {
                    try {
                        String dispatchingValue = ((BMap) FromJsonString.fromJsonString(
                                StringUtils.fromString(stringAggregator.getAggregateString())))
                                .getStringValue(StringUtils.fromString(dispatchingKey)).getValue();
                        return Optional.of(dispatchingValue);
                    } catch (RuntimeException e) {
                        return Optional.empty();
                    }
                });
    }

    public static String createCustomRemoteFunction(String dispatchingValue) {
        dispatchingValue = "on " + dispatchingValue;
        StringBuilder builder = new StringBuilder();
        String[] words = dispatchingValue.split("[\\W_]+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase(Locale.ENGLISH);
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1)
                        .toLowerCase(Locale.ENGLISH);
            }
            builder.append(word);
        }
        return builder.toString();
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
            Object dispatchingService = wsService.getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            balservice = (BObject) dispatchingService;
            MethodType[] remoteFunctions = ((ServiceType) (((BValue) dispatchingService).getType())).getMethods();
            for (MethodType remoteFunc : remoteFunctions) {
                String funcName = remoteFunc.getName();
                if (funcName.equals(WebSocketConstants.RESOURCE_NAME_ON_BINARY_MESSAGE) ||
                        funcName.equals(WebSocketConstants.RESOURCE_NAME_ON_MESSAGE)) {
                    onBinaryMessageResource = remoteFunc;
                    break;
                }
            }
            boolean hasOnError = Arrays.stream(remoteFunctions).anyMatch(remoteFunc -> remoteFunc.getName()
                    .equals(WebSocketConstants.RESOURCE_NAME_ON_ERROR));
            if (onBinaryMessageResource == null) {
                webSocketConnection.readNextFrame();
                return;
            }
            boolean finalFragment = binaryMessage.isFinalFragment();
            WebSocketConnectionInfo.ByteArrAggregator byteAggregator = connectionInfo
                    .createIfNullAndGetByteArrAggregator();
            if (finalFragment) {
                byteAggregator.appendAggregateArr(binaryMessage.getByteArray());
                createBvaluesForBinary(onBinaryMessageResource, balservice, connectionInfo,
                        byteAggregator.getAggregateByteArr(), webSocketConnection, wsService, hasOnError);
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
            Parameter[] parameters = onPingMessageResource.getParameters();
            Object[] bValues = new Object[parameters.length];
            createBvaluesForBarray(connectionInfo.getWebSocketEndpoint(), parameters, bValues,
                    controlMessage.getByteArray());
            executeResource(wsService, balservice, new WebSocketResourceCallback(
                            connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PING, wsService.getRuntime()),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PING);
        } catch (Exception e) {
            //Observe error
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_PING, e.getMessage());
        }
    }

    private static void createBvaluesForBinary(MethodType onBinaryMessageResource, BObject balservice,
                                               WebSocketConnectionInfo connectionInfo, byte[] byteArray,
                                               WebSocketConnection webSocketConnection, WebSocketService wsService,
                                               boolean hasOnError) throws IllegalAccessException {
        BObject wsEndpoint = connectionInfo.getWebSocketEndpoint();
        boolean validationEnabled = (boolean) wsService.getBalService().getNativeData(CONSTRAINT_VALIDATION);
        Parameter[] parameters = onBinaryMessageResource.getParameters();
        Object[] bValues = new Object[parameters.length];
        int index = 0;
        try {
            for (Parameter param : parameters) {
                int typeName = TypeUtils.getReferredType(param.type).getTag();
                boolean readOnly = false;
                typeName = getTypeName(param.type, typeName);
                Type paramType = param.type;
                if (typeName == INTERSECTION_TAG) {
                    List<Type> memberTypes = ((IntersectionType) param.type).getConstituentTypes();
                    if (invalidInputParams(webSocketConnection, param.type, memberTypes)) {
                        return;
                    }
                    readOnly = true;
                    for (Type type : memberTypes) {
                        if (type.getTag() == TypeTags.READONLY_TAG) {
                            continue;
                        }
                        paramType = type;
                        typeName = getTypeName(type, type.getTag());
                        break;
                    }
                }
                Object bValue;
                switch (typeName) {
                    case OBJECT_TYPE_TAG:
                        bValue = wsEndpoint;
                        break;
                    case BYTE_TAG:
                        bValue = ValueCreator.createArrayValue(byteArray);
                        break;
                    case STRING_TAG:
                        bValue = getBString(byteArray);
                        break;
                    case TypeTags.XML_TAG:
                        bValue = XmlUtils.parse(getBString(byteArray));;
                        break;
                    case TypeTags.RECORD_TYPE_TAG:
                        bValue = ValueUtils.convert(JsonUtils.parse(getBString(byteArray)), paramType);
                        break;
                    case TypeTags.UNION_TAG:
                        if (hasByteArrayType(paramType)) {
                            bValue = ValueUtils.convert(ValueCreator.createArrayValue(byteArray), paramType);
                            break;
                        }
                        // fall through
                    default:
                        bValue = FromJsonStringWithType.fromJsonStringWithType(getBString(byteArray),
                                ValueCreator.createTypedescValue(paramType));
                        break;
                }
                if (bValue instanceof BError) {
                    handleDataBindingError(connectionInfo, hasOnError, (BError) bValue);
                    return;
                }
                if (readOnly) {
                    bValue = CloneReadOnly.cloneReadOnly(bValue);
                }
                if (typeName != OBJECT_TYPE_TAG && validationEnabled) {
                    Object validationResult = Constraints.validate(bValue,
                            ValueCreator.createTypedescValue(paramType));
                    if (validationResult instanceof BError) {
                        BError validationErr = WebSocketUtil.createWebsocketErrorWithCause(
                                String.format("data validation failed: %s", validationResult),
                                WebSocketConstants.ErrorCode.PayloadValidationError, (BError) validationResult);
                        dispatchOnError(connectionInfo, validationErr, true);
                        return;
                    }
                }
                bValues[index++] = bValue;
            }
            executeResource(wsService, balservice, new WebSocketResourceCallback(connectionInfo,
                            onBinaryMessageResource.getName(), wsService.getRuntime()), bValues, connectionInfo,
                    onBinaryMessageResource.getName());
        } catch (IllegalAccessException | BError e) {
            if (e instanceof BError) {
                handleDataBindingError(connectionInfo, hasOnError, (BError) e);
                return;
            }
            observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_BINARY, e.getMessage());
        }
    }

    private static int getTypeName(Type param, int typeName) {
        if (typeName == ARRAY_TAG) {
            if (param.toString().equals(WebSocketConstants.BYTE_ARRAY)) {
                typeName = BYTE_TAG;
            }
        }
        return typeName;
    }

    private static boolean invalidInputParams(WebSocketConnection webSocketConnection, Type param,
                                              List<Type> memberTypes) {
        if (memberTypes.size() > 2) {
            sendDataBindingError(webSocketConnection, "invalid param type '" + param.getName() +
                    "': only readonly intersection is allowed");
            return true;
        }
        return false;
    }

    private static void createBvaluesForBarray(BObject wsEndpoint, Parameter[] parameters, Object[] bValues,
            byte[] byteArray) {
        int index = 0;
        for (Parameter param : parameters) {
            int typeName = param.type.getTag();
            switch (typeName) {
            case OBJECT_TYPE_TAG:
                bValues[index++] = wsEndpoint;
                break;
            case ARRAY_TAG:
                bValues[index++] = ValueCreator.createArrayValue(byteArray);
                break;
            case INTERSECTION_TAG:
                bValues[index++] = ValueCreator.createReadonlyArrayValue(byteArray);
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
            Parameter[] paramDetails = onPongMessageResource.getParameters();
            Object[] bValues = new Object[paramDetails.length];
            createBvaluesForBarray(connectionInfo.getWebSocketEndpoint(), paramDetails, bValues,
                    controlMessage.getByteArray());
            executeResource(wsService, balservice, new WebSocketResourceCallback(
                            connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PONG, wsService.getRuntime()),
                    bValues, connectionInfo, WebSocketConstants.RESOURCE_NAME_ON_PONG);
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

            Parameter[] paramDetails = onCloseResource.getParameters();
            Object[] bValues = new Object[paramDetails.length];
            int index = 0;
            for (Parameter param : paramDetails) {
                int typeName = param.type.getTag();
                switch (typeName) {
                case OBJECT_TYPE_TAG:
                    bValues[index++] = connectionInfo.getWebSocketEndpoint();
                    break;
                case STRING_TAG:
                    bValues[index++] =
                            closeReason == null ? StringUtils.fromString("") : StringUtils.fromString(closeReason);
                    break;
                case INT_TAG:
                    bValues[index++] = closeCode;
                    break;
                default:
                    break;
                }
            }
            Handler onCloseCallback = new Handler() {
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
                    WebSocketConstants.RESOURCE_NAME_ON_CLOSE);
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
                onErrorResource = getErrorMethod((BValue) dispatchingService,
                        WebSocketConstants.RESOURCE_NAME_ON_ERROR);
            } catch (IllegalAccessException ex) {
                connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
            }
        }
        if (onErrorResource == null) {
            ErrorCreator.createError(throwable.getCause()).printStackTrace();
            return;
        }

        Parameter[] paramDetails = onErrorResource.getParameters();
        Object[] bValues = new Object[paramDetails.length];

        getErrorBValues(connectionInfo, throwable, paramDetails, bValues);
        Handler onErrorCallback = getOnErrorCallback(connectionInfo);
        executeResource(webSocketService, balservice, onErrorCallback, bValues, connectionInfo,
                WebSocketConstants.RESOURCE_NAME_ON_ERROR);
    }

    public static void dispatchOnCustomError(WebSocketConnectionInfo connectionInfo, Throwable throwable,
                                             String errorMethodName) {
        try {
            WebSocketUtil.setListenerOpenField(connectionInfo);
            WebSocketService webSocketService = connectionInfo.getService();
            Object dispatchingService = webSocketService
                    .getWsService(connectionInfo.getWebSocketConnection().getChannelId());
            BObject balservice = (BObject) dispatchingService;
            MethodType onErrorRemoteFunction = getErrorMethod((BValue) dispatchingService,
                    errorMethodName);
            Parameter[] paramDetails = onErrorRemoteFunction.getParameters();
            Object[] bValues = new Object[paramDetails.length];

            getErrorBValues(connectionInfo, throwable, paramDetails, bValues);
            Handler onErrorCallback = getOnErrorCallback(connectionInfo);
            executeResource(webSocketService, balservice, onErrorCallback, bValues, connectionInfo,
                    errorMethodName);
        } catch (IllegalAccessException e) {
            connectionInfo.getWebSocketEndpoint().set(WebSocketConstants.LISTENER_IS_OPEN_FIELD, false);
        }
    }

    private static Handler getOnErrorCallback(WebSocketConnectionInfo connectionInfo) {
        return new Handler() {
            @Override
            public void notifySuccess(Object result) {
                try {
                    WebSocketConnection webSocketConnection = connectionInfo.getWebSocketConnection();
                    if (isCloseFrameRecord(result)) {
                        sendCloseFrame(result, connectionInfo);
                    } else if (webSocketConnection.isOpen()) {
                        webSocketConnection.readNextFrame();
                    }
                } catch (IllegalAccessException e) {
                    observeError(connectionInfo, ERROR_TYPE_MESSAGE_RECEIVED, MESSAGE_TYPE_TEXT, e.getMessage());
                }
            }

            @Override
            public void notifyFailure(BError error) {
                error.printStackTrace();
                observeError(connectionInfo, ERROR_TYPE_RESOURCE_INVOCATION, WebSocketConstants.RESOURCE_NAME_ON_ERROR,
                        error.getMessage());
            }
        };
    }

    private static void getErrorBValues(WebSocketConnectionInfo connectionInfo, Throwable throwable,
                                        Parameter[] paramDetails, Object[] bValues) {
        int index = 0;
        for (Parameter param : paramDetails) {
            int typeName = param.type.getTag();
            switch (typeName) {
            case OBJECT_TYPE_TAG:
                bValues[index++] = connectionInfo.getWebSocketEndpoint();
                break;
            case ERROR_TAG:
                bValues[index++] = WebSocketUtil.createErrorByType(throwable);
                break;
            default:
                break;
            }
        }
    }

    private static MethodType getErrorMethod(BValue dispatchingService, String onErrorFunctionName) {
        MethodType[] remoteFunctions = ((ServiceType) (dispatchingService.getType())).getMethods();
        for (MethodType remoteFunc : remoteFunctions) {
            if (remoteFunc.getName().equals(onErrorFunctionName)) {
                return remoteFunc;
            }
        }
        return null;
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
            Parameter[] paramDetails = onIdleTimeoutResource.getParameters();
            Object[] bValues = new Object[paramDetails.length];
            if (paramDetails.length > 0) {
                bValues[0] = connectionInfo.getWebSocketEndpoint();
            }
            Handler onIdleTimeoutCallback = new Handler() {
                @Override
                public void notifySuccess(Object result) {
                    if (isCloseFrameRecord(result)) {
                        sendCloseFrame(result, connectionInfo);
                    }
                }

                @Override
                public void notifyFailure(BError error) {
                    error.printStackTrace();
                    WebSocketUtil.closeDuringUnexpectedCondition(webSocketConnection);
                }
            };
            executeResource(wsService, balservice, onIdleTimeoutCallback, bValues, connectionInfo,
                    WebSocketConstants.RESOURCE_NAME_ON_IDLE_TIMEOUT);
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

    private static void executeResource(WebSocketService wsService, BObject balservice, Handler callback,
            Object[] bValues, WebSocketConnectionInfo connectionInfo, String resource) {
        Thread.startVirtualThread(() -> {
            Object result;
            try {
                Map<String, Object> properties = ModuleUtils.getProperties(resource);
                if (ObserveUtils.isTracingEnabled()) {
                    WebSocketObserverContext observerContext = new WebSocketObserverContext(connectionInfo);
                    properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
                }
                StrandMetadata strandMetadata = new StrandMetadata(isIsolated(balservice, resource), properties);
                result = wsService.getRuntime().callMethod(balservice, resource, strandMetadata, bValues);
                callback.notifySuccess(result);
                WebSocketObservabilityUtil.observeResourceInvocation(connectionInfo, resource);
            } catch (BError bError) {
                callback.notifyFailure(bError);
            }
        });
    }

    private static boolean isIsolated(BObject serviceObj, String remoteMethod) {
        ObjectType serviceObjType = (ObjectType) TypeUtils.getReferredType(serviceObj.getType());
        return serviceObjType.isIsolated() && serviceObjType.isIsolated(remoteMethod);
    }
}

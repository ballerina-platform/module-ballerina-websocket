/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnectorException;

/**
 * Represents an inbound request query parameter details.
 */
public class QueryParam {

    private final int typeTag;
    private final boolean nilable;
    private final Type type;

    QueryParam(Type type, boolean nilable) throws WebSocketConnectorException {
        this.type = type;
        this.typeTag = type.getTag();
        this.nilable = nilable;
        validateQueryParamType();
    }

    private void validateQueryParamType() throws WebSocketConnectorException {
        if (isValidBasicType(typeTag) || (typeTag == TypeTags.ARRAY_TAG && isValidBasicType(
                ((ArrayType) type).getElementType().getTag()))) {
            return;
        }
        throw new WebSocketConnectorException("Incompatible query parameter type: '" + type.getName() + "'");
    }

    private boolean isValidBasicType(int typeTag) {
        return typeTag == TypeTags.STRING_TAG || typeTag == TypeTags.INT_TAG || typeTag == TypeTags.FLOAT_TAG ||
                typeTag == TypeTags.BOOLEAN_TAG || typeTag == TypeTags.DECIMAL_TAG;
    }

    public boolean isNilable() {
        return this.nilable;
    }

    public Type getType() {
        return this.type;
    }
}


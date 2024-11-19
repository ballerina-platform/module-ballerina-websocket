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

import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.stdlib.http.transport.contract.websocket.WebSocketConnectorException;

import java.util.List;

/**
 * Represents an inbound request header parameter details.
 */
public class HeaderParam {
    private int typeTag;
    private boolean nilable;
    private Type type;
    private String headerName;

    public void init(Type type) throws WebSocketConnectorException {
        this.type = type;
        this.typeTag = type.getTag();
        validateHeaderParamType();
    }

    private void validateHeaderParamType() throws WebSocketConnectorException {
        if (this.type instanceof UnionType) {
            List<Type> memberTypes = ((UnionType) this.type).getMemberTypes();
            int size = memberTypes.size();
            if (size > 2 || !this.type.isNilable()) {
                throw new WebSocketConnectorException("Invalid header param type '" + this.type.getName() +
                        "': a string or an array of a string can only be union with '()'." +
                        "Eg: string|() or string[]|()");
            }
            this.nilable = true;
            for (Type type : memberTypes) {
                if (type.getTag() == TypeTags.NULL_TAG) {
                    continue;
                }
                validateBasicType(type);
                break;
            }
        } else {
            validateBasicType(this.type);
        }
    }

    private void validateBasicType(Type type) throws WebSocketConnectorException {
        if (isValidBasicType(type.getTag()) || (type.getTag() == TypeTags.ARRAY_TAG && isValidBasicType(
                ((ArrayType) type).getElementType().getTag()))) {
            this.typeTag = type.getTag();
            return;
        }
        throw new WebSocketConnectorException("Incompatible header parameter type: '" + type.getName() + "'. " +
                "expected: string or string[]");
    }

    private boolean isValidBasicType(int typeTag) {
        return typeTag == TypeTags.STRING_TAG;
    }

    public int getTypeTag() {
        return this.typeTag;
    }

    public boolean isNilable() {
        return this.nilable;
    }

    public String getHeaderName() {
        return headerName;
    }

    void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}

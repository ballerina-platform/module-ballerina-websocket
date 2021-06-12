// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/auth;
import ballerina/http;
import ballerina/jballerina.java;
import ballerina/jwt;
import ballerina/oauth2;

// This function is used for declarative auth design, where the authentication/authorization decision is taken by
// reading the auth annotations provided in service and the `Authorization` header taken with an interop call.
// This function is injected to the first lines of an websocket resource function. Then the logic will be executed
// during the runtime.
// If this function returns `()`, it will be moved to the execution of business logic, else there will be a 401/403
// response sent. The execution flow will be broken by panic with a distinct error.
# Uses for declarative auth design, where the authentication/authorization decision is taken
# by reading the auth annotations provided in service/resource and the `Authorization` header of request.
# 
# + serviceRef - The service reference where the resource locates
public isolated function authenticateResource(Service serviceRef) {
    ListenerAuthConfig[]? authConfig = getServiceAuthConfig(serviceRef);
    if (authConfig is ()) {
        return;
    }
    string|http:HeaderNotFoundError header = getAuthorizationHeader();
    if (header is string) {
        http:Unauthorized|http:Forbidden? result = tryAuthenticate(<ListenerAuthConfig[]>authConfig, header);
        if (result is http:Unauthorized) {
            notifyFailure(result.status.code);
        } else if (result is http:Forbidden) {
            notifyFailure(result.status.code);
        }
    } else {
        notifyFailure(401);
    }
}

isolated function tryAuthenticate(ListenerAuthConfig[] authConfig, string header) returns http:Unauthorized|http:Forbidden? {
    foreach ListenerAuthConfig config in authConfig {
        if (config is FileUserStoreConfigWithScopes) {
            http:ListenerFileUserStoreBasicAuthHandler handler = new(config.fileUserStoreConfig);
            auth:UserDetails|http:Unauthorized authn = handler.authenticate(header);
            string|string[]? scopes = config?.scopes;
            if (authn is auth:UserDetails) {
                if (scopes is string|string[]) {
                    http:Forbidden? authz = handler.authorize(authn, scopes);
                    return authz;
                }
                return;
            }
        } else if (config is LdapUserStoreConfigWithScopes) {
            http:ListenerLdapUserStoreBasicAuthHandler handler = new(config.ldapUserStoreConfig);
            auth:UserDetails|http:Unauthorized authn = handler->authenticate(header);
            string|string[]? scopes = config?.scopes;
            if (authn is auth:UserDetails) {
                if (scopes is string|string[]) {
                    http:Forbidden? authz = handler->authorize(authn, scopes);
                    return authz;
                }
                return;
            }
        } else if (config is JwtValidatorConfigWithScopes) {
            http:ListenerJwtAuthHandler handler = new(config.jwtValidatorConfig);
            jwt:Payload|http:Unauthorized authn = handler.authenticate(header);
            string|string[]? scopes = config?.scopes;
            if (authn is jwt:Payload) {
                if (scopes is string|string[]) {
                    http:Forbidden? authz = handler.authorize(authn, scopes);
                    return authz;
                }
                return;
            }
        } else {
            // Here, config is OAuth2IntrospectionConfigWithScopes
            http:ListenerOAuth2Handler handler = new(config.oauth2IntrospectionConfig);
            oauth2:IntrospectionResponse|http:Unauthorized|http:Forbidden auth = handler->authorize(header, config?.scopes);
            if (auth is oauth2:IntrospectionResponse) {
                return;
            } else if (auth is http:Forbidden) {
                return auth;
            }
        }
    }
    http:Unauthorized unauthorized = {};
    return unauthorized;
}

isolated function getServiceAuthConfig(Service serviceRef) returns ListenerAuthConfig[]? {
    typedesc<any> serviceTypeDesc = typeof serviceRef;
    var serviceAnnotation = serviceTypeDesc.@ServiceConfig;
    if (serviceAnnotation is ()) {
        return;
    }
    WSServiceConfig serviceConfig = <WSServiceConfig>serviceAnnotation;
    return serviceConfig?.auth;
}

isolated function notifyFailure(int responseCode) {
    // This panic is added to break the execution of the implementation inside the resource function after there is
    // an authn/authz failure and responded with 401/403 internally.
    panic error(responseCode.toString() + " received by auth desugar.");
}

isolated function getAuthorizationHeader() returns string|http:HeaderNotFoundError = @java:Method {
    'class: "org.ballerinalang.net.websocket.WebSocketUtil"
} external;

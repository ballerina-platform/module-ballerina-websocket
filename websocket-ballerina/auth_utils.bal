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
import ballerina/jwt;
import ballerina/log;
import ballerina/oauth2;

// Call relevant auth handling function based on the provided configurations
isolated function initClientAuthHandler(ClientConfiguration config) returns ClientAuthError? {
    ClientAuthConfig? authConfig = config.auth;
    if (authConfig is ()) {
       return;
    } else if (authConfig is CredentialsConfig) {
        check handleClientBasicAuth(authConfig, config);
    } else if (authConfig is BearerTokenConfig) {
        handleClientBearerTokenAuth(authConfig, config);
    } else if (authConfig is JwtIssuerConfig) {
        check handleClientSelfSignedJwtAuth(authConfig, config);
    } else {
        check handleClientOAuth2(authConfig, config);
    }
}

isolated function handleClientSelfSignedJwtAuth(JwtIssuerConfig config, ClientConfiguration clientConfig)
                     returns ClientAuthError? {
    jwt:ClientSelfSignedJwtAuthProvider provider = new(config);
    string|jwt:Error jwtToken = provider.generateToken();
    if (jwtToken is jwt:Error) {
        return prepareClientAuthError("Failed to enrich request with JWT.", jwtToken);
    } else {
        setAuthHeader(clientConfig, AUTH_SCHEME_BEARER, jwtToken);
    }
}

isolated function handleClientBasicAuth(CredentialsConfig config, ClientConfiguration clientConfig)
                       returns ClientAuthError? {
    auth:ClientBasicAuthProvider provider = new(config);
    string|auth:Error basicAuthToken = provider.generateToken();
    if (basicAuthToken is auth:Error) {
        return prepareClientAuthError("Failed to enrich request with Basic Auth token.", basicAuthToken);
    } else {
        setAuthHeader(clientConfig, AUTH_SCHEME_BASIC, basicAuthToken);
    }
}

isolated function handleClientBearerTokenAuth(BearerTokenConfig config, ClientConfiguration clientConfig) {
    setAuthHeader(clientConfig, AUTH_SCHEME_BEARER, config.token);
}

isolated function handleClientOAuth2(OAuth2GrantConfig config, ClientConfiguration clientConfig)
                        returns ClientAuthError? {
    oauth2:ClientOAuth2Provider provider = new(config);
    string|oauth2:Error oauthToken = provider.generateToken();
    if (oauthToken is oauth2:Error) {
        return prepareClientAuthError("Failed to enrich request with OAuth2 token.", oauthToken);
    } else {
        setAuthHeader(clientConfig, AUTH_SCHEME_BEARER, oauthToken);
    }
}

isolated function setAuthHeader(ClientConfiguration clientConfig, string authScheme, string token) {
    map<string> headers = clientConfig[CUSTOM_HEADERS];
    headers[AUTH_HEADER] = authScheme + " " + token;
    clientConfig[CUSTOM_HEADERS] = headers;
}

// Logs and prepares the `error` as an `websocket:ClientAuthError`.
isolated function prepareClientAuthError(string message, error? err = ()) returns ClientAuthError {
    log:printError(message, err = err);
    if (err is error) {
        return error ClientAuthError(message, err);
    }
    return error ClientAuthError(message);
}

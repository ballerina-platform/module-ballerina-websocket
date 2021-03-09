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
import ballerina/oauth2;

# Represents the Authorization header name.
public const string AUTH_HEADER = "Authorization";

# The prefix used to denote the Basic authentication scheme.
public const string AUTH_SCHEME_BASIC = "Basic";

# The prefix used to denote the Bearer authentication scheme.
public const string AUTH_SCHEME_BEARER = "Bearer";

# Defines the authentication configurations for the WebSocket client.
public type ClientAuthConfig CredentialsConfig|BearerTokenConfig|JwtIssuerConfig|OAuth2GrantConfig;

const string CUSTOM_HEADERS = "customHeaders";

# Represents JWT issuer configurations for JWT authentication.
public type JwtIssuerConfig record {|
    *jwt:IssuerConfig;
|};

# Represents credentials for Basic Auth authentication.
public type CredentialsConfig record {|
    *auth:CredentialsConfig;
|};

# Represents token for Bearer token authentication.
#
# + token - Bearer token for authentication
public type BearerTokenConfig record {|
    string token;
|};

# Represents OAuth2 client credentials grant configurations for OAuth2 authentication.
public type OAuth2ClientCredentialsGrantConfig record {|
    *oauth2:ClientCredentialsGrantConfig;
|};

# Represents OAuth2 password grant configurations for OAuth2 authentication.
public type OAuth2PasswordGrantConfig record {|
    *oauth2:PasswordGrantConfig;
|};

# Represents OAuth2 direct token configurations for OAuth2 authentication.
public type OAuth2DirectTokenConfig record {|
    *oauth2:DirectTokenConfig;
|};

# Represents OAuth2 grant configurations for OAuth2 authentication.
public type OAuth2GrantConfig OAuth2ClientCredentialsGrantConfig|OAuth2PasswordGrantConfig|OAuth2DirectTokenConfig;

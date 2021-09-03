// // Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
// //
// // WSO2 Inc. licenses this file to you under the Apache License,
// // Version 2.0 (the "License"); you may not use this file except
// // in compliance with the License.
// // You may obtain a copy of the License at
// //
// // http://www.apache.org/licenses/LICENSE-2.0
// //
// // Unless required by applicable law or agreed to in writing,
// // software distributed under the License is distributed on an
// // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// // KIND, either express or implied.  See the License for the
// // specific language governing permissions and limitations
// // under the License.

// import ballerina/test;
// import ballerina/http;

// @test:Config {}
// public function testEmptyForKeystore() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 path: "",
//                 password: "ballerina"
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "KeyStore file location must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an keystore file not found error");
//     }
// }

// @test:Config {}
// public function testEmptyPasswordForKeystore() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 path: KEYSTORE_PATH,
//                 password: ""
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "KeyStore password must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an keystore password not found error");
//     }
// }

// @test:Config {}
// public function testEmptyCertFile() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 keyFile: "tests/certsAndKeys/private.key",
//                 certFile: ""
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "Certificate file location must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an cert file not found error");
//     }
// }

// @test:Config {}
// public function testEmptyKeyFile() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 keyFile: "",
//                 certFile: "tests/certsAndKeys/public.crt"
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "Private key file location must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an key file not found error");
//     }
// }

// @test:Config {}
// public function testEmptyTrustore() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 keyFile: KEYSTORE_PATH,
//                 certFile: "tests/certsAndKeys/public.crt"
//             },
//             mutualSsl: {
//                 verifyClient: http:REQUIRE,
//                 cert: {
//                     path: "",
//                     password: "ballerina"
//                 }
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "TrustStore file location must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an trust store not found error");
//     }
// }

// @test:Config {}
// public function testEmptyTrustorePassword() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 keyFile: KEYSTORE_PATH,
//                 certFile: "tests/certsAndKeys/public.crt"
//             },
//             mutualSsl: {
//                 verifyClient: http:REQUIRE,
//                 cert: {
//                     path: TRUSTSTORE_PATH,
//                     password: ""
//                 }
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "TrustStore password must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an trust store password not found error");
//     }
// }

// @test:Config {}
// public function testTrustedCertFile() returns Error? {
//     Listener|Error l17 = new(21005, {
//         secureSocket: {
//             key: {
//                 keyFile: KEYSTORE_PATH,
//                 certFile: "tests/certsAndKeys/public.crt"
//             },
//             mutualSsl: {
//                 verifyClient: http:REQUIRE,
//                 cert: ""
//             }
//         }
//     });
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "Certificate file location must be provided for secure connection");
//     } else {
//         test:assertFail("Expected an trusted cert file not found error");
//     }
// }

// @test:Config {}
// public function testListenerPortNotDefined() returns Error? {
//     Listener|Error l17 = new(0);
//     if (l17 is Error) {
//         test:assertEquals(l17.message(), "Listener port is not defined");
//     } else {
//         test:assertFail("Expected an listener port is not defined error");
//     }
// }

// @test:Config {}
// public function testListenerIncorrectIdleTimeout() returns Error? {
//     Listener|Error l17 = new(9090, {timeout: -1});
//     if (l17 is Error) {
//         test:assertEquals(l17.message(),
//                     "Idle timeout cannot be negative. If you want to disable the timeout please use value 0");
//     } else {
//         test:assertFail("Expected an incorrect timeout defined error");
//     }
// }

# Changelog
This file contains all the notable changes done to the Ballerina WebSocket package through the releases.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- [Introduce write time out for WebSocket client](https://github.com/ballerina-platform/ballerina-standard-library/issues/1609)
- [Add OAuth2 JWT bearer grant type support](https://github.com/ballerina-platform/ballerina-standard-library/issues/1788)
- [Implement retry for WebSocket client](https://github.com/ballerina-platform/ballerina-standard-library/issues/1715)
- [Improve the 'get' resource to include header and query param values](https://github.com/ballerina-platform/ballerina-standard-library/issues/1737)
- [Add resource code snippet generation code action for tooling](https://github.com/ballerina-platform/ballerina-standard-library/issues/1896)

## [1.2.0-beta.2] - 2021-07-07

### Added
- [Implement declarative auth design for upgrade service](https://github.com/ballerina-platform/ballerina-standard-library/issues/1405)
- [Make WebSocket caller isolated](https://github.com/ballerina-platform/ballerina-standard-library/issues/1589)

### Fixed
- [Remove the countdown latches in the implementation](https://github.com/ballerina-platform/ballerina-standard-library/issues/1385)
- [Fix the issue of not sending the handshake cancel response when panicked from the upgrade service](https://github.com/ballerina-platform/ballerina-standard-library/issues/1439)
- [Fix the client handshake timeout not working](https://github.com/ballerina-platform/ballerina-standard-library/issues/1478)

## [1.2.0-beta.1] - 2021-05-06

### Fixed
- [Fix the listener initialization with inline configs compiler plugin error](https://github.com/ballerina-platform/ballerina-standard-library/issues/1304)
- [Fix the dispatching to remote functions issue when observability is enabled](https://github.com/ballerina-platform/ballerina-standard-library/issues/1313)

## [1.2.0-alpha8] - 2021-04-22

### Added
- [Add compiler plugin validation for WebSocket](https://github.com/ballerina-platform/ballerina-standard-library/issues/778)

### Fixed
- [Improve error message when creating the WebSocket client with an invalid URL](https://github.com/ballerina-platform/ballerina-standard-library/issues/1142)

## [1.2.0-alpha5] - 2021-03-19

### Added
- [Introduce auth support for the WebSocket client](https://github.com/ballerina-platform/ballerina-standard-library/issues/820)
- [Introduce HTTP cookie support for the WebSocket client](https://github.com/ballerina-platform/ballerina-standard-library/issues/978)
- [Introduce support to send text, binary, and pong messages by returning them from the remote methods](https://github.com/ballerina-platform/ballerina-standard-library/issues/1033)

### Changed
- [Make the websocket:Caller optional in WebSocket service remote functions](https://github.com/ballerina-platform/ballerina-standard-library/issues/1033)
- [Update SecureSocket API](https://github.com/ballerina-platform/ballerina-standard-library/issues/1068)
- [Convert all the timeout configurations to decimal](https://github.com/ballerina-platform/ballerina-standard-library/issues/1024)

### Removed
- Remove the support for the `websocket:AsyncClient`
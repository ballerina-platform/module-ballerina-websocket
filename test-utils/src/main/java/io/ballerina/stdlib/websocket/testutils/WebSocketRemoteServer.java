/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.websocket.testutils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple WebSocket server for Test cases.
 */
public final class WebSocketRemoteServer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketRemoteServer.class);

    private final int port;
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private boolean sslEnabled = false;

    public WebSocketRemoteServer(int port) {
        this.port = port;
    }

    public WebSocketRemoteServer(int port, boolean sslEnabled) {
        this.port = port;
        this.sslEnabled = sslEnabled;
    }

    public void run() throws InterruptedException {
        log.info("Starting websocket remote server at '" + port + "'");
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(5);

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new WebSocketRemoteServerInitializer(sslEnabled));
        bootstrap.bind(port).sync();
    }

    public static void stop() {
        log.info("Shutting down websocket remote server at '" + 21078 + "'");
        try {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            log.error("Error occurred when shutting down the server");
        }
    }

    public static void initiateServer() {
        WebSocketRemoteServer remoteServer = new WebSocketRemoteServer(21078);
        try {
            remoteServer.run();
        } catch (InterruptedException e) {
            log.error("Failed to start the server");
        }
    }
}

/*
 * Copyright 2017 (c) SecureSky-Tech, Inc.
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package tcpecho.netty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * copied and customized from netty 4.1 io.netty.example.telnet package.
 * original copyright: Copyright 2012 The Netty Project
 * original licens : APL 2.0
 * 
 * @see http://netty.io/4.1/xref/io/netty/example/telnet/package-summary.html
 */
public class NettyEchoClient {
    final Logger LOG = LoggerFactory.getLogger(NettyEchoClient.class);
    final InetSocketAddress connectTo;

    public NettyEchoClient(InetSocketAddress connectTo) {
        this.connectTo = connectTo;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new NettyEchoClientInitializer());

            // Start the connection attempt.
            Channel ch = b.connect(connectTo).sync().channel();
            LOG.info("connected to {}", connectTo);

            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (;;) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line + "\r\n");

                // If user typed the 'bye' command, wait until the server closes
                // the connection.
                if ("bye".equals(line.toLowerCase())) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            group.shutdownGracefully();
        }

    }

    static class NettyEchoClientInitializer extends ChannelInitializer<SocketChannel> {
        private static final StringDecoder DECODER = new StringDecoder();
        private static final StringEncoder ENCODER = new StringEncoder();

        // annotated as sharable handler :)
        private static final NettyEchoClientHandler CLIENT_HANDLER = new NettyEchoClientHandler();

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // Add the text line codec combination first,
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            // the encoder and decoder are static as these are sharable
            pipeline.addLast(DECODER);
            pipeline.addLast(ENCODER);

            // and then business logic.
            pipeline.addLast(CLIENT_HANDLER);
        }
    }

    @Sharable
    static class NettyEchoClientHandler extends SimpleChannelInboundHandler<String> {
        final Logger LOG = LoggerFactory.getLogger(NettyEchoClientHandler.class);

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOG.info("connected to {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            LOG.info("received [{}] from {}", msg, ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOG.info("disconnected from {}", ctx.channel().remoteAddress());
        }
    }

}

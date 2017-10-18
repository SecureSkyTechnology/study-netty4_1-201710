package tcpecho.netty;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * copied and customized from netty 4.1 io.netty.example.telnet package.
 * original copyright: Copyright 2012 The Netty Project
 * original licens : APL 2.0
 * 
 * @see http://netty.io/4.1/xref/io/netty/example/telnet/package-summary.html
 */
public class NettyEchoServer {
    final int port;
    final boolean isSingleThread;
    final long sleepms;
    final boolean isSeparated;

    public NettyEchoServer(final int port, final boolean isSingleThread) {
        this(port, isSingleThread, 0L, false);
    }

    public NettyEchoServer(final int port, final boolean isSingleThread, final long sleepms,
            final boolean isSeparated) {
        this.port = port;
        this.isSingleThread = isSingleThread;
        this.sleepms = sleepms;
        this.isSeparated = isSeparated;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = null;
        if (!this.isSingleThread) {
            workerGroup = new NioEventLoopGroup();
        }
        EventLoopGroup separatedGroup = null;
        if (this.isSeparated) {
            separatedGroup = new DefaultEventLoopGroup(5);
        }
        try {
            ServerBootstrap b = new ServerBootstrap();
            if (this.isSingleThread) {
                b.group(bossGroup);
            } else {
                b.group(bossGroup, workerGroup);
            }
            b.channel(NioServerSocketChannel.class);
            b.handler(new LoggingHandler(LogLevel.INFO));
            b.childHandler(new NettyEchoServerInitializer(sleepms, separatedGroup));
            b.bind(port).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            if (!this.isSingleThread) {
                workerGroup.shutdownGracefully();
            }
            if (this.isSeparated) {
                separatedGroup.shutdownGracefully();
            }
        }
    }

    static class NettyEchoServerInitializer extends ChannelInitializer<SocketChannel> {
        // annotated as sharable handler :)
        private static final StringDecoder DECODER = new StringDecoder();
        private static final StringEncoder ENCODER = new StringEncoder();
        private static final LoggingHandler LOGGER = new LoggingHandler("chlog", LogLevel.INFO);

        final long sleepms;
        final EventLoopGroup separatedGroup;

        public NettyEchoServerInitializer(final long sleepms, final EventLoopGroup separatedGroup) {
            this.sleepms = sleepms;
            this.separatedGroup = separatedGroup;
        }

        @Override
        public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // Add the text line codec combination first,
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            // the encoder and decoder are static as these are sharable
            pipeline.addLast(DECODER);
            pipeline.addLast(ENCODER);
            pipeline.addLast(LOGGER);

            // and then business logic.
            NettyEchoServerHandler serverHandler = new NettyEchoServerHandler(sleepms);
            if (Objects.isNull(separatedGroup)) {
                pipeline.addLast(serverHandler);
            } else {
                pipeline.addLast(separatedGroup, serverHandler);
            }
        }
    }

    static class NettyEchoServerHandler extends SimpleChannelInboundHandler<String> {
        final Logger LOG = LoggerFactory.getLogger(NettyEchoServerHandler.class);
        final long sleepms;

        public NettyEchoServerHandler(final long sleepms) {
            this.sleepms = sleepms;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOG.info("connected from {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
            LOG.info("received [{}] -> echo <> {}", request, ctx.channel().remoteAddress());
            // Generate and write a response.
            String response = request + "\r\n";
            if (0L < sleepms) {
                StringBuilder sb = new StringBuilder(request.length() + 2);
                for (int i = 0; i < request.length(); i++) {
                    char c = request.charAt(i);
                    sb.append(c);
                    LOG.info("sleepy ... '{}'", c);
                    try {
                        Thread.sleep(sleepms);
                    } catch (InterruptedException ignore) {
                    }
                }
                sb.append("\r\n");
                response = sb.toString();
            }
            boolean close = false;
            if ("bye".equals(request.toLowerCase())) {
                close = true;
            }
            ChannelFuture future = ctx.write(response);
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
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

package tcpecho.plainjava;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiThreadChannelServer {
    final Logger LOG = LoggerFactory.getLogger(MultiThreadChannelServer.class);
    final int port;

    public MultiThreadChannelServer(final int port) {
        this.port = port;
    }

    public void start() throws IOException {

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(port), 10);
        LOG.info("server bound to port {}", port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    serverChannel.close();
                } catch (IOException ignore) {
                }
                LOG.info("server listening socket closed.");
            }
        });

        try {
            while (true) {
                SocketChannel socketChannel = serverChannel.accept();
                new Thread() {
                    @Override
                    public void run() {
                        SocketAddress sa = socketChannel.socket().getRemoteSocketAddress();
                        LOG.info("server accepted from {}", sa);
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        try {
                            while (true) {
                                buffer.clear();
                                int readlen = socketChannel.read(buffer);
                                if (readlen < 0) {
                                    break;
                                }
                                buffer.flip();
                                String readstr =
                                    StandardCharsets.UTF_8.newDecoder().decode(buffer.duplicate()).toString();
                                LOG.info("received [{}] -> echo <> {}", readstr, sa);
                                socketChannel.write(buffer);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (Objects.nonNull(socketChannel)) {
                                try {
                                    socketChannel.close();
                                    LOG.info("server connection closed from {}", sa);
                                } catch (IOException ignore) {
                                }
                            }
                        }
                    }
                }.start();
            }
        } catch (IOException ignore) {
            // ignore accept() interruption via close() call from JVM shutdown hook 
        }
    }
}

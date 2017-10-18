package tcpecho.plainjava;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see http://www.techscore.com/tech/Java/JavaSE/NIO/5-2/
 * @see http://www.javainthebox.net/laboratory/JDK1.4/NewIO/SocketChannel/SocketChannel.html
 * @see http://itpro.nikkeibp.co.jp/article/COLUMN/20060515/237871/
 */
public class SingleThreadSelectorServer {
    final Logger LOG = LoggerFactory.getLogger(SingleThreadSelectorServer.class);
    final int port;

    public SingleThreadSelectorServer(final int port) {
        this.port = port;
    }

    static class EchoChannelContext {
        final Queue<ByteBuffer> queue = new LinkedList<>(); // no-need for thread safety :)

        public void push(ByteBuffer newBuffer) {
            queue.offer(newBuffer);
        }

        public ByteBuffer pop() {
            return queue.poll();
        }
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        serverChannel.bind(new InetSocketAddress(port), 10);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
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
            while (selector.select() > 0) {
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (key.isAcceptable()) {
                        doAccept(selector, key);
                    } else if (key.isReadable()) {
                        doRead(key);
                    } else if (key.isWritable() && key.isValid()) {
                        doWrite(key);
                    }
                }
            }
        } catch (IOException ignore) {
            ignore.printStackTrace();
            // ignore accept() interruption via close() call from JVM shutdown hook 
        }
    }

    private void doAccept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverChannel.accept();
        SocketAddress sa = socketChannel.socket().getRemoteSocketAddress();
        LOG.info("server accepted from {}", sa);
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, new EchoChannelContext());
    }

    private void doRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress sa = channel.socket().getRemoteSocketAddress();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        int readlen = channel.read(buffer);
        if (readlen < 0) {
            LOG.info("server connection closed from {}", sa);
            channel.close();
            return;
        }
        buffer.flip();
        String readstr = StandardCharsets.UTF_8.newDecoder().decode(buffer.duplicate()).toString();
        LOG.info("received [{}] <- {}", readstr, sa);

        EchoChannelContext ctx = (EchoChannelContext) key.attachment();
        ctx.push(buffer);

        if (key.interestOps() != (SelectionKey.OP_READ | SelectionKey.OP_WRITE)) {
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }

    }

    private void doWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress sa = channel.socket().getRemoteSocketAddress();
        EchoChannelContext ctx = (EchoChannelContext) key.attachment();
        ByteBuffer buffer = ctx.pop();
        if (Objects.nonNull(buffer)) {
            String str = StandardCharsets.UTF_8.newDecoder().decode(buffer.duplicate()).toString();
            LOG.info("send [{}] -> {}", str, sa);
            channel.write(buffer);
        } else {
            key.interestOps(SelectionKey.OP_READ);
        }
    }
}

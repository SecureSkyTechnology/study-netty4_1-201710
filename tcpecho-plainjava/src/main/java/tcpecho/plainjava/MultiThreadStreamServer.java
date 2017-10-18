package tcpecho.plainjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiThreadStreamServer {
    final Logger LOG = LoggerFactory.getLogger(MultiThreadStreamServer.class);
    final int port;

    public MultiThreadStreamServer(final int port) {
        this.port = port;
    }

    public void start() throws IOException {

        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port), 10);
        LOG.info("server bound to port {}", port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    serverSocket.close();
                } catch (IOException ignore) {
                }
                LOG.info("server listening socket closed.");
            }
        });

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread() {
                    @Override
                    public void run() {
                        SocketAddress sa = socket.getRemoteSocketAddress();
                        LOG.info("server accepted from {}", sa);
                        try {
                            BufferedReader socketReader =
                                new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
                            String line;
                            while ((line = socketReader.readLine()) != null) {
                                LOG.info("received [{}] -> echo <> {}", line, sa);
                                socketWriter.println(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (Objects.nonNull(socket)) {
                                try {
                                    socket.close();
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

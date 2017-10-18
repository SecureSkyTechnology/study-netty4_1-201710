package tcpecho.plainjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleEchoClient {
    final Logger LOG = LoggerFactory.getLogger(ConsoleEchoClient.class);
    final InetSocketAddress connectTo;

    public ConsoleEchoClient(InetSocketAddress connectTo) {
        this.connectTo = connectTo;
    }

    public void start() throws IOException {
        Socket socket = new Socket();
        socket.connect(connectTo);
        LOG.info("connected to {}", connectTo);
        BufferedReader sockReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter sockWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String consoleLine = consoleReader.readLine();
            if (Objects.isNull(consoleLine) || consoleLine.trim().length() == 0) {
                break;
            }
            sockWriter.println(consoleLine);
            String receivedLine = sockReader.readLine();
            if (receivedLine != null) {
                System.out.println(receivedLine);
            } else {
                break;
            }
        }
        LOG.info("connection close.");
        socket.close();
    }
}

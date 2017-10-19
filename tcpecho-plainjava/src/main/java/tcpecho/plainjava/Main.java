package tcpecho.plainjava;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

public class Main {

    static void usage() {
        System.out.println("usage: ");
        System.out.println("client <address> <port>");
        System.out.println("stream-server-singlethread <port>");
        System.out.println("stream-server-multithread <port>");
        System.out.println("channel-server-singlethread <port>");
        System.out.println("channel-server-multithread <port>");
        System.out.println("selector-server-singlethread <port>");
        System.out.println("note: all server listen 0.0.0.0 and specified <port>.");
    }

    static void invoke(String args[]) throws Exception {
        if (args.length < 2) {
            System.exit(-1);
        }
        final String component = args[0];
        if ("client".equals(component) && args.length == 3) {
            final String addr = args[1];
            final int port = Integer.parseInt(args[2]);
            new ConsoleEchoClient(new InetSocketAddress(addr, port)).start();
        } else {
            final int port = Integer.parseInt(args[1]);
            switch (component) {
            case "stream-server-singlethread":
                new SingleThreadStreamServer(port).start();
                break;
            case "stream-server-multithread":
                new MultiThreadStreamServer(port).start();
                break;
            case "channel-server-singlethread":
                new SingleThreadChannelServer(port).start();
                break;
            case "channel-server-multithread":
                new MultiThreadChannelServer(port).start();
                break;
            case "selector-server-singlethread":
                new SingleThreadSelectorServer(port).start();
                break;
            default:
                System.exit(-1);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 2) {
            usage();
            System.out.print("Enter component and args: ");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                args = br.readLine().trim().split("\\s");
                // call invoke() before closing System.in for console client
                invoke(args);
            }
        } else {
            invoke(args);
        }
    }
}

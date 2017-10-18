package tcpecho.netty;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

/**
 * copied and customized from netty 4.1 io.netty.example.telnet package.
 * original copyright: Copyright 2012 The Netty Project
 * original licens : APL 2.0
 * 
 * @see http://netty.io/4.1/xref/io/netty/example/telnet/package-summary.html
 */
public class Main {

    static void usage() {
        System.out.println("usage: ");
        System.out.println("client <address> <port>");
        System.out.println("netty-server-singlethread <port>");
        System.out.println("netty-server-multithread <port>");
        System.out.println("netty-server-sleepy-singlethread <port> <sleep_milisec>");
        System.out.println("netty-server-sleepy-singlethread-separated <port> <sleep_milisec>");
        System.out.println("netty-server-sleepy-multithread <port> <sleep_milisec>");
        System.out.println("netty-server-sleepy-multithread-separated <port> <sleep_milisec>");
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
            new NettyEchoClient(new InetSocketAddress(addr, port)).start();
        } else {
            final int port = Integer.parseInt(args[1]);
            long sleepms = 0L;
            try {
                sleepms = Long.parseLong(args[2]);
            } catch (Exception ignore) {
            }
            switch (component) {
            case "netty-server-singlethread":
                new NettyEchoServer(port, true).start();
                break;
            case "netty-server-multithread":
                new NettyEchoServer(port, false).start();
                break;
            case "netty-server-sleepy-singlethread":
                new NettyEchoServer(port, true, sleepms, false).start();
                break;
            case "netty-server-sleepy-singlethread-separated":
                new NettyEchoServer(port, true, sleepms, true).start();
                break;
            case "netty-server-sleepy-multithread":
                new NettyEchoServer(port, false, sleepms, false).start();
                break;
            case "netty-server-sleepy-multithread-separated":
                new NettyEchoServer(port, false, sleepms, true).start();
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

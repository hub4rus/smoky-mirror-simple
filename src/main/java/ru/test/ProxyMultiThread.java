package ru.test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;

public class ProxyMultiThread {

    public static final Logger log = Logger.getLogger(SecureProxyMultiThread.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length > 0  && args.length != 3)
                throw new IllegalArgumentException("insuficient arguments");
            // and the local port that we listen for connections on
            String host = (args.length > 0) ? args[0] : "localhost" /*"yandex.ru"*/;
            int remoteport = Integer.parseInt((args.length > 0) ? args[1] : "80");
            int localport = Integer.parseInt((args.length > 0) ? args[2] : "9999");
            boolean debug = Boolean.parseBoolean((args.length > 0) ? args[3] : "true");
            // Print a start-up message
            log.warning("Starting proxy for " + host + ":" + remoteport
                    + " on port " + localport);
            log.info("Current IP address : " + InetAddress.getLocalHost().getHostAddress());
            ServerSocket server = new ServerSocket(localport);
            while (true) {
                new ThreadProxy(server.accept(), host, remoteport, debug);
            }
        } catch (Exception e) {
            //System.err.println(e);
            log.throwing("ProxyMultiThread", "main", e);
            log.info("Usage: java ProxyMultiThread "
                    + "<host> <remoteport> <localport>");
        }
    }
}
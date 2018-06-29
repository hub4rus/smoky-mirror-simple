package ru.test;

import ru.test.server.MainServer;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {
    /*
        Example 1 (MultiSocket Proxy Server)

        Use Example 1 as HTTP Proxy for Another HTTP Proxy

        Example 2 (Only 1 Socket at the same time)

        Example 3 HTTP Proxy
    */

    public static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {
        //sh target/bin/smoky-mirror

//        System.getProperties().setProperty("javax.net.debug", "ssl");
        LogManager.getLogManager().readConfiguration(Main.class.getClassLoader().getResourceAsStream("logging.properties"));

        MainServer.main(args);

        //ProxyMultiThread.main(args);
        //SecureProxyMultiThread.main(args);
        log.warning("Ready !!!");
    }
}

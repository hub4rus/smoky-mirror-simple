package ru.test;


import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.logging.Logger;

public class SecureProxyMultiThread {

    public static final Logger log = Logger.getLogger(SecureProxyMultiThread.class.getName());

    private enum Protokol {HTTP, HTTPS}

    public static void main(String[] args) {
        try {
            // Check the number of arguments
            if (args.length > 0  && args.length != 3)
                throw new IllegalArgumentException("Wrong number of arguments.");
            // Get the command-line arguments: the host and port we are proxy for
            // and the local port that we listen for connections on
            Protokol protokol = (args.length > 0)
                    ? (Protokol.HTTPS.name().equalsIgnoreCase(args[0]) ? Protokol.HTTPS : Protokol.HTTP)
                    : Protokol.HTTP;
            String host = (args.length > 0) ? args[1] : "localhost";
            int remoteport = Integer.parseInt((args.length > 0) ? args[2] : "80");
            int localport = Integer.parseInt((args.length > 0) ? args[3] : "9999");
            boolean debug = Boolean.parseBoolean((args.length > 0) ? args[4] : "true");
            // Print a start-up message
            log.warning("Starting proxy for " + host + ":" + remoteport +
                    " on port " + localport + " with protokol " + protokol.name() +
                    (debug ? " - DEBUG " : ""));
            // And start running the server
            new SecureProxyMultiThread().start(protokol, host, remoteport, localport, debug);
        }
        catch (Exception e) {
            //System.err.println(e);
            log.throwing("SecureProxyMultiThread", "main", e);
            log.info("Usage: java SimpleProxyServer " +
                    "<host> <remoteport> <localport>");
        }
    }

    private void start(Protokol protokol, String host, int remoteport, int localport, boolean debug) throws IOException {
        ServerSocket server;
        switch (protokol) {
            case HTTP:
                server = new ServerSocket(localport);
                break;
            case HTTPS:
                SSLContext sslContext = createSSLContext();
                // Create server socket factory
                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
                // Create server socket
                /*SSLServerSocket*/ server = (SSLServerSocket) sslServerSocketFactory.createServerSocket(localport);
                break;
            default:
                throw new RuntimeException("Bad Transport");
        }
        while (true) {
            new ThreadProxy(server.accept(), host, remoteport, debug);
        }
    }

    private SSLContext createSSLContext(){
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");
            //keyStore.load(new FileInputStream("test.jks"),"passphrase".toCharArray());
            //keyStore.load(SimpleHttpsSecureServer.class.getResourceAsStream("sslKey\\" +  "testkey.jks"),"password".toCharArray());
            InputStream in = loadJKS();
            keyStore.load(in,"password".toCharArray());
            in.close();

            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            //keyManagerFactory.init(keyStore, "passphrase".toCharArray());
            keyManagerFactory.init(keyStore, "password".toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(km,  tm, null);

            return sslContext;
        } catch (Exception ex){
            //ex.printStackTrace();
            log.throwing("HTTPSServer","createSSLContext", ex);
        }

        return null;
    }

    public InputStream loadJKS() {
        String jksPath = "sslKey\\" +  "testkey.jks"; //from ide
        log.warning(String.format("testkey.jks init ... [%s]", jksPath) );
        ClassLoader classloader = this.getClass().getClassLoader();
        //ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream in = classloader.getResourceAsStream(jksPath);
        if (in == null) {
            jksPath = "/sslKey/" +"testkey.jks"; //from jar
            log.warning(String.format("testkey.jks init ... [%s]", jksPath) );
            in = this.getClass().getResourceAsStream(jksPath);
            if (in == null) throw new RuntimeException(String.format("jks - ResourceAsStream error ! [%s]", jksPath));
        }
        try {
            log.warning("testkey.jks - " + in.available() + " bytes loading");
        } catch (IOException e) {
            //e.printStackTrace();
            log.throwing("SecureKeystore", "loadJKS", e);
        }
        return in;
    }
}
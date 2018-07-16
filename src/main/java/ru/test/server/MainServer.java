package ru.test.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
String encoded = new sun.misc.BASE64Encoder().encodeBuffer((proxyUserName + ":" + proxyPassword).getBytes()).replace("\r\n", "");
httpURLConnection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);

HTTP response code 407. HTTP 407 means proxy authentication required

-----------

HTTP/1.0 407 Proxy Authentication Required

*/

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 * https://stackoverflow.com/questions/9357585/creating-a-java-proxy-server-that-accepts-https
 */
public class MainServer extends Thread {

    private static boolean DEBUG = false;

    public static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());
    private final static String BASE_ENCODE = "UTF-8"; //"ISO-8859-1";

    public static void main(String[] args) {
//        LOGGER.setLevel(Level.INFO);
        (new MainServer()).run();
    }

    public MainServer() {
        super("Server Thread");
    }

    @Override
    public void run() {
        String sPort = System.getenv("PORT");
        if (sPort == null || sPort.trim().isEmpty()) {
            sPort = "9999";
        }
        //sPort = "30134"; //80
        LOGGER.warning(String.format("Ready !!! on port = %s", sPort));
        try (ServerSocket serverSocket = new ServerSocket(Integer.valueOf(sPort)/*9999*/)) {
            Socket socket;
            try {
                while ((socket = serverSocket.accept()) != null) {
                    (new Handler(socket)).start();
                }
            } catch (IOException e) {
                //e.printStackTrace();  // TODO: implement catch
                //LOGGER.throwing(this.getName(), "run:accept Socked", e);
                LOGGER.log(Level.INFO, this.getName() + " -> run:accept Socked", e);
            }
        } catch (IOException e) {
            //e.printStackTrace();  // TODO: implement catch
            //LOGGER.throwing(this.getName(), "run", e);
            LOGGER.log(Level.INFO, this.getName() + " -> run", e);
            return;
        }
    }

    public static class Handler extends Thread {
        private static final int BUFFER_SIZE = 4096;
        //HTTPS = CONNECT www.google.com:443 HTTP/1.1
        public static final Pattern CONNECT_PATTERN_HTTPS = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        //HTTP = GET http://www.java-online.ru/ HTTP/1.1
        public static final Pattern CONNECT_PATTERN_HTTP = Pattern.compile("(HEAD|GET|POST|PUT) (.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        //public static final Pattern CONNECT_PATTERN_HTTP_HOST = Pattern.compile("HOST: (.+):(.+)", Pattern.CASE_INSENSITIVE);
        private final Socket clientSocket;
        private boolean previousWasR = false;

        public Handler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        private enum Protokol {HTTP, HTTPS}
        private enum HttpMethod {CONNECT, OTHER}

        private class Info {
            private final Protokol protokol;
            private final String host;
            private String port="80";
            private final String httpVersion;
            private final byte[] request;
            private final HttpMethod method;

            public Info(Protokol protokol, String host, String port, String httpVersion) {
                this.protokol = protokol;
                this.host = host;
                this.port = port;
                this.request = null;
                this.httpVersion = httpVersion;
                this.method = HttpMethod.CONNECT;
            }
            public Info(String method, Protokol protokol, String host, int port, String httpVersion, byte[] request) {
                //this.method = HttpMethod.GET.name().equalsIgnoreCase(method) ? HttpMethod.GET : HttpMethod.CONNECT;
                this.method = HttpMethod.OTHER;
                this.protokol = protokol;
                this.request = request;
                this.host = host;
                if (port > 0) {
                    this.port = String.valueOf(port);
                }
                this.httpVersion = httpVersion;
            }
        }

        @Override
        public void run() {
            try {
                WKey key = readLineKey(clientSocket);
                byte[] requestByte = key.data;
                String request = key.value;
                //String request = readLine(clientSocket);
                //System.out.println(request);
                LOGGER.info("request = " + request);
                Matcher matcher = CONNECT_PATTERN_HTTPS.matcher(request);
                boolean ok = matcher.matches();
                final Info info;
                if (ok) {
                    info = new Info(Protokol.HTTPS, matcher.group(1), matcher.group(2), matcher.group(3));
                    String header;
                    do {
                        header = readLine(clientSocket);
                        LOGGER.info("header = " + header);
                    } while (!"".equals(header));
                } else {
                    matcher = CONNECT_PATTERN_HTTP.matcher(request);
                    ok = matcher.matches();
                    if (ok) {
                        //Matcher matcherHost = CONNECT_PATTERN_HTTP_HOST.matcher(request);
                        //ok = ok & matcherHost.matches();
                        //info = new Info(Protokol.HTTP, matcherHost.group(1), matcherHost.group(2), matcher.group(3));
                        URL url = new URL(matcher.group(2));
                        //byte[] requestByte = (request.trim() + "\r\n").getBytes(BASE_ENCODE);
                        info = new Info(matcher.group(1), Protokol.valueOf(url.getProtocol().toUpperCase()),
                                            url.getHost(), url.getPort(), matcher.group(3), requestByte);
                    } else {
                        info = null;
                    }
                }
                if (ok & info != null) {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(), BASE_ENCODE);

                    final Socket forwardSocket;
                    try {
                        forwardSocket = new Socket(info.host, Integer.parseInt(info.port));
                        //System.out.println(forwardSocket);
                        LOGGER.info("forwardSocket:" + forwardSocket);
                    } catch (IOException | NumberFormatException e) {
                        ///
                        //    outputStreamWriter.write("HTTP/" + info.httpVersion + " 407 Proxy Auth Required");
                        //    outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        //    outputStreamWriter.write("\r\n");
                        //    outputStreamWriter.flush();
                        ///
                        //e.printStackTrace();  // TODO: implement catch
                        //LOGGER.throwing(this.getName(), "run: new Socked", e);
                        LOGGER.log(Level.INFO, this.getName() + " -> run: new Socked", e);

                        outputStreamWriter.write("HTTP/" + info.httpVersion + " 502 Bad Gateway\r\n");
                        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();

                        //ByteArrayOutputStream responseProxyHead = new ByteArrayOutputStream();
                        //responseProxyHead.write(("HTTP/" + info.httpVersion + " 502 Bad Gateway\r\n").getBytes(BASE_ENCODE));
                        //responseProxyHead.write("Proxy-agent: Simple/0.1\r\n".getBytes(BASE_ENCODE));
                        //responseProxyHead.write("\r\n".getBytes(BASE_ENCODE));
                        //forwardData(new ByteArrayInputStream(responseProxyHead.toByteArray()), clientSocket, false, DEBUG ? "request" : null);

                        return;
                    }
                    try {
                        if (info.method == HttpMethod.CONNECT) {
                            outputStreamWriter.write("HTTP/" + info.httpVersion + " 200 Connection established\r\n");
                            outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                            outputStreamWriter.write("\r\n");
                            outputStreamWriter.flush();

                            //ByteArrayOutputStream responseProxyHead = new ByteArrayOutputStream();
                            //responseProxyHead.write(("HTTP/" + info.httpVersion + " 200 Connection established\r\n").getBytes(BASE_ENCODE));
                            //responseProxyHead.write("Proxy-agent: Simple/0.1\r\n".getBytes(BASE_ENCODE));
                            //responseProxyHead.write("\r\n".getBytes(BASE_ENCODE));
                            //forwardData(new ByteArrayInputStream(responseProxyHead.toByteArray()), clientSocket, false, DEBUG ? "request" : null);
                        }

                        Thread remoteToClient = new Thread() {
                            @Override
                            public void run() {
                                forwardData(forwardSocket, clientSocket, HttpMethod.CONNECT /*info.method*/, DEBUG ? "response" : null);
                            }
                        };
                        remoteToClient.start();
                        try {
                            if (info.request != null) {
                                //OutputStream o = forwardSocket.getOutputStream();
                                //o.write(info.request);
                                //o.flush();
                                forwardData(new ByteArrayInputStream(info.request), forwardSocket, false, DEBUG ? "request" : null);

//                                OutputStreamWriter z = new OutputStreamWriter(forwardSocket.getOutputStream(), BASE_ENCODE);
//                                //z.write(request.trim() + "\r\n");
//                                //z.write("GET http://www.google.com/search?q=mkyong HTTP/1.1" + "\r\n");
//                                z.write("User-Agent: Mozilla/5.0" + "\r\n");
//                                z.write("Host: www.google.com" + "\r\n");
//                                z.write("Accept: text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2" + "\r\n");
//                                z.write("Proxy-Connection: keep-alive" + "\r\n");
//                                z.write("\r\n");
//                                //z.write("\r\n");
//                                z.flush();
//                                forwardSocket.shutdownOutput();

                                //forwardData(new ByteArrayInputStream(info.request), forwardSocket, false, true);
                                //forwardData(new ByteArrayInputStream((request.trim() + "\r\n").getBytes(BASE_ENCODE)), forwardSocket, false, true);
                                //forwardSocket.getOutputStream().write(info.request);
                                //forwardSocket.getOutputStream().write((request.trim() + "\r\n").getBytes(BASE_ENCODE));
                            }
                            if (previousWasR) {
                                int read = clientSocket.getInputStream().read();
                                if (read != -1) {
                                    if (read != '\n') {
                                        forwardSocket.getOutputStream().write(read);
                                    }
                                    forwardData(clientSocket, forwardSocket, info.method, DEBUG ? "request" : null);
                                } else {
                                    if (!forwardSocket.isOutputShutdown()) {
                                        forwardSocket.shutdownOutput();
                                    }
                                    if (!clientSocket.isInputShutdown()) {
                                        clientSocket.shutdownInput();
                                    }
                                }
                            } else {
                                forwardData(clientSocket, forwardSocket, info.method, DEBUG ? "request" : null);
                            }
                        } finally {
                            try {
                                remoteToClient.join();
                            } catch (InterruptedException e) {
                                //e.printStackTrace();  // TODO: implement catch
                                //LOGGER.throwing(this.getName(), "run: remoteToClient join", e);
                                LOGGER.log(Level.INFO, this.getName() + " -> run: remoteToClient join", e);
                            }
                        }
                    } finally {
                        forwardSocket.close();
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();  // TODO: implement catch
                //LOGGER.throwing(this.getName(), "run", e);
                LOGGER.log(Level.INFO, this.getName() + " -> run", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    //e.printStackTrace();  // TODO: implement catch
                    //LOGGER.throwing(this.getName(), "run: close Socked", e);
                    LOGGER.log(Level.INFO, this.getName() + " -> run: close Socked", e);
                }
            }
        }

//        private static void forwardData(Socket inputSocket, Socket outputSocket, boolean debug) {
//            try {
//                InputStream inputStream = inputSocket.getInputStream();
//                try {
//                    OutputStream outputStream = outputSocket.getOutputStream();
//                    try {
//                        byte[] buffer = new byte[4096];
//                        int read;
//                        do {
//                            read = inputStream.read(buffer);
//                            if (read > 0) {
//                                outputStream.write(buffer, 0, read);
//                                if (inputStream.available() < 1) {
//                                    outputStream.flush();
//                                }
//                                if (debug) {
//                                    byte[] destArr = new byte[read];
//                                    System.arraycopy (buffer, 0, destArr, 0, read);
//                                    System.out.println(" >> bytes_read = " + read + "; to_server--->" + new String(destArr, "UTF-8") + "<---");
//                                    //System.out.println(" >> bytes_read = " + read + "; to_server--->" + new String(destArr, "ISO-8859-1") + "<---");
//                                }
//                            }
//                        } while (read >= 0);
//                    } finally {
//                        if (!outputSocket.isOutputShutdown()) {
//                            outputSocket.shutdownOutput();
//                        }
//                    }
//                } finally {
//                    if (!inputSocket.isInputShutdown()) {
//                        inputSocket.shutdownInput();
//                    }
//                }
//            } catch (IOException e) {
////                e.printStackTrace();  // TODO: implement catch
//                //LOGGER.throwing(MainServer.Handler.class.getName(), "forwardData", e);
//                LOGGER.log(Level.INFO, MainServer.Handler.class.getName() + " -> forwardData", e);
//            }
//        }

        private static void forwardData(Socket inputSocket, Socket outputSocket) {
            forwardData(inputSocket, outputSocket, HttpMethod.CONNECT, null);
        }

        private static void forwardData(Socket inputSocket, Socket outputSocket, HttpMethod method, String debugKey) {
            try {
                InputStream inputStream = inputSocket.getInputStream();
                try {
                    forwardData(inputStream, outputSocket, method, true, debugKey);
                } finally {
                    if (!inputSocket.isInputShutdown()) {
                        inputSocket.shutdownInput();
                    }
                }
            } catch (IOException e) {
//                e.printStackTrace();  // TODO: implement catch
                //LOGGER.throwing(MainServer.Handler.class.getName(), "forwardData", e);
                LOGGER.log(Level.INFO, MainServer.Handler.class.getName() + " -> forwardData", e);
            }
        }

        private static void forwardData(InputStream inputStream, Socket outputSocket,
                                        boolean closeSocketAfter, String debugKey) throws IOException {
            forwardData(inputStream, outputSocket, HttpMethod.CONNECT, closeSocketAfter, debugKey);
        }

        private static void forwardData(InputStream inputStream, Socket outputSocket, HttpMethod method,
                                        boolean closeSocketAfter, String debugKey) throws IOException {
            OutputStream outputStream = outputSocket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                do {
                    read = bis.read(buffer);
                    if (read > 0) {
// fix bugs HttpUrlConnection - test client
//                        if (read < BUFFER_SIZE
//                                && buffer[read-7] == '\r' && buffer[read-6] == '\n'
//                                && buffer[read-5] == 48
//                                && buffer[read-4] == '\r' && buffer[read-3] == '\n'
//                                && buffer[read-2] == '\r' && buffer[read-1] == '\n') {
//                            read = read - 5;
//                        }
                        bos.write(buffer, 0, read);
                        if (bis.available() < 1) {
                            bos.flush();
                        }
                        if (debugKey != null) {
                            System.out.println(String.format(debugKey + " >> bytes_read = %04d; to_server--->%s<---",
                                    read, new String(buffer, 0 , read, "UTF-8")));
                            //byte[] destArr = new byte[read];
                            //System.arraycopy(buffer, 0, destArr, 0, read);
                            //System.out.println(String.format(" >> bytes_read = %04d; to_server--->%s<---",
                            //        read, new String(destArr, "UTF-8")));
                            //System.out.println(" >> bytes_read = " + read + "; to_server--->" + new String(destArr, "ISO-8859-1") + "<---");
                        }
                    }
                } while (read >= 0);
                //} while ((method != HttpMethod.CONNECT) ? read >= BUFFER_SIZE : read >= 0);
            } finally {
                if (closeSocketAfter && !outputSocket.isOutputShutdown()) {
                    outputSocket.shutdownOutput();
                }
            }
        }

        private String readLine(Socket socket) throws IOException {
            return readLineStream(socket).toString(BASE_ENCODE);
        }

        private class WKey {
            private final String value;
            private final byte[] data;
            public WKey(String value, byte[] data) {
                this.value = value;
                this.data = data;
            }
        }

        private WKey readLineKey(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = readLineStream(socket);
            String value = byteArrayOutputStream.toString(BASE_ENCODE);
            byteArrayOutputStream.write('\r');
            byteArrayOutputStream.write('\n');
            return new WKey(value, byteArrayOutputStream.toByteArray());
        }

        private ByteArrayOutputStream readLineStream(Socket socket) throws IOException {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int next;
            readerLoop:
            while ((next = socket.getInputStream().read()) != -1) {
                if (previousWasR && next == '\n') {
                    previousWasR = false;
                    continue;
                }
                previousWasR = false;
                switch (next) {
                    case '\r':
                        previousWasR = true;
                        break readerLoop;
                    case '\n':
                        break readerLoop;
                    default:
                        byteArrayOutputStream.write(next);
                        break;
                }
            }
            return byteArrayOutputStream;
        }
    }
}
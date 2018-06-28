package ru.test.server.bak;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
String encoded = new sun.misc.BASE64Encoder().encodeBuffer((proxyUserName + ":" + proxyPassword).getBytes()).replace("\r\n", "");
httpURLConnection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);

HTTP response code 407. HTTP 407 means proxy authentication required
*/

/**
 * Created for http://stackoverflow.com/q/16351413/1266906.
 * https://stackoverflow.com/questions/9357585/creating-a-java-proxy-server-that-accepts-https
 */
public class MainServerBak extends Thread {

    public static final Logger LOGGER = Logger.getLogger(MainServerBak.class.getName());

    public static void main(String[] args) {
//        LOGGER.setLevel(Level.INFO);
        (new MainServerBak()).run();
    }

    public MainServerBak() {
        super("Server Thread");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
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
        public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        //public static final Pattern CONNECT_PATTERN = Pattern.compile("(HEAD|GET|POST|PUT) (.+):(.+) HTTP/(1\\.[01])", Pattern.CASE_INSENSITIVE);
        private final Socket clientSocket;
        private boolean previousWasR = false;

        public Handler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                String request = readLine(clientSocket);
                //System.out.println(request);
                LOGGER.info("request = " + request);
                Matcher matcher = CONNECT_PATTERN.matcher(request);
                if (matcher.matches()) {
                    String header;
                    do {
                        header = readLine(clientSocket);
                        LOGGER.info("header = " + header);
                    } while (!"".equals(header));
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                            "ISO-8859-1");

                    final Socket forwardSocket;
                    try {
                        forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));
                        //System.out.println(forwardSocket);
                        LOGGER.info("forwardSocket:" + forwardSocket);
                    } catch (IOException | NumberFormatException e) {
                        ///
                        //    outputStreamWriter.write("HTTP/" + matcher.group(3) + " 407 Proxy Auth Required");
                        //    outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        //    outputStreamWriter.write("\r\n");
                        //    outputStreamWriter.flush();
                        ///
                        //e.printStackTrace();  // TODO: implement catch
                        //LOGGER.throwing(this.getName(), "run: new Socked", e);
                        LOGGER.log(Level.INFO, this.getName() + " -> run: new Socked", e);
                        outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();
                        return;
                    }
                    try {
                        outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                        outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                        outputStreamWriter.write("\r\n");
                        outputStreamWriter.flush();

                        Thread remoteToClient = new Thread() {
                            @Override
                            public void run() {
                                forwardData(forwardSocket, clientSocket);
                            }
                        };
                        remoteToClient.start();
                        try {
                            if (previousWasR) {
                                int read = clientSocket.getInputStream().read();
                                if (read != -1) {
                                    if (read != '\n') {
                                        forwardSocket.getOutputStream().write(read);
                                    }
                                    forwardData(clientSocket, forwardSocket);
                                } else {
                                    if (!forwardSocket.isOutputShutdown()) {
                                        forwardSocket.shutdownOutput();
                                    }
                                    if (!clientSocket.isInputShutdown()) {
                                        clientSocket.shutdownInput();
                                    }
                                }
                            } else {
                                forwardData(clientSocket, forwardSocket);
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

        private static void forwardData(Socket inputSocket, Socket outputSocket) {
            try {
                InputStream inputStream = inputSocket.getInputStream();
                try {
                    OutputStream outputStream = outputSocket.getOutputStream();
                    try {
                        byte[] buffer = new byte[4096];
                        int read;
                        do {
                            read = inputStream.read(buffer);
                            if (read > 0) {
                                outputStream.write(buffer, 0, read);
                                if (inputStream.available() < 1) {
                                    outputStream.flush();
                                }
                                {
                                    byte[] destArr = new byte[read];
                                    System.arraycopy (buffer, 0, destArr, 0, read);
                                    System.out.println(" >> bytes_read = " + read + "; to_server--->" + new String(destArr, "UTF-8") + "<---");
                                }
                            }
                        } while (read >= 0);
                    } finally {
                        if (!outputSocket.isOutputShutdown()) {
                            outputSocket.shutdownOutput();
                        }
                    }
                } finally {
                    if (!inputSocket.isInputShutdown()) {
                        inputSocket.shutdownInput();
                    }
                }
            } catch (IOException e) {
//                e.printStackTrace();  // TODO: implement catch
                //LOGGER.throwing(MainServer.Handler.class.getName(), "forwardData", e);
                LOGGER.log(Level.INFO, MainServerBak.Handler.class.getName() + " -> forwardData", e);
            }
        }

        private String readLine(Socket socket) throws IOException {
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
            return byteArrayOutputStream.toString("ISO-8859-1");
        }
    }
}
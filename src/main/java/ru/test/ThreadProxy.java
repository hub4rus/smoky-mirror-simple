package ru.test;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ThreadProxy extends Thread {

    public static final Logger log = Logger.getLogger(ThreadProxy.class.getName());

    private Socket sClient;
    private /*final*/ String SERVER_URL;
    private /*final*/ int SERVER_PORT;
    private final boolean DEBUG;
    ThreadProxy(Socket sClient, String ServerUrl, int ServerPort, boolean debug) {
        this.SERVER_URL = ServerUrl;
        this.SERVER_PORT = ServerPort;
        this.sClient = sClient;
        this.DEBUG = debug;
        this.start();
    }

    private boolean parseHeadGetHost(final InputStream inFromClient, List<String> listHead) throws IOException {
        //    GET http://ya.ru/ HTTP/1.1
        //    Host: ya.ru
        //    User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:56.0) Gecko/20100101 Firefox/56.0
        //    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
        //    Accept-Language: ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3
        //    Accept-Encoding: gzip, deflate
        //    Cookie: yandexuid=8573785771480513315; yp=1521018639.ygu.1; _ym_uid=1480513316770053055; mda=0; yandex_gid=213; zm=m-white_yaru.css%3Awww_YVUo98aQOPCqCoiictc7uBqL0YI%3Al; _ym_isad=2
        //    Connection: keep-alive
        //    Upgrade-Insecure-Requests: 1
        StringBuilder builder = new StringBuilder();
        int idx;
        String line = null;
        int readSize=0;
        byte[] headRead = new byte[4];
        while(true) {
            while ((readSize = inFromClient.read(headRead))>0) {
                String sm = new String(headRead, 0 , readSize, "UTF-8");
                builder.append(sm);
                idx = builder.toString().indexOf("\r\n");
                if (idx >= 0) {
                    idx = idx + "\r\n".length();
                    line = builder.toString().substring(0, idx);
                    builder = new StringBuilder().append(builder.substring(idx));
                    break;
                }
            }

            if (line != null) {
                listHead.add(line);
                //System.out.println(listHead.get(listHead.size()-1));
            }
            if (line == null || line.toUpperCase().startsWith("HOST:")) {
                if (line != null) {
                    //InetAddress[] addresses = InetAddress.getAllByName(line.substring(5).trim());
                    //SERVER_URL = addresses[0].getHostAddress();
                    if (line.substring(5).trim().contains(":")) {
                        String sx[] = line.substring(5).trim().split(":");
                        SERVER_URL = sx[0];
                        SERVER_PORT = Integer.parseInt(sx[1]);
                    } else {
                        SERVER_URL = line.substring(5).trim();
                    }
                    listHead.add(builder.toString());
                    //System.out.println(listHead.get(listHead.size()-1));
                }
                break;
            } else {
                if (readSize < headRead.length) break;
            }
        };
        return (readSize < headRead.length);
    }

    private void CopyParsedHead(final OutputStream outToServer, List<String> listHead, boolean fullRead) throws IOException {
        for(String x : listHead) {
            if (x != null & !x.isEmpty()) {
                outToServer.write(x.getBytes("UTF-8"));
                if (DEBUG) {
                    log.info(x.length() + " : to_server--->" + x + "<---");
                }
            }
        }
        if (fullRead) {
            outToServer.write("\r\n".getBytes("UTF-8"));
        }
        outToServer.flush();
    }

    @Override
    public void run() {
        try {
            final byte[] request = new byte[1024];
            byte[] reply = new byte[4096];

            //Socket client = null;
            final InputStream inFromClient = sClient.getInputStream();
            final OutputStream outToClient = sClient.getOutputStream();
            Socket server;
            final InputStream inFromServer;
            final OutputStream outToServer;
            final boolean fullRead;
            // connects a socket to the server
            try {
                List<String> listHead = new ArrayList<>();
                fullRead = parseHeadGetHost(inFromClient, listHead);
                //
                if (listHead.size() == 0) return;
                server = new Socket(SERVER_URL, SERVER_PORT);
                inFromServer = server.getInputStream();
                outToServer = server.getOutputStream();
                //
                CopyParsedHead(outToServer, listHead, fullRead);
//                int zzz = inFromServer.read(reply);
//                server = new Socket("ya.ru", 443);
            } catch (IOException e) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(outToClient));
                out.flush();
                log.warning(">>" + SERVER_URL + ":" + SERVER_PORT);
                throw new RuntimeException(e);
            }
            // a new thread to manage streams from server to client (DOWNLOAD)
            if (!DEBUG) {
                log.warning(">>" + SERVER_URL + ":" + SERVER_PORT);
            }
            // a new thread for uploading to the server
            if (!fullRead) {
                new Thread() {
                    public void run() {
                        int bytes_read;
                        try {
                            while (!fullRead && (bytes_read = inFromClient.read(request)) != -1) {
                                outToServer.write(request, 0, bytes_read);
                                if (DEBUG) {
                                    //byte[] destArr = new byte[bytes_read];
                                    //System.arraycopy (request, 0, destArr, 0, bytes_read);
                                    log.info(bytes_read + " _ to_server--->" + new String(request, 0, bytes_read, "UTF-8") + "<---");
                                }
                                outToServer.flush();
                                //TODO CREATE YOUR LOGIC HERE
                            }
                        } catch (IOException e) {
                            log.throwing("ThreadProxy", "run", e);
                        }
                        try {
                            outToServer.close();
                        } catch (IOException e) {
                            //e.printStackTrace();
                            log.throwing("ThreadProxy", "run", e);
                        }
                    }
                }.start();
            }
            // current thread manages streams from server to client (DOWNLOAD)
            int bytes_read;
            try {
                while ((bytes_read = inFromServer.read(reply)) != -1) {
                    outToClient.write(reply, 0, bytes_read);
                    outToClient.flush();
                    //TODO CREATE YOUR LOGIC HERE
                    if (DEBUG) {
                        log.info(bytes_read + " ERRR --->" + new String(reply, 0, bytes_read, "UTF-8") + "<---");
                    }
                }
            } catch (IOException e) {
                //e.printStackTrace();
                log.throwing("ThreadProxy", "run", e);
            } finally {
                try {
                    if (server != null)
                        server.close();
//                    if (client != null)
//                        client.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                    log.throwing("ThreadProxy", "run", e);
                }
            }
            outToClient.close();
            sClient.close();
        } catch (IOException e) {
            //e.printStackTrace();
            log.throwing("ThreadProxy", "run", e);
        }
    }
}
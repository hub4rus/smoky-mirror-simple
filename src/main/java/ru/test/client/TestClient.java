package ru.test.client;

import java.io.*;
import java.net.*;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class TestClient {

    private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {

        //        print_Proxy(); System.exit(0);
        //        testConnectBySocket(); System.exit(0);

        //    System.getProperties().put("proxySet", "true");
        //    System.getProperties().put("https.proxyHost", "localhost");
        //    System.getProperties().put("https.proxyPort", "9999");
        //    System.getProperties().put("http.proxyHost", "localhost");
        //    System.getProperties().put("http.proxyPort", "9999");

        /*
        /usr/java/jdk1.7.0_04/bin/java -Dhttp.proxyHost=10.128.128.13
            -Dhttp.proxyPassword -Dhttp.proxyPort=80 -Dhttp.proxyUserName
            -Dhttps.proxyHost=10.128.128.13 -Dhttps.proxyPassword -Dhttps.proxyPort=80
            -Dhttps.proxyUserName com.stackoverflow.Runner
        */

        TestClient http = new TestClient();

        //not work !!!
        //Properties systemProperties = System.getProperties();
        //systemProperties.setProperty("http.proxyHost","localhost");
        //systemProperties.setProperty("http.proxyPort","9999");

        /* //or   = https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
            System.setProperty("http.proxyHost", "localhost");
            System.setProperty("http.proxyPort", "9999");
            System.setProperty("http.nonProxyHosts", "");
            System.setProperty("https.proxyHost", "localhost");
            System.setProperty("https.proxyPort", "9999");
        */

        //work !!!
        //Proxy proxy = Proxy.NO_PROXY;
        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.0.1", 8080));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 9999));
        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("smoky-mirror.herokuapp.com", 56978));
        /*
            if your proxy requires authentication it will give you response 407.
            In this case you'll need the following code:

                Authenticator authenticator = new Authenticator() {

                    public PasswordAuthentication getPasswordAuthentication() {
                        return (new PasswordAuthentication("user",
                                "password".toCharArray()));
                    }
                };
                Authenticator.setDefault(authenticator);
        */

        //http
//        String urlGet = "http://java-online.ru/blog-tokenizer.xhtml";
        //String urlGet = "http://java-online.ru/";
        //String urlGet = "http://www.rgagnon.com/javadetails/java-0085.html";
//        String urlGet = "http://www.google.com/search?q=mkyong";


        //https
        //String urlGet = "https://www.linuxmint.com/start/rosa/";
        String urlGet = "https://www.nix.ru/";
        //String urlGet = "https://ya.ru/";


        System.out.println("Testing 1 - Send Http GET request");
        http.sendGet(urlGet, proxy);

        //https
        String urlPost = "https://selfsolve.apple.com/wcResults.do";
        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

//        System.out.println("\nTesting 2 - Send Http POST request");
//        http.sendPost(urlPost, proxy);


        //System.clearProperty("http.proxyHost");

    }

    // HTTP GET request
    private void sendGet(String url, Proxy proxy) throws Exception {

        URL obj = new URL(url);

        //HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        HttpURLConnection con = getConnection(obj, proxy);

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.connect();
        if (con instanceof HttpsURLConnection) {
            print_https_cert((HttpsURLConnection) con);
        }

        int responseCode = con.getResponseCode();

        System.out.println("Proxy? " + con.usingProxy());

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());

    }

    // HTTP POST request
    private void sendPost(String url, Proxy proxy, String urlParameters) throws Exception {

        URL obj = new URL(url);
        //HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
        HttpURLConnection con = getConnection(obj, proxy);

        System.out.println("Proxy? " + con.usingProxy());

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        con.connect();
        if (con instanceof HttpsURLConnection) {
            print_https_cert((HttpsURLConnection) con);
        }

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + urlParameters);
        System.out.println("Response Code : " + responseCode);

        readResponse(con.getInputStream());
//        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
//        String inputLine;
//        StringBuffer response = new StringBuffer();
//
//        while ((inputLine = in.readLine()) != null) {
//            response.append(inputLine);
//        }
//        in.close();
//
//        //print result
//        System.out.println(response.toString());

    }

    private HttpURLConnection getConnection(URL url, Proxy proxy) throws IOException {
        HttpURLConnection con;
        switch (url.getProtocol()) {
            case "http":
                con = (HttpURLConnection) url.openConnection(proxy);
                break;
            case "https":
                con = (HttpsURLConnection) url.openConnection(proxy);
                break;
            default:
                throw new RuntimeException("wrong protocol");
        }
        return con;
    }

    ////////////////////////////////////////////

    private void print_https_cert(HttpsURLConnection con){

        if(con!=null){

            try {

                System.out.println("Response Code : " + con.getResponseCode());
                System.out.println("Cipher Suite : " + con.getCipherSuite());
                System.out.println("\n");

                Certificate[] certs = con.getServerCertificates();
                for(Certificate cert : certs){
                    System.out.println("Cert Type : " + cert.getType());
                    System.out.println("Cert Hash Code : " + cert.hashCode());
                    System.out.println("Cert Public Key Algorithm : "
                            + cert.getPublicKey().getAlgorithm());
                    System.out.println("Cert Public Key Format : "
                            + cert.getPublicKey().getFormat());
                    System.out.println("\n");
                }

            } catch (SSLPeerUnverifiedException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }

        }

    }


    private void print_Proxy() {
        System.setProperty("java.net.useSystemProxies", "true");
        List l = null;
        try {
            l = ProxySelector.getDefault().select(new URI("http://www.yahoo.com"));
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (l != null) {
            for (Iterator iter = l.iterator(); iter.hasNext(); ) {
                java.net.Proxy proxy = (java.net.Proxy) iter.next();
                System.out.println("proxy hostname : " + proxy.type());
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                if (addr == null) {
                    System.out.println("No Proxy");
                }
                else {
                    System.out.println("proxy hostname : " + addr.getHostName());
                    System.out.println("proxy port : " + addr.getPort());
                }
            }
        }
    }

    private void testConnectBySocket() throws IOException {
        String url = /*"http://www.marchal.com/"*/ "http://java-online.ru/blog-tokenizer.xhtml" ,
                proxy = /*"proxy.mydomain.com"*/ "localhost",
                port = /*"8080"*/ "9999",
                authentication = "usr:pwd";
        URL server = new URL(url);
        Socket socket = new Socket(proxy, Integer.parseInt(port));

//        SocketAddress proxyAddr = new InetSocketAddress(proxy, Integer.parseInt(port));
//        Socket socket = new Socket(new Proxy(Proxy.Type.HTTP, proxyAddr));
//        socket.connect(new InetSocketAddress(address, port));


        //HTTP/1.0 407 Proxy Authentication Required
        Writer writer = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
        //writer.write("CONNECT " + server.toExternalForm() + " HTTP/1.0\r\n");
        writer.write("GET " + server.toExternalForm() + " HTTP/1.0\r\n");
        writer.write("Host: " + server.getHost() + "\r\n");
        writer.write("Proxy-Authorization: Basic "
                + //new sun.misc.BASE64Encoder().encode(
                Base64.getEncoder().encode(
                authentication.getBytes())
                + "\r\n\r\n");

        writer.flush();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(),"US-ASCII"));
        String line = reader.readLine();
        if(line != null && line.startsWith("HTTP/"))
        {
            int sp = line.indexOf(' ');
            String status = line.substring(sp + 1,sp + 4);
            if(status.equals("200"))
            {
                while(line.length() != 0)
                    line = reader.readLine();
                readResponse(reader);
            }
            else
                throw new FileNotFoundException("Host reports error " +
                        status);
        }
        else {
            throw new IOException("Bad protocol");
        }
        reader.close();
        writer.close();
        socket.close();
    }

    private static void readResponse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
    }

    private static void readResponse(BufferedReader reader ) throws IOException {
        //
        String f;
        while ((f = reader.readLine()) != null) {
            System.out.println(f);
        }
        reader.close();
    }

}
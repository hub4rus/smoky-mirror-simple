package ru.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

public class TestClient {

    private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {

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

        //not work !!!
        //System.setProperty("https.proxyHost", "smoky-mirror.herokuapp.com");
        //System.setProperty("https.proxyHost", "localhost");
        //System.setProperty("https.proxyPort", "9999");
        //System.setProperty("http.proxyHost", "webcache.mydomain.com");
        //System.setProperty("http.proxyPort", "8080");

        System.out.println("Testing 1 - Send Http GET request");
        http.sendGet(true);

//        System.out.println("\nTesting 2 - Send Http POST request");
//        http.sendPost();


        //System.clearProperty("http.proxyHost");

    }

    // HTTP GET request
    private void sendGet(boolean useProxy) throws Exception {

        String url = "http://www.google.com/search?q=mkyong";

        URL obj = new URL(url);

        //work !!!
        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.0.1", 8080));
        //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 9999));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("smoky-mirror.herokuapp.com", 9999));
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


        //HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        HttpURLConnection con = (HttpURLConnection) (useProxy ? obj.openConnection(proxy) : obj.openConnection());

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();

        System.out.println("Proxy? " + con.usingProxy());

        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
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
    private void sendPost() throws Exception {

        String url = "https://selfsolve.apple.com/wcResults.do";
        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        System.out.println("Proxy? " + con.usingProxy());

        //add reuqest header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

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

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());

    }

}
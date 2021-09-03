package com.sap.cap.productsservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.security.BasicCredentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@WebServlet("/ignoressl/*")
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class BTPAzureProxyServletIgnoreSSL extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServletIgnoreSSL.class);
    private static final String DESTINATION_NAME = "s4test";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {   
        HttpDestination httpDestination = DestinationAccessor.getDestination(DESTINATION_NAME).asHttp();
        KeyStore keyStore = httpDestination.getTrustStore().getOrNull();
        SSLContext sslContext = null;
        TrustManagerFactory trustManagerFactory;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            //Certificate certificate = keyStore.getCertificate("fake.cer");
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
        // Create trusting host name verifier for private link IP
        String privateIP = httpDestination.getUri().getHost();
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                boolean result = false;
                if( hostname.equals(privateIP)){
                    result = true;
                }
            return result;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        String uriBase = httpDestination.getUri().toString();
        String uriReplaced = request.getRequestURI().trim().split("/ignoressl/")[1];
        String queryString = request.getQueryString();

        logger.info("Destination target: " + uriBase + uriReplaced + "?" + queryString);
        URL url = null;        
        if(queryString != null){
            url = new URL(uriBase + uriReplaced + "?" + queryString);
        }else{
            url = new URL(uriBase + uriReplaced);
        }

        BasicCredentials props = httpDestination.getBasicCredentials().get();
        String basic = "Basic " + Base64.getEncoder().encodeToString((props.getUsername() + ":" + props.getPassword()).getBytes());
        //measure execution time
        Instant start = Instant.now();
        
        HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
        con.setSSLSocketFactory(sslContext.getSocketFactory());
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", basic);
        
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        logger.info("Message round trip time: " + timeElapsed + "ms");

        int status = con.getResponseCode();

        BufferedReader in = null;

        if (status > 299) {
            in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        } else {
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        }

        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
    
        //post back response
        String result = content.toString();
        response.setCharacterEncoding("UTF-8");
        if (result != "") {

            response.setContentType("application/xml");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(result);
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/plain");
            response.getWriter().write("No data available.");
        }
    }
}
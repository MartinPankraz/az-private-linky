package com.sap.cap.productsservice;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpClientFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

@WebServlet("/azure/*")
public class BTPAzureProxyServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServlet.class);
    private static final String DESTINATION_NAME = "s4testshort";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {
        try {//Bypass SSL handshake boundary. Fix with proper certificate store for productive usage
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch(KeyManagementException e) {
            e.printStackTrace();
        }
        
        logger.info("I am running! " + request.getRequestURI().trim() + " "+ request.getQueryString());
        String uriReplaced = request.getRequestURI().trim().split("/azure")[1];
        String queryString = request.getQueryString();

        String url = "";        
        if(queryString != null){
            url = uriReplaced + "?" + queryString;
        }else{
            url = uriReplaced;
        }
        logger.info("Destination target: " + url);
        DefaultHttpClientFactory customFactory = new DefaultHttpClientFactory();
        final HttpDestination destination = DestinationAccessor.getDestination(DESTINATION_NAME).asHttp();
        final HttpClient httpClient = customFactory.createHttpClient(destination);
        
        final HttpResponse exchangeRateResponse = httpClient.execute(new HttpGet(url));
        final HttpEntity entity = exchangeRateResponse.getEntity();
    
        //post back response
        response.setCharacterEncoding("UTF-8");
        if (entity != null) {
            final String content = EntityUtils.toString(entity);

            response.setContentType("application/atom+xml");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(content.toString());
        } else {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/plain");
            response.getWriter().write("No data available.");
        }
    }
}
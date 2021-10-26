package com.sap.cap.productsservice;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpClientFactory;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@WebServlet("/pingtest/*")
/* uncomment once you start using the approuter */
//@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class BTPAzureProxyServletPing extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServletPing.class);
    private static final String DESTINATION_NAME = "s4BasicAuth";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {      
        String uriReplaced = request.getRequestURI().trim().split("/pingtest")[1];
        String queryString = request.getQueryString();
        String httpMethod = request.getMethod();

        String url = "";        
        if(queryString != null){
            url = uriReplaced + "?" + queryString;
        }else{
            url = uriReplaced;
        }
        logger.info("Destination target (" + httpMethod + "): " + url);
        DefaultHttpClientFactory customFactory = new DefaultHttpClientFactory();
        final HttpDestination destination = DestinationAccessor.getDestination(DESTINATION_NAME).asHttp();
        HttpClient httpClient = customFactory.createHttpClient(destination);
        
        HttpUriRequest myRequest = new HttpGet(url);
        myRequest.setHeader("Content-Type", request.getContentType());
        //measure execution time
        Instant start = Instant.now();

        final HttpResponse productResponse = httpClient.execute(myRequest);
        
        Instant end = Instant.now();
        long timeElapsed = Duration.between(start, end).toMillis();
        logger.info("Message round trip time: " + timeElapsed + "ms");

        final HttpEntity entity = productResponse.getEntity();
    
        //post back response
        response.setCharacterEncoding("UTF-8");
        response.setContentType(productResponse.getFirstHeader("Content-Type").getValue());
        response.setStatus(productResponse.getStatusLine().getStatusCode());     
        if (entity != null) {
            final String content = EntityUtils.toString(entity);
            response.getWriter().write(content.toString());
        } else {
            response.getWriter().write("No data available.");
        }
    }
}
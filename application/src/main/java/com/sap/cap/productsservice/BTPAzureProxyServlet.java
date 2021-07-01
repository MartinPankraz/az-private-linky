package com.sap.cap.productsservice;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
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

@WebServlet("/azure/*")
public class BTPAzureProxyServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServlet.class);
    private static final String DESTINATION_NAME = "s4test";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {      
        String uriReplaced = request.getRequestURI().trim().split("/azure")[1];
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
        String contentType = productResponse.getFirstHeader("Content-Type").getValue();
        response.setContentType(contentType);
        response.setStatus(productResponse.getStatusLine().getStatusCode());
        if (entity != null) {
            final String content = EntityUtils.toString(entity);
            response.getWriter().write(content.toString());
        } else {
            response.getWriter().write("No data available.");
        }
    }

    @Override
    protected void doPost( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {      
        String uriReplaced = request.getRequestURI().trim().split("/azure")[1];
        String queryString = request.getQueryString();
        //String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String body = IOUtils.toString(request.getReader());
        String url = "";        
        if(queryString != null){
            url = uriReplaced + "?" + queryString;
        }else{
            url = uriReplaced;
        }
        //logger.info("Destination target (POST): " + url.toString());
        DefaultHttpClientFactory customFactory = new DefaultHttpClientFactory();
        final HttpDestination destination = DestinationAccessor.getDestination(DESTINATION_NAME).asHttp();
        HttpClient httpClient = customFactory.createHttpClient(destination);
        //measure execution time
        //Instant start = Instant.now();
        //make sure batch requests are passed on
        HttpPost myPost = new HttpPost(url);
        StringEntity requestEntity = new StringEntity(body);
        myPost.setEntity(requestEntity);
        myPost.setHeader("Content-Type", request.getContentType());
        
        final HttpResponse productResponse = httpClient.execute(myPost);
        
        //Instant end = Instant.now();
        //long timeElapsed = Duration.between(start, end).toMillis();
        //logger.info("Message round trip time: " + timeElapsed + "ms");

        final HttpEntity entity = productResponse.getEntity();
        String contentType = productResponse.getFirstHeader("Content-Type").getValue();
        response.setContentType(contentType);
        //post back response
        response.setCharacterEncoding("UTF-8");
        response.setStatus(productResponse.getStatusLine().getStatusCode());
        if (entity != null) {
            final String content = EntityUtils.toString(entity);
            response.getWriter().write(content.toString());
        } else {
            response.getWriter().write("No data available.");
        }
    }

    @Override
    protected void doHead( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {      
        String uriReplaced = request.getRequestURI().trim().split("/azure")[1];
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
        
        HttpUriRequest myRequest = new HttpHead(url);
        myRequest.setHeader("Content-Type", request.getContentType());

        final HttpResponse productResponse = httpClient.execute(myRequest);

        final HttpEntity entity = productResponse.getEntity();
    
        //post back response
        response.setCharacterEncoding("UTF-8");
        String contentType = productResponse.getFirstHeader("Content-Type").getValue();
        response.setContentType(contentType);
        response.setStatus(productResponse.getStatusLine().getStatusCode());
        if (entity != null) {
            final String content = EntityUtils.toString(entity);
            response.getWriter().write(content.toString());
        } else {
            response.getWriter().write("No data available.");
        }
    }
}
package com.sap.cap.productsservice;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/debug")
public class JWTDebugProxyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServlet.class);
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response )
            throws ServletException, IOException
    {
        response.setContentType("text/plain");
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);

            response.getOutputStream().println(key+" : "+value);
        }
    }
}
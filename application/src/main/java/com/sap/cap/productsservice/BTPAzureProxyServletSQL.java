package com.sap.cap.productsservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.cloudplatform.security.BasicCredentials;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@WebServlet("/sql/*")
/* uncomment once you start using the approuter */
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class BTPAzureProxyServletSQL extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServletSQL.class);
    private static final String DESTINATION_NAME = "AzureMySQLBasic";
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";//"org.mariadb.jdbc.Driver"
    private static final String SQLDB = "sap_cap_example";
    private static final String SQLTABLE = "my_bookshop_books";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws IOException
    {
        logger.info("I am running!");
        String sqlSelectAllBooks = "SELECT * FROM " + SQLTABLE;
        final HttpDestination destination = DestinationAccessor.getDestination(DESTINATION_NAME).asHttp();
        String SQLHostname = destination.getUri().getHost();
        int SQLPort = destination.getUri().getPort();
        String connectionUrl = "jdbc:mysql://" + SQLHostname +":" + SQLPort + "/" + SQLDB + "?serverTimezone=UTC";//"jdbc:mariadb://" + SQLHostname +":" + SQLPort + "/" + SQLDB";

        ArrayList<String> list = new ArrayList<String>();
        
        try {
            Class.forName(DRIVER_CLASS);
            BasicCredentials props = destination.getBasicCredentials().get();
            String SQLUser = props.getUsername();
            String SQLPwd = props.getPassword();
            Connection conn = DriverManager.getConnection(connectionUrl, SQLUser, SQLPwd); 
            PreparedStatement ps = conn.prepareStatement(sqlSelectAllBooks); 
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int id = rs.getInt("ID");
                String title = rs.getString("title");
                String stock = rs.getString("stock");

                // do something with the extracted data...
                list.add(id + " "+ title +"("+stock+")");
            }
        } catch (SQLException | ClassNotFoundException e) {
            response.getWriter().write(e.getMessage());
        }
        response.getWriter().write(list.toString());
    }
}
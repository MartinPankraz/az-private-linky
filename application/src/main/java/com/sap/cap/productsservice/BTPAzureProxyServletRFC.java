package com.sap.cap.productsservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.s4hana.connectivity.exception.RequestExecutionException;
import com.sap.cloud.sdk.s4hana.connectivity.rfc.RfmRequest;
import com.sap.cloud.sdk.s4hana.connectivity.rfc.RfmRequestResult;

import java.io.IOException;

@WebServlet("/myRFC/*")
/* uncomment once you start using the approuter */
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class BTPAzureProxyServletRFC extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServletRFC.class);
    private static final String DESTINATION_NAME = "s4RFC";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {      
        String fmNameFromPath = request.getRequestURI().trim().split("/myRFC/")[1];
        //logger.info("***fm name: " + fmNameFromPath);
        final Destination destination = DestinationAccessor.getDestination(DESTINATION_NAME);
        try {
            //clear rfc cache to avoid structural erros on interface changes with cached values
            //RemoteFunctionCache.clearCache(destination);
            //logger.info("***calling destination "+DESTINATION_NAME);
            final RfmRequestResult result = new RfmRequest(fmNameFromPath,false).execute(destination);
            if(result.hasFailed()){
                response.getWriter().write(result.getErrorMessages().toString());
            }else{
                String resp = result.toString();
                logger.info("***great-success "+resp);
                response.getWriter().write(result.getResultElements().toString());
            }
            
        } catch (final RequestExecutionException e) {
            e.printStackTrace(response.getWriter());
        }
    }

}
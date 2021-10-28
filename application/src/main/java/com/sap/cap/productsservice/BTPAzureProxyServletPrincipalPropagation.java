package com.sap.cap.productsservice;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
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
import com.sap.cloud.sdk.cloudplatform.security.BasicCredentials;
import com.sap.cloud.security.xsuaa.client.ClientCredentials;
import com.sap.cloud.security.xsuaa.client.DefaultOAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.XsuaaDefaultEndpoints;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/sso/*")
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Display" }))
public class BTPAzureProxyServletPrincipalPropagation extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(BTPAzureProxyServletPrincipalPropagation.class);
    private static final String DESTINATION_NAME = "s4oauth";
    private static final String OAUTH_DESTINATION_NAME = "s4BasicAuth";
    private static final String ODATA_DESTINATION_NAME = "s4NoAuth";

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {      
        String uriReplaced = request.getRequestURI().trim().split("/sso")[1];
        String queryString = request.getQueryString();

        String url = "";        
        if(queryString != null){
            url = uriReplaced + "?" + queryString;
        }else{
            url = uriReplaced;
        }
        //String UserTokenFromAppRouter = (new JSONObject(System.getenv("test"))).getString("token");
        String UserTokenFromAppRouter = request.getHeader("Authorization").split("Bearer ")[1];
        
        //logger.info("My JWT:::" + UserTokenFromAppRouter + ":::");
        String exchangedTokenForSAMLBearer = getSAMLForXSUAA(UserTokenFromAppRouter);
        //logger.info("xsuaa token for SAML:::" + exchangedTokenForSAMLBearer + ":::");

        String finalBearerToken = getBearerFromSAP(exchangedTokenForSAMLBearer);
        //logger.info("SAP Bearer token :::" + finalBearerToken);
        DefaultHttpClientFactory customFactory = new DefaultHttpClientFactory();
        final HttpDestination destination = DestinationAccessor.getDestination(ODATA_DESTINATION_NAME).asHttp();

        //logger.info("Destination for OAUTH loaded:"+destination.getAuthenticationType().toString());
        HttpClient httpClient = customFactory.createHttpClient(destination);
        
        HttpUriRequest myRequest = new HttpGet(url);
        myRequest.setHeader("Content-Type", request.getContentType());
        //Inject token from SAMLAssertion flow
        myRequest.setHeader("Authorization", "Bearer " + finalBearerToken);

        final HttpResponse productResponse = httpClient.execute(myRequest);

        final HttpEntity entity = productResponse.getEntity();
    
        //post back response
        response.setCharacterEncoding("UTF-8");
        response.setContentType(productResponse.getFirstHeader("Content-Type").getValue());
        response.setStatus(productResponse.getStatusLine().getStatusCode());
        //decoratServletResponseWithSAPHeader(response, productResponse);        
        if (entity != null) {
            final String content = EntityUtils.toString(entity);
            response.getWriter().write(content.toString());
        } else {
            response.getWriter().write("No data available.");
        }
        response.getWriter().write("");
    }

    private JSONObject getCredentialsFor(String xsuaaOrDestination){
        JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
        JSONArray jsonArr = jsonObj.getJSONArray(xsuaaOrDestination);
        return jsonArr.getJSONObject(0).getJSONObject("credentials");
    }

    private String getSAMLForXSUAA(String userJWTToken){

        JSONObject destinationCredentials = getCredentialsFor("destination");
        // get value of "clientid" and "clientsecret" from the environment variables
        String clientid = destinationCredentials.getString("clientid");
        String clientsecret = destinationCredentials.getString("clientsecret");
        
        // get the URL to xsuaa from the environment variables
        JSONObject xsuaaCredentials = getCredentialsFor("xsuaa");
        String xsuaaJWTToken = "";
        URI xsuaaUri = null;
        try {
            xsuaaUri = new URI(xsuaaCredentials.getString("url"));
            // use the XSUAA client library to ease the implementation of the user token exchange flow
            XsuaaTokenFlows tokenFlows = new XsuaaTokenFlows(new DefaultOAuth2TokenService(), new XsuaaDefaultEndpoints(xsuaaUri.toString()), new ClientCredentials(clientid, clientsecret));
            xsuaaJWTToken = tokenFlows.clientCredentialsTokenFlow().execute().getAccessToken();
        } catch (JSONException | URISyntaxException | IllegalArgumentException | TokenFlowException e1) {
            e1.printStackTrace();
        }
        
        URL url;
        int status = 0;
        StringBuffer content = new StringBuffer();
        try {
            //Check Destination Service API for reference: https://api.sap.com/api/SAP_CP_CF_Connectivity_Destination/resource
            url = new URL("https://destination-configuration.cfapps.eu20.hana.ondemand.com/destination-configuration/v1/destinations/"+DESTINATION_NAME);
            HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "Bearer " + xsuaaJWTToken);
            con.setRequestProperty("X-user-token", userJWTToken);
            status = con.getResponseCode();
        
            BufferedReader in = null;

            if (status > 299) {
                in = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject obj = new JSONObject(new JSONTokener(content.toString()));
        JSONObject value = obj.getJSONArray("authTokens").getJSONObject(0);
        String token = value.getString("value");
    
        return token;
    }

    private String getBearerFromSAP(String assertion){
        DefaultHttpClientFactory customFactory = new DefaultHttpClientFactory();
        final HttpDestination oauthDestination = DestinationAccessor.getDestination(OAUTH_DESTINATION_NAME).asHttp();
        HttpClient httpClient = customFactory.createHttpClient(oauthDestination);

        HttpPost myRequest = new HttpPost("/sap/bc/sec/oauth2/token");
        HashMap<String, String> formValues = new HashMap<String, String>();
        formValues.put("grant_type", "urn:ietf:params:oauth:grant-type:saml2-bearer");
        BasicCredentials props = oauthDestination.getBasicCredentials().get();
        formValues.put("client_id", props.getUsername());
        formValues.put("scope", "ZEPM_REF_APPS_PROD_MAN_SRV_0001");
        formValues.put("assertion", assertion);

        JSONObject obj = null;
        try {
            String payload = getDataString(formValues);
            //logger.info("samlbearer-payload:::" + payload);
            StringEntity body = new StringEntity(payload);
            body.setContentType("application/x-www-form-urlencoded");
            myRequest.setEntity(body);            
            final HttpResponse tokenResponse = httpClient.execute(myRequest);
            final int statusCode = tokenResponse.getStatusLine().getStatusCode();
            if(statusCode == HttpStatus.SC_OK){
                String response = EntityUtils.toString(tokenResponse.getEntity());
                //logger.info("response-body:::" + response);
                obj = new JSONObject(new JSONTokener(response));
            }else{
                logger.error("token retrieval from S4 failed, code:" + statusCode + ":" + tokenResponse.getStatusLine().getReasonPhrase());
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return obj.get("access_token").toString();
    }

    private String getDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder stringEntityBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (stringEntityBuilder.length() > 0) {
            stringEntityBuilder.append("&");
            }
            stringEntityBuilder.append(String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue(), "utf-8")));
        }
        return stringEntityBuilder.toString();
    }
}
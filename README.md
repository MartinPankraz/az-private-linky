# az-private-linky
Java Project to get you started with [SAP BTP Private Link Service for Azure](https://help.sap.com/viewer/product/PRIVATE_LINK/CLOUD/en-US) with [SAP Cloud SDK](https://sap.github.io/cloud-sdk/).

This app was built from SAP's Cloud SDK [getting-started Java project](https://developers.sap.com/tutorials/s4sdk-cloud-foundry-sample-application.html#e733958f-50fc-45e3-8f30-d7a53f2c9ad0).

Find my blog post series on the topic [here](https://blogs.sap.com/2021/12/29/getting-started-with-btp-private-link-service-for-azure/).

Additional Resources |
--- |
[Alternative CAP Project](https://github.com/MartinPankraz/az-private-linky-cap) |
[Fiori Project using CAP or Java backend](https://github.com/MartinPankraz/az-products-ui) |
[iFlow example using CAP or Java backend](https://github.com/MartinPankraz/az-private-linky-integration-suite) |
[SAP's tutorial with CF CLI commands](https://developers.sap.com/tutorials/private-link-microsoft-azure.html) |
[SAP's official blog](https://blogs.sap.com/2021/06/28/sap-private-link-service-beta-is-available/) |
[SAP's Discovery Center Mission](https://github.com/SAP-samples/s4hana-btp-extension-devops/tree/mission/05-PrivateLink) |

We used the `/sap/opu/odata/sap/epm_ref_apps_prod_man_srv` OData service for this project.

## Project context
[Azure Private Link Service](https://docs.microsoft.com/en-us/azure/private-link/private-link-service-overview) allows private connectivity between resources running on Azure in different environments. That includes SAP's Business Technology Platform when provisioned on Azure. SAP made that functionality available via a CloudFoundry Service.

Meaning you get now a managed component to expose your SAP backends to BTP on Azure without the need for a Cloud Connector. We developed against S4 primarily but anything executable in a service behind the Azure load balancer would be reachable. That involves for instance ECC, BO, PI/PO, SolMan etc.

![Architecture overview](/application/src/main/webapp/priv-lnk-overview.png)

## BTP Destination config for all scenarios in the blog series
We describe a set of Destinations, whereas the first one refers to the initial simple setup discussed in part 1 of the blog series. The next two are required to realize the SAMLAssertion flow. This additional complexity is due to the fact that the SAML2BearerAssertion flow cannot be used, because the BTP Private Link Service operates isolated from all other BTP services by design. As part of the flow the connectivity service would need to reach the OAuth2 server on the SAP backend but can't because it has no visibility of the private endpoint.

We could get away with 2 destinations, because the target configuration is the same except the authorization header. For a cleaner approach and better separation I decided to have a separate instance to avoid overriding authentication.

In general for end-to-end SSL you need to consider three options:
1) Use __TrustAll__ property setting to accept any certificate
2) Override the verifier within your code (see [BTPAzureProxyServletIgnoreSSL](/application/src/main/java/com/sap/cap/productsservice/BTPAzureProxyServletIgnoreSSL.java))
3) Maintain the trust store of your destination and configure Server Name Indicator (SNI) with your SAP Personal Security Environment (PSE) on your backend (either through STRUST on NetWeaver or SAP Web Dispatcher). Find details on this setup on [part 7](https://blogs.sap.com/2021/12/01/btp-private-linky-swear-with-azure-how-to-setup-ssl-end-to-end-with-private-link-service/) of the blog series.

> In case your [version](https://sap.github.io/cloud-sdk/docs/java/release-notes-sap-cloud-sdk-for-java#3610---january-13-2022) of the CloudSDK doesn't support the new Proxy Type *PrivateLink*, revert to *Internet*. Be aware that this is a configuration topic only. By no means does traffic flow to the Internet. It will be resolved to the private tunnel exposed by PLS.

The last destination describes the setup for SQL connection to the newly added PLS feature scope for Azure PaaS. MariaDB and MySQL have been added first.

### Skip this sub section if you don't want to secure your private linke enabled CF app just yet and proceed with 1
Furthermore you need to deploy the approuter to authenticate through XSUAA and be able to initiate the required token exchange for SAP Principal Propagation. Find more details on the [SAP tutorial](https://developers.sap.com/tutorials/s4sdk-secure-cloudfoundry.html).

- Uncomment login-config, security-constraint and security-role in [web.xml](/application/src/main/webapp/WEB-INF/web.xml)
- Run cf cli command: `cf create-service xsuaa application my-xsuaa -c xs-security.json` to create associated XSUAA instance
- Navigate to approuter project subfolder `cd approuter`
- `cf push`

Going forward you can access your Java Servlets only through the approuter now (if they enforce the `ServletSecurity` annotation).

### 1. Direct OData calls without Principal Propagation or trust store for SSL
key | value |
--- | --- |
Name | s4BasicAuth |
Type | HTTP |
URL | https://[your private hostname]/ |
Proxy Type | PrivateLink |
Authentication | Whatever you have here. We tested Basic Auth initially. (Adjust user id and pwd for SAP Principal Propagation!) |

### Additional Properties
key | value |
--- | --- |
sap-client | your client no |
TrustAll | true |
HTML5.DynamicDestination | true |
WebIDEEnabled | true |
WebIDEUsage | odata_abap |

### 2. Request SAMLAssertion from SAP backend (OAuth server)
key | value |
--- | --- |
Name | s4oauth |
Type | HTTP |
URL | https://[your private hostname]/sap/bc/sec/oauth2/token?sap-client=[your client no] |
Proxy Type | PrivateLink |
Authentication | SAMLAssertion |
Audience | check Provider Name on __SAML2 backend transaction__ |
AuthnContextClassRef | urn:oasis:names:tc:SAML:2.0:ac:classes:x509 |

### Additional Properties
key | value |
--- | --- |
sap-client | your client no |
TrustAll | true |
HTML5.DynamicDestination | true |
WebIDEEnabled | true |
WebIDEUsage | odata_abap |
nameIdFormat | urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress |
tokenServiceURL (type manually and put cursor at beginning to replace capital "T") | https://[your private hostname]/sap/bc/sec/oauth2/token?sap-client=[your client no] |

Be aware that currently the property tokenServiceURL will be hidden after save. Also adding trust store will override your tokenServiceURL setting. So, make sure to maintain it again when you make changes to the additional properties. You can just type it again. It will override if still existing.

### 3. Provide config for final call to OData without Authentication
Hence we need to inject the Bearer token from preceeding calls described in above paragraph.
key | value |
--- | --- |
Name | s4NoAuth |
Type | HTTP |
URL | identical to first destination |
Proxy Type | PrivateLink |
Authentication | No Authentication |

### Additional Properties
key | value |
--- | --- |
sap-client | your client no |
TrustAll | true |
HTML5.DynamicDestination | true |
WebIDEEnabled | true |
WebIDEUsage | odata_abap |

If you configure SAP Principal Propagation based upon above config, please note that you need to change the BasicAuthentication user on the first Destination to match the OAuth2 Client Id and secret (check backend transaction __SOAUTH2__).

I can warmly recommend transaction __sec_diag_tool__ to troubleshoot any Principal Propagation related issues.

#### Testing the SAMLAssertion flow
To get started I recommend to test your authentication flow without BTP private link to rule out any hickups. I provided a [Postman collection](/Templates/BTP_Private_Link_Service_Testing.postman_collection.json) for that. It is meant to be executed from top to bottom.

### 4. WebSocket RFC Destination configuration
key | value |
--- | --- |
Name | s4BasicAuth |
Type | RFC |
Proxy Type | PrivateLink |
User | Your SAP RFC User |
Password | Your SAP RFC User Password|

### Additional Properties
key | value |
--- | --- |
jco.client.client | your SAP client no |
jco.client.lang | potentially define the language for the output or pass down from your CF app |
jco.client.wshost | btp_private_link_hostname that points to your SAP WDisp |
jco.client.wsport | your SAP WDisp port |
jco.destination.pool_capacity | default 1 |

Note WebSocket RFC is available as of S4 1909. Also for initial testing of the RFC connection with JCo, the property __jco.client.tls_trust_all__ might be helpful. Find more details on JCo properties [here](https://help.sap.com/viewer/cca91383641e40ffbe03bdc78f00f681/Cloud/en-US/ab6eac92978f469e9eabe3d477ca2411.html) and from the NEO docs [here](https://help.sap.com/viewer/b865ed651e414196b39f8922db2122c7/Cloud/en-US/8278bed44893498f95d5d6d5f0a47f35.html).

### 5. Piggybacked http destination for SQL connection
key | value |
--- | --- |
Name | AzureMySQLBasic |
Type | HTTP |
URL | https://[your db domain].[mysql or mariadb].database.azure.com:3306 |
Proxy Type | PrivateLink |
User | sql_user@your_db_domain |
Password | Your SQL User Password|

### Additional Properties
key | value |
--- | --- |
HTML5.DynamicDestination | true |

Note: I am not actually using the http destination, but for the lack of another type I chose this one. It serves only as config store for the jdbc connection on the backend. The http part is ignored.

## Get into contact
Reach out via the [GitHub Issues page](https://github.com/MartinPankraz/az-private-linky/issues) of this reposto talk about it some more :-)

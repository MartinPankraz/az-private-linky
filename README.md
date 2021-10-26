# az-private-linky
Java Project to get you started with [SAP BTP Private Link Service for Azure](https://help.sap.com/viewer/product/PRIVATE_LINK/CLOUD/en-US) with [SAP Cloud SDK](https://sap.github.io/cloud-sdk/).

This app was built from SAP's Cloud SDK [getting-started Java project](https://developers.sap.com/tutorials/s4sdk-cloud-foundry-sample-application.html#e733958f-50fc-45e3-8f30-d7a53f2c9ad0).

Find my blog post series on the topic [here]().

Additional Resources |
--- |
[Alternative CAP Project](https://github.com/MartinPankraz/az-private-linky-cap) |
[SAP's tutorial with CF CLI commands](https://developers.sap.com/tutorials/private-link-microsoft-azure.html) |
[SAP's official blog](https://blogs.sap.com/2021/06/28/sap-private-link-service-beta-is-available/) |

We used the `/sap/opu/odata/sap/epm_ref_apps_prod_man_srv` OData service for this project.

## Project context
[Azure Private Link Service](https://docs.microsoft.com/en-us/azure/private-link/private-link-service-overview) allows private connectivity between resources running on Azure in different environments. That includes SAP's Business Technology Platform when provisioned on Azure. SAP made that functionality available via a CloudFoundry Service.

Meaning you get now a managed component to expose your SAP backends to BTP on Azure without the need for a Cloud Connector. I listed a couple of backend targets that might be of interest. We developed against S4 primarily.

![Architecture overview](/application/src/main/webapp/priv-lnk-overview.png)

## Destination config
We describe three Destinations, whereas the first one refers to the initial simple setup discussed in part 1 of the blog series. The other two are required to realize the SAMLAssertion flow. This additional complexity is due to the fact that the SAML2BearerAssertion flow cannot be used, because the BTP Private Link Service operates isolated from all other BTP services by design. As part of the flow the connectivity service would need to reach the OAuth2 server on the SAP backend but can't because it has no visibility of the private endpoint.

We could get away with 2 destinations, because the target configuration is the same except the authorization header. For a cleaner approach and better separation I decided to have a separate instance to avoid overriding authentication.

Please note the __TrustAll__ setting. It is required for https if no further actions are taken.

- You can override the verifier within your code (see [BTPAzureProxyServletIgnoreSSL](/application/src/main/java/com/sap/cap/productsservice/BTPAzureProxyServletIgnoreSSL.java))
- Coming soon: You can maintain the trust store of your destination and configure Server Name Indicator (SNI) with your SAP Personal Security Environment (PSE) on your backend (either through STRUST on NetWeaver or SAP Web Dispatcher). That is not yet possible due to pending host name feature for BTP private link service. I will publish a detailed blog once it is available.

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
URL | https://[your private IP]/ |
Proxy Type | Internet |
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
URL | https://[your private IP]/sap/bc/sec/oauth2/token?sap-client=[your client no] |
Proxy Type | Internet |
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
tokenServiceURL (type manually and put cursor at beginning to replace capital "T") | https://[your private IP]/sap/bc/sec/oauth2/token?sap-client=[your client no] |

Be aware that currently the property tokenServiceURL will be hidden after save. Also adding trust store will override your tokenServiceURL setting. So, make sure to maintain it again when you make changes to the additional properties. You can just type it again. It will override if still existing.

### 3. Provide config for final call to OData without Authentication
Hence we need to inject the Bearer token from preceeding calls.
key | value |
--- | --- |
Name | s4NoAuth |
URL | identical to first destionation |
Proxy Type | Internet |
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

Reach out via the [GitHub Issues page](https://github.com/MartinPankraz/az-private-linky/issues) of this reposto talk about it some more :-)
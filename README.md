# az-private-linky
Java Project to get you started with [SAP BTP Private Link Service for Azure](https://help.sap.com/viewer/product/PRIVATE_LINK/CLOUD/en-US) with [SAP Cloud SDK](https://sap.github.io/cloud-sdk/). Find SAP's tutorial with CF CLI commands [here](https://developers.sap.com/tutorials/private-link-microsoft-azure.html) and their official blog [here](https://blogs.sap.com/2021/06/28/sap-private-link-service-beta-is-available/).

This app was built from SAP's Cloud SDK [getting-started Java project](https://developers.sap.com/tutorials/s4sdk-cloud-foundry-sample-application.html#e733958f-50fc-45e3-8f30-d7a53f2c9ad0).

## Project context
[Azure Private Link Service](https://docs.microsoft.com/en-us/azure/private-link/private-link-service-overview) allows private connectivity between resources running on Azure in different environments. That includes SAP's Business Technology Platform when provisioned on Azure. SAP made that functionality available via a CloudFoundry Service.

Meaning you get now a managed component to expose your SAP backends to BTP on Azure without the need for a Cloud Connector. I listed a couple of backend targets that might be of interest. We developed against S4 primarily.

![Architecture overview](/application/src/main/webapp/priv-lnk-overview.png)

## Destination config
key | value |
--- | --- |
Name | s4test |
URL | https://[your private IP]/ |
Proxy Type | Internet |
Authentication | whatever you have here. We tested Basic Auth initially |

### Additional Properties
key | value |
--- | --- |
sap-client | your client no |
TrustAll | true |
HTML5.DynamicDestination | true |
WebIDEEnabled | true |
WebIDEUsage | odata_abap |

TrustAll needed with https and if no code based approach to override verifier.

Reach out via the [GitHub Issues page](https://github.com/MartinPankraz/az-private-linky/issues) of this reposto talk about it some more :-)
# az-private-linky
Java Project to get you started with [SAP BTP Private Link Service for Azure](https://help.sap.com/viewer/product/PRIVATE_LINK/CLOUD/en-US) with [SAP Cloud SDK](https://sap.github.io/cloud-sdk/).

It was built from SAP's getting-started project [here](https://developers.sap.com/tutorials/s4sdk-cloud-foundry-sample-application.html#e733958f-50fc-45e3-8f30-d7a53f2c9ad0).

## Project context
[Azure Private Link Service](https://docs.microsoft.com/en-us/azure/private-link/private-link-service-overview) allows private connectivity between resources running on Azure in different environments. That includes SAP's Business Technology Platform when provisioned on Azure. SAP made that functionality available via a CloudFoundry Service.

Meaning you get now a managed component to expose your SAP backend to BTP on Azure without the need for a Cloud Connector.

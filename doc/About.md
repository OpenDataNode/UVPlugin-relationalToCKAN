### Description

Uploads internal relational database tables into CKAN datastore

### Configuration parameters

| Name | Description |
|:----|:----|
|**CKAN resource name** | Name of CKAN resource to be created, this has precedence over input from e-distributionMetadata, and if even that is not set, it will use VirtualPath or symbolic name as resource name |
|**Overwrite existing resources** | If resource already exists, it will be rewritten |

### Inputs and outputs

|Name |Type | DataUnit | Description | Mandatory |
|:--------|:------:|:------:|:-------------|:---------------------:|
|tablesInput       |i| RelationalDataUnit | Tables to be uploaded into CKAN datastore |x|
|distributionInput |i| RDFDataUnit | Distribution metadata produced by e-distributionMetadata | |
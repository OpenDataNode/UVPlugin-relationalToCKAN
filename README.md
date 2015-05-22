# L-RelationalToCkan #
----------

###General###

|                              |                                                                             |
|------------------------------|-----------------------------------------------------------------------------|
|**Name:**                     |L-RelationalToCkan                                                           |
|**Description:**              |Uploads internal relational database tables into CKAN datastore              |
|                              |                                                                             |
|**DPU class name:**           |RelationalToCkan                                                             |
|**Configuration class name:** |RelationalToCkanConfig_V1                                                    |
|**Dialogue class name:**      |RelationalToCkanVaadinDialog                                                 |

***

###Dialog configuration parameters###

|Parameter                                       |Description                                                              |
|------------------------------------------------|-------------------------------------------------------------------------|
|**Overwrite existing resources***               |If resource already exists, it will be rewritten.                        |

***

###Configuration parameters###
In order to work properly, this DPU needs configuration parameters properly set in UV backend config.properties

|Parameter                             |Description                             |
|--------------------------------------|----------------------------------------|
|**org.opendatanode.CKAN.secret.token**    |Token used to authenticate to CKAN, has to be set in backend.properties  |
|**org.opendatanode.CKAN.api.url** | URL where CKAN api is located, has to be set in backend.properties |
|**org.opendatanode.CKAN.http.header.[key]** | Custom HTTP header added to requests on CKAN |

####Deprecated parameters###

These parameters are deprecated and kept only for backward compatibility with version 1.0.X.
They will be removed in 1.1.0 of DPU.

|Parameter                                       |Description                                                              |
|---------------------------------------------|-------------------------------------|
|dpu.uv-l-relationalToCkan.secret.token | alias to _org.opendatanode.CKAN.secret.token_  |
|dpu.uv-l-relationalToCkan.catalog.api.url | alias to _org.opendatanode.CKAN.api.url_ |
|dpu.uv-l-relationalToCkan.http.header.[key] | alias to _org.opendatanode.CKAN.http.header.[key]_ |

####Examples####
```INI
org.opendatanode.CKAN.secret.token = 12345678901234567890123456789012
org.opendatanode.CKAN.api.url = ﻿http://localhost:9080/internalcatalog/api/action/internal_api
```

***

### Inputs and outputs ###

|Name          |Type           |DataUnit           |Description                                  |
|--------------|---------------|-------------------|---------------------------------------------|
|tablesInput   |i              |RelationalDataUnit |Tables to be uploaded into CKAN datastore    |

***

### Version history ###

|Version          |Release notes               |
|-----------------|----------------------------|
|1.0.2            | unification of config parameters |
|1.0.1            | bug fixes and update in build dependencies |
|1.0.0            | First version              |


***

### Developer's notes ###

|Author           |Notes                           |
|-----------------|--------------------------------|
|N/A              |N/A                             |

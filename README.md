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

###Configuration parameters###

|Parameter                                       |Description                                                              |
|------------------------------------------------|-------------------------------------------------------------------------|
|**CKAN resource name**                          |Name of CKAN resource to be created                                      |
|**Overwrite existing resources***               |If resource already exists, it will be rewritten.                        |

***

###Global UnifiedViews configuration###
In order to work properly, this DPU needs configuration parameters properly set in UV backend config.properties

|Parameter                                       |Description                                                              |
|------------------------------------------------|-------------------------------------------------------------------------|
|dpu.uv-l-relationalToCkan.catalog.api.url   |URL of CKAN catalog with proper UV extension installed                   |
|dpu.uv-l-relationalToCkan.secret.token      |Authentication token to CKAN                                             |

***

### Inputs and outputs ###

|Name          |Type           |DataUnit           |Description                                  |
|--------------|---------------|-------------------|---------------------------------------------|
|tablesInput   |i              |RelationalDataUnit |Tables to be uploaded into CKAN datastore    |

***

### Version history ###

|Version          |Release notes               |
|-----------------|----------------------------|
|1.1.0            | Resource name in CKAN is now configured by user; Only one table can be processed by DPU |
|1.0.1            | bug fixes and update in build dependencies |
|1.0.0            | First version              |


***

### Developer's notes ###

|Author           |Notes                           |
|-----------------|--------------------------------|
|N/A              |N/A                             |

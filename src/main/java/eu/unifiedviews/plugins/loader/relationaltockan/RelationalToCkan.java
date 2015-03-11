package eu.unifiedviews.plugins.loader.relationaltockan;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.unifiedviews.dataunit.DataUnit;
import eu.unifiedviews.dataunit.DataUnitException;
import eu.unifiedviews.dataunit.relational.RelationalDataUnit;
import eu.unifiedviews.dpu.DPU;
import eu.unifiedviews.dpu.DPUContext;
import eu.unifiedviews.dpu.DPUException;
import eu.unifiedviews.helpers.dataunit.relationalhelper.RelationalHelper;
import eu.unifiedviews.helpers.dataunit.resourcehelper.Resource;
import eu.unifiedviews.helpers.dataunit.resourcehelper.ResourceConverter;
import eu.unifiedviews.helpers.dataunit.resourcehelper.ResourceHelpers;
import eu.unifiedviews.helpers.dpu.config.AbstractConfigDialog;
import eu.unifiedviews.helpers.dpu.config.ConfigDialogProvider;
import eu.unifiedviews.helpers.dpu.config.ConfigurableBase;
import eu.unifiedviews.helpers.dpu.localization.Messages;

/**
 * Loader of relational data into external CKAN catalog
 * This DPU loads all input database tables into resources in dataset mapped in CKAN to pipeline ID
 * By default, if resource with the same name exists, creating of the resource and datastore is skipped for this table.
 * In overwrite mode, all resources are updated and datastore is deleted and created again
 */
@DPU.AsLoader
public class RelationalToCkan extends ConfigurableBase<RelationalToCkanConfig_V1> implements ConfigDialogProvider<RelationalToCkanConfig_V1> {

    private static Logger LOG = LoggerFactory.getLogger(RelationalToCkan.class);

    public static final String PROXY_API_ACTION = "action";

    public static final String PROXY_API_PIPELINE_ID = "pipeline_id";

    public static final String PROXY_API_USER_ID = "user_id";

    public static final String PROXY_API_TOKEN = "token";

    public static final String CKAN_API_URL_TYPE = "url_type";

    public static final String CKAN_API_URL_TYPE_DATASTORE = "datastore";

    public static final String CKAN_API_PACKAGE_SHOW = "package_show";

    public static final String CKAN_API_RESOURCE_UPDATE = "resource_update";

    public static final String CKAN_API_RESOURCE_CREATE = "resource_create";

    public static final String CKAN_API_DATASTORE_CREATE = "datastore_create";

    public static final String CKAN_API_DATASTORE_DELETE = "datastore_delete";

    public static final String PROXY_API_STORAGE_ID = "storage_id";

    public static final String PROXY_API_DATA = "data";

    public static final String CKAN_DATASTORE_RESOURCE_ID = "resource_id";

    public static final String CKAN_DATASTORE_FIELDS = "fields";

    public static final String CKAN_DATASTORE_RECORDS = "records";

    public static final String CKAN_DATASTORE_PRIMARY_KEY = "primary_key";

    public static final String CKAN_DATASTORE_INDEXES = "indexes";

    public static final String SECRET_TOKEN = "dpu.l-relationalToCkan.secret.token";

    private Messages messages;

    private DPUContext context;

    @DataUnit.AsInput(name = "tablesInput")
    public RelationalDataUnit tablesInput;

    public RelationalToCkan() {
        super(RelationalToCkanConfig_V1.class);
    }

    @Override
    public void execute(DPUContext context) throws DPUException, InterruptedException {
        this.context = context;
        this.messages = new Messages(this.context.getLocale(), this.getClass().getClassLoader());

        String shortMessage = this.messages.getString("dpu.ckan.starting", this.getClass().getSimpleName());
        String longMessage = String.valueOf(this.config);
        this.context.sendMessage(DPUContext.MessageType.INFO, shortMessage, longMessage);

        Map<String, String> environment = this.context.getEnvironment();
        long pipelineId = this.context.getPipelineId();
        String userId = this.context.getPipelineOwner();
        String token = environment.get(SECRET_TOKEN);
        if (token == null || token.isEmpty()) {
            throw new DPUException(this.messages.getString("errors.token.missing"));
        }
        String catalogApiLocation = environment.get("dpu.l-relationalToCkan.catalog.api.location");
        if (catalogApiLocation == null || catalogApiLocation.isEmpty()) {
            throw new DPUException(this.messages.getString("errors.api.missing"));
        }

        CatalogApiConfig apiConfig = new CatalogApiConfig(catalogApiLocation, pipelineId, userId, token);

        Iterator<RelationalDataUnit.Entry> tablesIteration;
        try {
            tablesIteration = RelationalHelper.getTables(this.tablesInput).iterator();
        } catch (DataUnitException ex) {
            this.context.sendMessage(DPUContext.MessageType.ERROR,
                    this.messages.getString("errors.dpu.failed"), this.messages.getString("errors.tables.iterator"), ex);
            return;
        }

        Map<String, String> existingResources = getExistingResources(apiConfig);

        try {
            while (!this.context.canceled() && tablesIteration.hasNext()) {
                final RelationalDataUnit.Entry entry = tablesIteration.next();
                final String sourceTableName = entry.getTableName();
                LOG.debug("Going to load table {} to CKAN dataset as resource", sourceTableName);
                try {
                    if (existingResources.containsKey(sourceTableName)) {
                        if (this.config.isOverWriteTables()) {
                            LOG.info("Resource already exists, overwrite mode is enabled -> resource will be updated");
                            String resourceId = existingResources.get(sourceTableName);
                            updateCkanResource(entry, resourceId, apiConfig);
                            deleteCkanDatastore(resourceId, apiConfig);
                            createDatastoreFromTable(entry, resourceId, apiConfig);
                            LOG.info("Resource and datastore for table {} successfully updated", sourceTableName);
                            this.context.sendMessage(DPUContext.MessageType.INFO,
                                    this.messages.getString("dpu.resource.updated", sourceTableName));

                        } else {
                            LOG.info("Resource already exists, overwrite mode is disabled -> fail");
                            this.context.sendMessage(DPUContext.MessageType.WARNING,
                                    this.messages.getString("dpu.resource.skipped.short", sourceTableName),
                                    this.messages.getString("dpu.resource.skipped.long"));
                            continue;
                        }
                    } else {
                        LOG.info("Resource does not exist yet, it will be created");
                        String resourceId = createCkanResource(entry, apiConfig);
                        createDatastoreFromTable(entry, resourceId, apiConfig);
                        LOG.info("Resource and datastore for table {} successfully created", sourceTableName);
                        this.context.sendMessage(DPUContext.MessageType.INFO,
                                this.messages.getString("dpu.resource.created", sourceTableName));
                    }
                } catch (Exception e) {
                    LOG.error("Failed to create resource / datastore for table {}", sourceTableName, e);
                    this.context.sendMessage(DPUContext.MessageType.ERROR,
                            this.messages.getString("dpu.resource.upload", sourceTableName));
                }
            }
        } catch (DataUnitException e) {
            throw new DPUException(this.messages.getString("errors.dpu.upload"), e);
        }
    }

    /**
     * Get a map of existing resources for this pipeline. Key in this map is the name of the resource,
     * in this case it is database table name. Value is resource ID - unique identifier of the resource, generated by CKAN
     * 
     * @return Map of existing resources from CKAN. Key is the name (table name), value is resource ID
     * @throws DPUException
     *             If error occurs obtaining resources from CKAN
     */
    private Map<String, String> getExistingResources(CatalogApiConfig apiConfig) throws DPUException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        Map<String, String> existingResources = new HashMap<>();
        try {
            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());

            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, CKAN_API_PACKAGE_SHOW, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_PIPELINE_ID, String.valueOf(apiConfig.getPipelineId()), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, "{}", ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();
            httpPost.setEntity(entity);
            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new DPUException(this.messages.getString("dpu.resource.dataseterror", EntityUtils.toString(response.getEntity())));
            }

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject dataset = reader.readObject();
            JsonArray resources = dataset.getJsonObject("result").getJsonArray("resources");
            for (JsonObject resource : resources.getValuesAs(JsonObject.class)) {
                existingResources.put(resource.getString("name"), resource.getString("id"));
            }
        } catch (ParseException | URISyntaxException | IllegalStateException | IOException ex) {
            throw new DPUException(this.messages.getString("errors.dpu.dataset"), ex);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }

        return existingResources;
    }

    /**
     * Create CKAN resource from internal relational database table.
     * This method should be called only in case resource for given table does not exist yet.
     * 
     * @param table
     *            Internal input database table, from which the resource should be created
     * @return Resource ID of the created resource
     * @throws Exception
     *             If problem occurs during creating of the resource
     */
    private String createCkanResource(RelationalDataUnit.Entry table, CatalogApiConfig apiConfig) throws Exception {
        String resourceId = null;

        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            String storageId = table.getTableName();
            Resource resource = ResourceHelpers.getResource(this.tablesInput, table.getSymbolicName());
            resource.setName(storageId);
            JsonObjectBuilder resourceBuilder = buildResource(factory, resource);

            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());

            MultipartEntityBuilder builder = buildCommonResourceParams(table, apiConfig);
            builder.addTextBody(PROXY_API_DATA, resourceBuilder.build().toString(), ContentType.APPLICATION_JSON.withCharset("UTF-8"));
            builder.addTextBody(PROXY_API_ACTION, CKAN_API_RESOURCE_CREATE, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
                JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
                JsonObject createdResource = reader.readObject().getJsonObject("result");
                if (!createdResource.containsKey("id")) {
                    throw new Exception("Missing resource ID of the newly created CKAN resource");
                }
                resourceId = createdResource.getString("id");
            } else {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
                throw new Exception("Failed to create CKAN resource");
            }
        } catch (ParseException | IOException | DataUnitException | URISyntaxException e) {
            throw new Exception("Failed to create CKAN resource", e);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }

        return resourceId;
    }

    /**
     * Update existing CKAN resource based on internal relational database table.
     * This method should be called only in case resource for given already exists and its resource ID is known
     * 
     * @param table
     *            Internal input database table, based on which the resource should be updated
     * @return Resource ID of the created resource
     * @throws Exception
     *             If problem occurs during updating of the resource
     */
    private void updateCkanResource(RelationalDataUnit.Entry table, String resourceId, CatalogApiConfig apiConfig) throws Exception {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            String storageId = table.getTableName();
            Resource resource = ResourceHelpers.getResource(this.tablesInput, table.getSymbolicName());
            resource.setName(storageId);
            JsonObjectBuilder resourceBuilder = buildResource(factory, resource);
            resourceBuilder.add("id", resourceId);

            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());

            MultipartEntityBuilder builder = buildCommonResourceParams(table, apiConfig);
            builder.addTextBody(PROXY_API_DATA, resourceBuilder.build().toString(), ContentType.APPLICATION_JSON.withCharset("UTF-8"));
            builder.addTextBody(PROXY_API_ACTION, CKAN_API_RESOURCE_UPDATE, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Response:" + EntityUtils.toString(response.getEntity()));
            } else {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
                throw new Exception("Failed to update CKAN resource");
            }
        } catch (ParseException | IOException | DataUnitException | URISyntaxException e) {
            throw new Exception("Error updating resource", e);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }
    }

    // TODO: maybe data should be split into several bulks if large data
    // Otherwise we can have problems with memory / connection
    /**
     * Create CKAN datastore from input internal relational database table.
     * It obtains all the information about columns, column types, primary keys and indexes.
     * It creates the corresponding datastore definition and also sends all the data from the table
     * 
     * @param table
     *            Input database table, from which the datastore should be created
     * @param resourceId
     *            CKAN resource ID
     * @throws Exception
     *             If error occurs during creating of the datastore definition or data upload
     */
    private void createDatastoreFromTable(RelationalDataUnit.Entry table, String resourceId, CatalogApiConfig apiConfig) throws Exception {
        Connection conn = null;
        ResultSet tableData = null;
        Statement stmnt = null;
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String sourceTableName = table.getTableName();
        try {

            conn = this.tablesInput.getDatabaseConnection();
            List<ColumnDefinition> columns = RelationalToCkanHelper.getColumnsForTable(conn, sourceTableName);
            JsonArray fields = RelationalToCkanHelper.buildFieldsDefinitionJson(columns);

            stmnt = conn.createStatement();
            tableData = stmnt.executeQuery("SELECT * FROM " + sourceTableName);
            JsonArray records = RelationalToCkanHelper.buildRecordsJson(tableData, columns);

            List<String> indexes = RelationalToCkanHelper.getTableIndexes(conn, sourceTableName);
            List<String> primaryKeys = RelationalToCkanHelper.getTablePrimaryKeys(conn, sourceTableName);
            JsonObject dataStoreParams = RelationalToCkanHelper.buildCreateDataStoreParameters(resourceId, indexes, primaryKeys,
                    fields, records);

            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());

            MultipartEntityBuilder builder = buildCommonResourceParams(table, apiConfig);
            builder.addTextBody(PROXY_API_DATA, dataStoreParams.toString(), ContentType.APPLICATION_JSON.withCharset("UTF-8"));
            builder.addTextBody(PROXY_API_ACTION, CKAN_API_DATASTORE_CREATE, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
                throw new Exception("Failed to create CKAN datastore");
            }
        } catch (DataUnitException | SQLException | URISyntaxException | IOException ex) {
            throw new Exception("Failed to create CKAN datastore", ex);
        } finally {
            RelationalToCkanHelper.tryCloseDbResources(conn, stmnt, tableData);
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }
    }

    /**
     * Deletes existing datastore belonging to the resource defined by provided resource ID
     * 
     * @param resourceId
     *            CKAN resource ID
     * @throws Exception
     *             If error occurs
     */
    private void deleteCkanDatastore(String resourceId, CatalogApiConfig apiConfig) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(apiConfig.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());

            JsonObject deleteDataStoreParams = RelationalToCkanHelper.buildDeleteDataStoreParamters(resourceId);

            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, CKAN_API_DATASTORE_DELETE, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_PIPELINE_ID, String.valueOf(apiConfig.getPipelineId()), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_DATA, deleteDataStoreParams.toString(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Response:" + EntityUtils.toString(response.getEntity()));
            } else {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
                throw new Exception("Failed to delete CKAN datastore");
            }
        } catch (ParseException | IOException | URISyntaxException e) {
            throw new Exception("Failed to delete CKAN datastore", e);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }
    }

    /**
     * Builds commom multipart parameters for resource API calls
     * 
     * @param table
     *            Input database table name
     * @return Common multi-part parameters
     * @throws DataUnitException
     *             If error occurs
     */
    private MultipartEntityBuilder buildCommonResourceParams(RelationalDataUnit.Entry table, CatalogApiConfig apiConfig) throws DataUnitException {
        String storageId = table.getTableName();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody(PROXY_API_PIPELINE_ID, String.valueOf(apiConfig.getPipelineId()), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_USER_ID, apiConfig.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_TOKEN, apiConfig.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_STORAGE_ID, storageId, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

        return builder;
    }

    /**
     * Create JSON object builder for resource
     * 
     * @param factory
     *            JsonBuilderFactory to be used to create Json objects
     * @param resource
     *            Resource to create Json builder from
     * @return JSON representation of provided resource
     */
    private JsonObjectBuilder buildResource(JsonBuilderFactory factory, Resource resource) {

        JsonObjectBuilder resourceBuilder = factory.createObjectBuilder();
        for (Map.Entry<String, String> mapEntry : ResourceConverter.resourceToMap(resource).entrySet()) {
            resourceBuilder.add(mapEntry.getKey(), mapEntry.getValue());
        }

        Map<String, String> extrasMap = ResourceConverter.extrasToMap(resource.getExtras());
        if (extrasMap != null && !extrasMap.isEmpty()) {
            JsonObjectBuilder resourceExtrasBuilder = factory.createObjectBuilder();
            for (Map.Entry<String, String> mapEntry : extrasMap.entrySet()) {
                resourceExtrasBuilder.add(mapEntry.getKey(), mapEntry.getValue());
            }
            resourceBuilder.add("extras", resourceExtrasBuilder);
        }

        resourceBuilder.add(CKAN_API_URL_TYPE, CKAN_API_URL_TYPE_DATASTORE);
        // just dummy URL, it will be overwritten in CKAN
        resourceBuilder.add("url", "datastore");

        return resourceBuilder;
    }

    @Override
    public AbstractConfigDialog<RelationalToCkanConfig_V1> getConfigurationDialog() {
        return new RelationalToCkanVaadinDialog();
    }

}

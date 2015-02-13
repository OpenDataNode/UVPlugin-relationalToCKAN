package eu.unifiedviews.plugins.loader.relationaltockan;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
 * @author Tomas
 */
@DPU.AsLoader
public class RelationalToCkan extends ConfigurableBase<RelationalToCkanConfig_V1> implements ConfigDialogProvider<RelationalToCkanConfig_V1> {

    private static Logger LOG = LoggerFactory.getLogger(RelationalToCkan.class);

    public static final String PROXY_API_ACTION = "action";

    public static final String PROXY_API_PIPELINE_ID = "pipeline_id";

    public static final String PROXY_API_USER_ID = "user_id";

    public static final String PROXY_API_TOKEN = "token";

    public static final String CKAN_API_PACKAGE_SHOW = "package_show";

    public static final String CKAN_API_RESOURCE_UPDATE = "resource_update";

    public static final String CKAN_API_RESOURCE_CREATE = "resource_create";

    public static final String PROXY_API_STORAGE_ID = "storage_id";

    public static final String PROXY_API_DATA = "data";

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

        String shortMessage = this.messages.getString("dpu.ckan.starting", this.getClass().getSimpleName()) + " starting.";
        String longMessage = String.valueOf(this.config);
        this.context.sendMessage(DPUContext.MessageType.INFO, shortMessage, longMessage);

        Iterator<RelationalDataUnit.Entry> tablesIteration;
        try {
            tablesIteration = RelationalHelper.getTables(this.tablesInput).iterator();
        } catch (DataUnitException ex) {
            this.context.sendMessage(DPUContext.MessageType.ERROR, this.messages.getString("errors.dpu.failed"), this.messages.getString("errors.tables.iterator"), ex);
            return;
        }

        Map<String, String> existingResources = getExistingResources();

        try {
            while (!this.context.canceled() && tablesIteration.hasNext()) {
                final RelationalDataUnit.Entry entry = tablesIteration.next();
                final String sourceTableName = entry.getTableName();
                LOG.debug("Going to load table {} to CKAN dataset as resource", sourceTableName);
                try {
                    if (existingResources.containsKey(sourceTableName)) {
                        if (this.config.getOverWriteTables()) {
                            LOG.info("Resource already exists, overwrite mode is enabled -> resource will be updated");
                            String resourceId = existingResources.get(sourceTableName);
                            updateCkanResource(entry, resourceId);
                            createDatastoreFromTable(sourceTableName, resourceId);
                            LOG.info("Resource and datastore for table {} successfully updated", sourceTableName);
                        } else {
                            LOG.info("Resource already exists, overwrite mode is disabled -> fail");
                            this.context.sendMessage(DPUContext.MessageType.WARNING,
                                    this.messages.getString("dpu.resource.skipped.short", sourceTableName),
                                    this.messages.getString("dpu.resource.skipped.long"));
                            continue;
                        }
                    } else {
                        LOG.info("Resource does not exist yet, it will be created");
                        String resourceId = createCkanResource(entry);
                        createDatastoreFromTable(sourceTableName, resourceId);
                        LOG.info("Resource and datastore for table {} successfully created", sourceTableName);
                    }
                } catch (Exception e) {
                    this.context.sendMessage(DPUContext.MessageType.ERROR,
                            this.messages.getString("dpu.", "dpu.resource.upload"), sourceTableName);
                }
            }
        } catch (DataUnitException e) {
            throw new DPUException(this.messages.getString("errors.dpu.upload"), e);
        }

    }

    private void createDatastoreFromTable(String sourceTableName, String resourceId) {
        // TODO Auto-generated method stub

    }

    private Map<String, String> getExistingResources() throws DPUException {
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        Map<String, String> existingResources = new HashMap<>();
        try {
            URIBuilder uriBuilder;
            uriBuilder = new URIBuilder(this.config.getCatalogApiLocation());

            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addTextBody(PROXY_API_ACTION, CKAN_API_PACKAGE_SHOW, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_PIPELINE_ID, String.valueOf(this.config.getPipelineId()), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_USER_ID, this.config.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .addTextBody(PROXY_API_TOKEN, this.config.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                    .build();
            httpPost.setEntity(entity);
            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new DPUException("Could not obtain Dataset entity from CKAN. Request: " + EntityUtils.toString(entity) + " Response:" + EntityUtils.toString(response.getEntity()));
            }

            JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
            JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
            JsonObject dataset = reader.readObject();
            JsonArray resources = dataset.getJsonArray("resources");
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

    private void updateCkanResource(RelationalDataUnit.Entry table, String resourceId) throws Exception {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            String storageId = table.getTableName();
            Resource resource = ResourceHelpers.getResource(this.tablesInput, table.getSymbolicName());
            resource.setName(storageId);
            JsonObjectBuilder resourceBuilder = buildResource(factory, resource);
            resourceBuilder.add("id", resourceId);

            URIBuilder uriBuilder = new URIBuilder(this.config.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());

            MultipartEntityBuilder builder = buildCommonResourceParams(table, resourceBuilder);
            builder.addTextBody(PROXY_API_ACTION, CKAN_API_RESOURCE_UPDATE, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Response:" + EntityUtils.toString(response.getEntity()));
            } else {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
            }
        } catch (ParseException | IOException | DataUnitException | URISyntaxException e) {
            throw new Exception("Error exporting metadata", e);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }
    }

    private String createCkanResource(RelationalDataUnit.Entry table) throws Exception {
        String resourceId = null;

        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            String storageId = table.getTableName();
            Resource resource = ResourceHelpers.getResource(this.tablesInput, table.getSymbolicName());
            resource.setName(storageId);
            JsonObjectBuilder resourceBuilder = buildResource(factory, resource);

            URIBuilder uriBuilder = new URIBuilder(this.config.getCatalogApiLocation());
            uriBuilder.setPath(uriBuilder.getPath());
            HttpPost httpPost = new HttpPost(uriBuilder.build().normalize());

            MultipartEntityBuilder builder = buildCommonResourceParams(table, resourceBuilder);
            builder.addTextBody(PROXY_API_ACTION, CKAN_API_RESOURCE_CREATE, ContentType.TEXT_PLAIN.withCharset("UTF-8"));

            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);

            response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("Response:" + EntityUtils.toString(response.getEntity()));
                JsonReaderFactory readerFactory = Json.createReaderFactory(Collections.<String, Object> emptyMap());
                JsonReader reader = readerFactory.createReader(response.getEntity().getContent());
                resourceId = reader.readObject().getString("id");
            } else {
                LOG.error("Response:" + EntityUtils.toString(response.getEntity()));
            }
        } catch (ParseException | IOException | DataUnitException | URISyntaxException e) {
            throw new Exception("Error exporting metadata", e);
        } finally {
            RelationalToCkanHelper.tryCloseHttpResponse(response);
            RelationalToCkanHelper.tryCloseHttpClient(client);
        }

        return resourceId;
    }

    private MultipartEntityBuilder buildCommonResourceParams(RelationalDataUnit.Entry table, JsonObjectBuilder resourceBuilder) throws DataUnitException {
        String storageId = table.getTableName();

        MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                .addTextBody(PROXY_API_PIPELINE_ID, String.valueOf(this.config.getPipelineId()), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_USER_ID, this.config.getUserId(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_TOKEN, this.config.getToken(), ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_STORAGE_ID, storageId, ContentType.TEXT_PLAIN.withCharset("UTF-8"))
                .addTextBody(PROXY_API_DATA, resourceBuilder.build().toString(), ContentType.APPLICATION_JSON.withCharset("UTF-8"));

        return builder;
    }

    private JsonObjectBuilder buildResource(JsonBuilderFactory factory, Resource resource) {
        JsonObjectBuilder resourceExtrasBuilder = factory.createObjectBuilder();
        for (Map.Entry<String, String> mapEntry : ResourceConverter.extrasToMap(resource.getExtras()).entrySet()) {
            resourceExtrasBuilder.add(mapEntry.getKey(), mapEntry.getValue());
        }

        JsonObjectBuilder resourceBuilder = factory.createObjectBuilder();
        for (Map.Entry<String, String> mapEntry : ResourceConverter.resourceToMap(resource).entrySet()) {
            resourceBuilder.add(mapEntry.getKey(), mapEntry.getValue());
        }
        resourceBuilder.add("extras", resourceExtrasBuilder);

        return resourceBuilder;
    }

    @Override
    public AbstractConfigDialog<RelationalToCkanConfig_V1> getConfigurationDialog() {
        return new RelationalToCkanVaadinDialog();
    }

}

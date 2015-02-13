package eu.unifiedviews.plugins.loader.relationaltockan;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationalToCkanHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RelationalToCkanHelper.class);

    public static List<ColumnDefinition> getColumnsForTable(Connection conn, String tableName) throws SQLException {
        ResultSet columns = null;
        List<ColumnDefinition> columnDefinitions = new ArrayList<>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            columns = meta.getColumns(null, null, tableName, null);
            while (columns.next()) {
                ColumnDefinition cd = new ColumnDefinition(
                        columns.getString("COLUMN_NAME"),
                        columns.getString("TYPE_NAME"),
                        columns.getInt("DATA_TYPE"));
                columnDefinitions.add(cd);
            }
        } finally {
            tryCloseResultSet(columns);
        }

        return columnDefinitions;
    }

    public static List<String> getTableIndexes(Connection conn, String tableName) throws SQLException {
        List<String> columnIndexes = new ArrayList<>();
        ResultSet indexes = null;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            indexes = meta.getIndexInfo(null, null, tableName, false, false);
            while (indexes.next()) {
                String indexedColumn = indexes.getString("COLUMN_NAME");
                if (indexedColumn != null) {
                    columnIndexes.add(indexedColumn);
                }
            }
        } finally {
            tryCloseResultSet(indexes);
        }

        return columnIndexes;
    }

    public static void tryCloseDbResources(Connection conn, Statement stmnt, ResultSet rs) {
        tryCloseResultSet(rs);
        tryCloseStatement(stmnt);
        tryCloseConnection(conn);
    }

    public static void tryRollbackConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOG.warn("Error occurred during rollback of connection", e);
            }
        }
    }

    public static void tryCloseStatement(Statement stmnt) {
        try {
            if (stmnt != null) {
                stmnt.close();
            }
        } catch (SQLException e) {
            LOG.warn("Error occurred during closing statement", e);
        }
    }

    public static void tryCloseResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            LOG.warn("Error occurred during closing result set", e);
        }
    }

    public static void tryCloseConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.warn("Error occurred during closing result set", e);
            }
        }
    }

    public static void tryCloseHttpClient(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP client", e);
            }
        }
    }

    public static void tryCloseHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                LOG.warn("Failed to close HTTP response", e);
            }
        }
    }

    public static JsonObject buildDataStoreParameters(String resourceId, List<String> indexes, List<String> primaryKeys, JsonArray fields, JsonArray records) {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        JsonObjectBuilder dataStoreBuilder = factory.createObjectBuilder();
        dataStoreBuilder.add(RelationalToCkan.CKAN_DATASTORE_RESOURCE_ID, resourceId)
                .add(RelationalToCkan.CKAN_DATASTORE_FIELDS, fields)
                .add(RelationalToCkan.CKAN_DATASTORE_RECORDS, records);

        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            JsonArrayBuilder primaryKeysArray = factory.createArrayBuilder();
            for (String key : primaryKeys) {
                primaryKeysArray.add(key);
            }
            dataStoreBuilder.add(RelationalToCkan.CKAN_DATASTORE_PRIMARY_KEY, primaryKeysArray);
        }

        if (indexes != null && !indexes.isEmpty()) {
            JsonArrayBuilder indexesArray = factory.createArrayBuilder();
            for (String index : indexes) {
                indexesArray.add(index);
            }
            dataStoreBuilder.add(RelationalToCkan.CKAN_DATASTORE_INDEXES, indexesArray);
        }

        return dataStoreBuilder.build();
    }

    public static JsonArray buildFieldsDefinitionJson(List<ColumnDefinition> columns) {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        JsonArrayBuilder fieldsBuilder = factory.createArrayBuilder();

        for (ColumnDefinition column : columns) {
            fieldsBuilder.add(factory.createObjectBuilder()
                    .add("id", column.getColumnName())
                    .add("type", column.getColumnTypeName()));
        }

        return fieldsBuilder.build();
    }

    public static JsonArray buildRecordsJson(ResultSet rs, List<ColumnDefinition> columns) throws SQLException {
        JsonBuilderFactory factory = Json.createBuilderFactory(Collections.<String, Object> emptyMap());
        JsonArrayBuilder recordsBuilder = factory.createArrayBuilder();
        while (rs.next()) {
            JsonObjectBuilder entryBuilder = factory.createObjectBuilder();
            for (ColumnDefinition column : columns) {
                switch (column.getColumnType()) {
                    case Types.INTEGER:
                        entryBuilder.add(column.getColumnName(), rs.getInt(column.getColumnName()));
                        break;
                    case Types.BIGINT:
                        entryBuilder.add(column.getColumnName(), rs.getLong(column.getColumnName()));
                        break;

                    case Types.DECIMAL:
                    case Types.NUMERIC:
                        entryBuilder.add(column.getColumnName(), rs.getBigDecimal(column.getColumnName()));
                        break;

                    case Types.FLOAT:
                    case Types.REAL:
                    case Types.DOUBLE:
                        entryBuilder.add(column.getColumnName(), rs.getDouble(column.getColumnName()));
                        break;

                    case Types.NVARCHAR:
                    case Types.VARCHAR:
                    case Types.LONGNVARCHAR:
                    case Types.LONGVARCHAR:
                        entryBuilder.add(column.getColumnName(), rs.getString(column.getColumnName()));
                        break;

                    case Types.BOOLEAN:
                    case Types.BIT:
                        entryBuilder.add(column.getColumnName(), rs.getBoolean(column.getColumnName()));
                        break;

                    case Types.TINYINT:
                    case Types.SMALLINT:
                        entryBuilder.add(column.getColumnName(), rs.getShort(column.getColumnName()));
                        break;

                    case Types.DATE:
                        entryBuilder.add(column.getColumnName(), rs.getDate(column.getColumnName()).toString());
                        break;

                    case Types.TIMESTAMP:
                        entryBuilder.add(column.getColumnName(), rs.getTime(column.getColumnName()).toString());
                        break;

                    case Types.ARRAY:
                        entryBuilder.add(column.getColumnName(), getSqlArrayAsJsonArray(factory, rs.getArray(column.getColumnName())));
                        break;

                    case Types.BLOB:
                    case Types.CLOB:
                        // TODO: implement BLOB/CLOB conversion
                        entryBuilder.addNull(column.getColumnName());
                        break;

                    case Types.BINARY:
                    case Types.VARBINARY:
                    case Types.LONGVARBINARY:
                        //TODO: implement binary formats conversion
                        entryBuilder.addNull(column.getColumnName());
                        break;

                    case Types.STRUCT:
                    case Types.DISTINCT:
                    case Types.REF:
                    case Types.JAVA_OBJECT:
                    default:
                        // TODO: which of these to implement?
                        entryBuilder.addNull(column.getColumnName());
                        break;
                }
            }
            recordsBuilder.add(entryBuilder);
        }

        return recordsBuilder.build();
    }

    private static JsonArrayBuilder getSqlArrayAsJsonArray(JsonBuilderFactory factory, Array array) throws SQLException {
        JsonArrayBuilder jsonArray = factory.createArrayBuilder();
        ResultSet rs = array.getResultSet();
        while (rs.next()) {
            jsonArray.add(rs.getObject(2).toString());
        }

        return jsonArray;

    }

    public static void main(String[] args) {
        ColumnDefinition def1 = new ColumnDefinition("id", "integer", 12);
        ColumnDefinition def2 = new ColumnDefinition("name", "varchar", 11);
        List<ColumnDefinition> defs = new ArrayList<>();
        defs.add(def1);
        defs.add(def2);
        JsonArray array = buildFieldsDefinitionJson(defs);
        System.out.println(array.toString());
    }

}

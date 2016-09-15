package org.opendatanode.plugins.loader.relationaltockan;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RelationalToCkanHelperTest {

    private static final ColumnDefinition COLUMN_1 = new ColumnDefinition("id", "Integer", 4);

    private static final ColumnDefinition COLUMN_2 = new ColumnDefinition("name", "Varchar", 12);

    private static final ColumnDefinition COLUMN_3 = new ColumnDefinition("surname", "Varchar", 12);

    private static final ColumnDefinition COLUMN_4 = new ColumnDefinition("birthdate", "Date", 91);

    private static final ColumnDefinition COLUMN_5 = new ColumnDefinition("resident", "Boolean", 16);

    private static final List<String> PRIMARY_KEYS = Arrays.asList("id");

    private static final List<String> INDEXES = Arrays.asList("id", "birthdate");

    private static final String DATE_STRING = "2015-02-13";

    private static final String RESOURCE_ID = "abc-123-456";

    private static final String EXPECTED_DELETE_DATASTORE_JSON = "{\"resource_id\":\"" + RESOURCE_ID + "\"}";

    private static final String EXPECTED_FIELDS_JSON =
            "[{\"id\":\"id\",\"type\":\"Integer\"},"
                    + "{\"id\":\"name\",\"type\":\"Varchar\"},"
                    + "{\"id\":\"surname\",\"type\":\"Varchar\"},"
                    + "{\"id\":\"birthdate\",\"type\":\"Date\"},"
                    + "{\"id\":\"resident\",\"type\":\"Boolean\"}]";

    private static final String EXPECTED_RECORDS_JSON =
            "[{\"id\":1,\"name\":\"Jim\",\"surname\":\"White\",\"birthdate\":\"2015-02-13\",\"resident\":true},"
                    + "{\"id\":2,\"name\":\"John\",\"surname\":\"Black\",\"birthdate\":\"2015-02-13\",\"resident\":false}]";

    private static final String EXPECTED_DATASTORE_PARAMS_JSON =
            "{\"resource_id\":\"" + RESOURCE_ID + "\","
                    + "\"fields\":[{\"id\":\"id\",\"type\":\"Integer\"},{\"id\":\"name\",\"type\":\"Varchar\"},{\"id\":\"surname\",\"type\":\"Varchar\"},{\"id\":\"birthdate\",\"type\":\"Date\"},{\"id\":\"resident\",\"type\":\"Boolean\"}],"
                    + "\"records\":[{\"id\":1,\"name\":\"Jim\",\"surname\":\"White\",\"birthdate\":\"2015-02-13\",\"resident\":true},{\"id\":2,\"name\":\"John\",\"surname\":\"Black\",\"birthdate\":\"2015-02-13\",\"resident\":false}],"
                    + "\"primary_key\":[\"id\"],"
                    + "\"indexes\":[\"id\",\"birthdate\"]}";

    private List<ColumnDefinition> columns;

    @Before
    public void before() {
        this.columns = new ArrayList<>();
        this.columns.add(COLUMN_1);
        this.columns.add(COLUMN_2);
        this.columns.add(COLUMN_3);
        this.columns.add(COLUMN_4);
        this.columns.add(COLUMN_5);
    }

    @Test
    public void buildFieldsDefinitionJsonTest() {
        JsonArray fields = RelationalToCkanHelper.buildFieldsDefinitionJson(this.columns);
        Assert.assertEquals(EXPECTED_FIELDS_JSON, fields.toString());
    }

    @Test
    public void buildRecordsJsonTest() throws SQLException, ParseException {
        ResultSet mockedResultSet = mockResultSet();
        JsonArray records = RelationalToCkanHelper.buildRecordsJson(mockedResultSet, this.columns);
        Assert.assertEquals(EXPECTED_RECORDS_JSON, records.toString());
    }

    @Test
    public void buildDataStoreParametersTest() throws SQLException, ParseException {
        ResultSet mockedResultSet = mockResultSet();
        JsonArray fields = RelationalToCkanHelper.buildFieldsDefinitionJson(this.columns);
        JsonArray records = RelationalToCkanHelper.buildRecordsJson(mockedResultSet, this.columns);

        JsonObject datastore = RelationalToCkanHelper.buildCreateDataStoreParameters(RESOURCE_ID, INDEXES, PRIMARY_KEYS, fields, records);
        Assert.assertEquals(EXPECTED_DATASTORE_PARAMS_JSON, datastore.toString());

    }

    @Test
    public void buildDeleteDataStoreParamtersTest() {
        JsonObject deleteDatastore = RelationalToCkanHelper.buildDeleteDataStoreParamters(RESOURCE_ID);
        Assert.assertEquals(EXPECTED_DELETE_DATASTORE_JSON, deleteDatastore.toString());
    }

    private ResultSet mockResultSet() throws SQLException, ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date utilDate = formatter.parse(DATE_STRING);
        ResultSet rs = Mockito.mock(ResultSet.class);
        Mockito.when(rs.next()).thenReturn(true).thenReturn(true).thenReturn(false);

        Mockito.when(rs.getInt(COLUMN_1.getColumnName())).thenReturn(1).thenReturn(2);
        Mockito.when(rs.getObject(COLUMN_1.getColumnName())).thenReturn(1).thenReturn(2);
        
        Mockito.when(rs.getString(COLUMN_2.getColumnName())).thenReturn("Jim").thenReturn("John");
        Mockito.when(rs.getObject(COLUMN_2.getColumnName())).thenReturn("Jim").thenReturn("John");
        
        Mockito.when(rs.getString(COLUMN_3.getColumnName())).thenReturn("White").thenReturn("Black");
        Mockito.when(rs.getObject(COLUMN_3.getColumnName())).thenReturn("White").thenReturn("Black");
        long utilDateTime = utilDate.getTime();
        Mockito.when(rs.getDate(COLUMN_4.getColumnName())).thenReturn(new Date(utilDateTime)).thenReturn(new Date(utilDateTime));
        Mockito.when(rs.getObject(COLUMN_4.getColumnName())).thenReturn(new Date(utilDateTime)).thenReturn(new Date(utilDateTime));
        
        Mockito.when(rs.getBoolean(COLUMN_5.getColumnName())).thenReturn(true).thenReturn(false);
        Mockito.when(rs.getObject(COLUMN_5.getColumnName())).thenReturn(true).thenReturn(false);
        

        return rs;
    }

}

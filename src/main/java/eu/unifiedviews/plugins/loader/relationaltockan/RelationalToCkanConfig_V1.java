package eu.unifiedviews.plugins.loader.relationaltockan;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RelationalToCkanConfig_V1 {

    private String catalogApiLocation = "http://localhost/internalcatalog/uv";

    private Long pipelineId = 0L;

    private String userId = "mvi";

    private String token = "secret_token";

    private boolean bOverwriteTables = false;

    public String getCatalogApiLocation() {
        return this.catalogApiLocation;
    }

    public void setCatalogApiLocation(String catalogApiLocation) {
        this.catalogApiLocation = catalogApiLocation;
    }

    public Long getPipelineId() {
        return this.pipelineId;
    }

    public void setPipelineId(Long pipelineId) {
        this.pipelineId = pipelineId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setOverWriteTables(boolean overwrite) {
        this.bOverwriteTables = overwrite;
    }

    public boolean getOverWriteTables() {
        return this.bOverwriteTables;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}

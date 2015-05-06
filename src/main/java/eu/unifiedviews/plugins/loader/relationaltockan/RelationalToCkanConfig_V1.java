package eu.unifiedviews.plugins.loader.relationaltockan;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class RelationalToCkanConfig_V1 {

    private boolean bOverwriteTables = false;
    
    private String resourceName;

    public void setOverWriteTables(boolean overwrite) {
        this.bOverwriteTables = overwrite;
    }

    public boolean isOverWriteTables() {
        return this.bOverwriteTables;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE).toString();
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    
}

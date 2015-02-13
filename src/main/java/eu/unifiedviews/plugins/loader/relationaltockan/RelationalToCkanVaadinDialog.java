package eu.unifiedviews.plugins.loader.relationaltockan;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.config.BaseConfigDialog;
import eu.unifiedviews.helpers.dpu.config.InitializableConfigDialog;

public class RelationalToCkanVaadinDialog extends BaseConfigDialog<RelationalToCkanConfig_V1> implements InitializableConfigDialog {

    private static final long serialVersionUID = 4097151848036185236L;

    public RelationalToCkanVaadinDialog() {
        super(RelationalToCkanConfig_V1.class);
    }

    @Override
    public void initialize() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void setConfiguration(RelationalToCkanConfig_V1 conf) throws DPUConfigException {
        // TODO Auto-generated method stub

    }

    @Override
    protected RelationalToCkanConfig_V1 getConfiguration() throws DPUConfigException {
        // TODO Auto-generated method stub
        return null;
    }

}

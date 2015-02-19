package eu.unifiedviews.plugins.loader.relationaltockan;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.config.BaseConfigDialog;
import eu.unifiedviews.helpers.dpu.config.InitializableConfigDialog;
import eu.unifiedviews.helpers.dpu.localization.Messages;

public class RelationalToCkanVaadinDialog extends BaseConfigDialog<RelationalToCkanConfig_V1> implements InitializableConfigDialog {

    private static final long serialVersionUID = 4097151848036185236L;

    private Messages messages;

    private VerticalLayout mainLayout;

    private CheckBox chckOverWriteTables;

    public RelationalToCkanVaadinDialog() {
        super(RelationalToCkanConfig_V1.class);
    }

    @Override
    public void initialize() {
        this.messages = new Messages(getContext().getLocale(), this.getClass().getClassLoader());

        setWidth("100%");
        setHeight("100%");

        this.mainLayout = new VerticalLayout();
        this.mainLayout.setImmediate(false);
        this.mainLayout.setWidth("100%");
        this.mainLayout.setHeight("-1px");
        this.mainLayout.setSpacing(true);
        this.mainLayout.setMargin(false);

        this.chckOverWriteTables = new CheckBox();
        this.chckOverWriteTables.setCaption(this.messages.getString("dialog.ckan.overwrite"));
        this.mainLayout.addComponent(this.chckOverWriteTables);

        Panel panel = new Panel();
        panel.setSizeFull();
        panel.setContent(this.mainLayout);
        setCompositionRoot(panel);
    }

    @Override
    protected void setConfiguration(RelationalToCkanConfig_V1 config) throws DPUConfigException {
        this.chckOverWriteTables.setValue(config.isOverWriteTables());
    }

    @Override
    protected RelationalToCkanConfig_V1 getConfiguration() throws DPUConfigException {
        RelationalToCkanConfig_V1 config = new RelationalToCkanConfig_V1();
        config.setOverWriteTables(this.chckOverWriteTables.getValue());

        return config;
    }

}

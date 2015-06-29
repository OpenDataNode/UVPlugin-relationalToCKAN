package org.opendatanode.plugins.loader.relationaltockan;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unifiedviews.dpu.config.DPUConfigException;
import eu.unifiedviews.helpers.dpu.vaadin.dialog.AbstractDialog;
import eu.unifiedviews.plugins.loader.relationaltockan.RelationalToCkanConfig_V1;

public class RelationalToCkanVaadinDialog extends AbstractDialog<RelationalToCkanConfig_V1> {

    private static final long serialVersionUID = 4097151848036185236L;

    private VerticalLayout mainLayout;

    private CheckBox chckOverWriteTables;

    private TextField txtResourceName;

    public RelationalToCkanVaadinDialog() {
        super(RelationalToCkan.class);
    }

    @Override
    protected void buildDialogLayout() {
        setWidth("100%");
        setHeight("100%");

        this.mainLayout = new VerticalLayout();
        this.mainLayout.setWidth("100%");
        this.mainLayout.setHeight("-1px");
        this.mainLayout.setSpacing(true);
        this.mainLayout.setMargin(true);

        this.txtResourceName = new TextField();
        this.txtResourceName.setNullRepresentation("");
        this.txtResourceName.setRequired(true);
        this.txtResourceName.setCaption(this.ctx.tr("dialog.ckan.resource.name"));
        this.txtResourceName.setWidth("100%");
        this.txtResourceName.setDescription(this.ctx.tr("dialog.resource.name.help"));
        this.mainLayout.addComponent(this.txtResourceName);

        this.chckOverWriteTables = new CheckBox();
        this.chckOverWriteTables.setCaption(this.ctx.tr("dialog.ckan.overwrite"));
        this.mainLayout.addComponent(this.chckOverWriteTables);

        Panel panel = new Panel();
        panel.setSizeFull();
        panel.setContent(this.mainLayout);
        setCompositionRoot(panel);
    }

    @Override
    protected void setConfiguration(RelationalToCkanConfig_V1 config) throws DPUConfigException {
        this.chckOverWriteTables.setValue(config.isOverWriteTables());
        this.txtResourceName.setValue(config.getResourceName());
    }

    @Override
    protected RelationalToCkanConfig_V1 getConfiguration() throws DPUConfigException {
        boolean isValid = this.txtResourceName.isValid();
        if (!isValid) {
            throw new DPUConfigException(ctx.tr("dialog.errors.params"));
        }
        RelationalToCkanConfig_V1 config = new RelationalToCkanConfig_V1();
        config.setOverWriteTables(this.chckOverWriteTables.getValue());
        config.setResourceName(this.txtResourceName.getValue());

        return config;
    }
}

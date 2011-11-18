package org.onehippo.forge.externalresource.reports.panel;

import org.onehippo.cms7.reports.ReportsPerspective;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.onehippo.cms7.reports.plugins.PortalPanelPlugin;

/**
 * @version $Id$
 */
public class SynchronizationPlugin extends PortalPanelPlugin {

    public static String SYNCHRONIZATION_SERVICE_ID = "service.reports.synchronization";

/*
    public static class SynchronizationItemsPanel extends PanelPluginBreadCrumbPanel {

        public SynchronizationItemsPanel(final String id, IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
            super(id, breadCrumbModel);
            add(new PortalPanel("reports-panel", context, SYNCHRONIZATION_SERVICE_ID));
        }

        public IModel<String> getTitle(final Component component) {
            return new StringResourceModel("synchronization-panel-title", SynchronizationItemsPanel.this, null);
        }

    }
*/

    public SynchronizationPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    @Override
    public String getPortalPanelServiceId() {
        return SYNCHRONIZATION_SERVICE_ID;
    }

    public ResourceReference getImage() {
        return new ResourceReference(SynchronizationPlugin.class, "synchronization-48.png");
    }

    public IModel<String> getTitle() {
        return new ClassResourceModel("synchronization-panel-title", SynchronizationPlugin.class);
    }

    public IModel<String> getHelp() {
        return new ClassResourceModel("synchronization-panel-help", SynchronizationPlugin.class);
    }

    public String getPanelServiceId() {
        return ReportsPerspective.REPORTING_SERVICE;
    }

/*
    @Override
    public PanelPluginBreadCrumbPanel create(final String componentId, final IBreadCrumbModel breadCrumbModel) {
        return new SynchronizationItemsPanel(componentId, getPluginContext(), breadCrumbModel);
    }
*/

}

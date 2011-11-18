package org.onehippo.forge.externalresource.reports.panel;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.onehippo.cms7.reports.ReportsPerspective;
import org.onehippo.cms7.reports.plugins.PortalPanelPlugin;

/**
 * @version $Id$
 */
public class StatisticsReportPlugin extends PortalPanelPlugin {

    public static String STATISTICS_SERVICE_ID = "service.reports.statistics";

    public StatisticsReportPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    public ResourceReference getImage() {
        return new ResourceReference(StatisticsReportPlugin.class, "statistics-report-48.png");
    }

    public IModel<String> getTitle() {
        return new ClassResourceModel("statistics-report-panel-title", StatisticsReportPlugin.class);
    }

    public IModel<String> getHelp() {
        return new ClassResourceModel("statistics-report-panel-help", StatisticsReportPlugin.class);
    }

    @Override
    public String getPanelServiceId() {
        return ReportsPerspective.REPORTING_SERVICE;
    }

    @Override
    public String getPortalPanelServiceId() {
        return STATISTICS_SERVICE_ID;
    }

}

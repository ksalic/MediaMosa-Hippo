package org.onehippo.forge.externalresource.reports.plugins.statistics.chart;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.reports.AbstractExtRenderPlugin;
import org.wicketstuff.js.ext.ExtComponent;

/**
 * @version $Id$
 */
public class StatisticsChartPlugin extends AbstractExtRenderPlugin {

    private StatisticsChartPanel panel;

    public StatisticsChartPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        panel = new StatisticsChartPanel(context, config);
        add(panel);
    }

    @Override
    public ExtComponent getExtComponent() {
        return panel;
    }

}

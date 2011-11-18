package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.reports.AbstractExtRenderPlugin;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.reports.plugins.statistics.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.ExtPanel;

/**
 * @version $Id$
 */
public class StatisticsListPlugin extends AbstractExtRenderPlugin {

    private Logger log = LoggerFactory.getLogger(StatisticsListPlugin.class);
    protected static final String STATISTICS_PROVIDER_CLASS = "statistics.provider.class";
    protected static final String STATISTICS_SERVICE_PARAMETER_NAMES = "service.parameter.names";
    protected static final String STATISTICS_SERVICE_PARAMETER_VALUES = "service.parameter.values";
    protected static final String EXTERNAL_RESOURCE_SERVICE_ID = "external.processor.service";

    private ExtPanel panel;

    public StatisticsListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String statisticsProviderClass = config.getString(STATISTICS_PROVIDER_CLASS);
        if (statisticsProviderClass == null) {
            log.error("Report configuration '{}' is missing the required string property '{}' ",
                    config.getName(), STATISTICS_PROVIDER_CLASS);
            return;
        }


        StatisticsProvider provider = null;
        try {
            provider = ((Constructor<StatisticsProvider>) Class.forName(statisticsProviderClass)
                    .getConstructor(new Class[]{Map.class}))
                    .newInstance(new Object[]{getStatisticsServiceParameters(config)});
            provider.setResourceService(context.getService(config.getString("external.processor.id",
                EXTERNAL_RESOURCE_SERVICE_ID), ExternalResourceService.class));
        } catch (Exception e) {
            log.error("Cannot instantiate '{}'", statisticsProviderClass, e);
            return;
        }
        panel = new StatisticsListPanel(context, config, provider);
        add(panel);
    }

    public ExtComponent getExtComponent() {
        return panel;
    }

    //TODO Move to utils
    protected Map<String, String> getStatisticsServiceParameters(IPluginConfig config) {
        if (!config.containsKey(STATISTICS_SERVICE_PARAMETER_NAMES) || !config.containsKey(STATISTICS_SERVICE_PARAMETER_VALUES)) {
            return new HashMap<String, String>();
        }

        String[] parameterNames = config.getStringArray(STATISTICS_SERVICE_PARAMETER_NAMES);
        String[] parameterValues = config.getStringArray(STATISTICS_SERVICE_PARAMETER_VALUES);

        if (parameterNames == null || parameterValues == null || parameterNames.length != parameterValues.length) {
            log.warn("Service parameter names/values are misconfigured");
            return new HashMap<String, String>();
        }

        Map<String, String> statisticsServiceParameters = new HashMap<String, String>();
        for (int i = 0; i < parameterNames.length; i++) {
            statisticsServiceParameters.put(parameterNames[i], parameterValues[i]);
        }

        return statisticsServiceParameters;
    }

}
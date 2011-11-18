package org.onehippo.forge.externalresource.reports.plugins.statistics.chart;

import org.apache.wicket.Component;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.reports.plugins.statistics.StatisticsProvider;
import org.onehippo.forge.externalresource.reports.temp.AbstractChartStore;
import org.onehippo.forge.externalresource.reports.temp.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class StatisticsChartStore extends AbstractChartStore {

    private static final Logger log = LoggerFactory.getLogger(StatisticsChartStore.class);
    protected static final String STATISTICS_PROVIDER_CLASS = "statistics.provider.class";
    protected static final String STATISTICS_SERVICE_PARAMETER_NAMES = "service.parameter.names";
    protected static final String STATISTICS_SERVICE_PARAMETER_VALUES = "service.parameter.values";
    protected static final String EXTERNAL_RESOURCE_SERVICE_ID = "external.processor.service";

    protected StatisticsProvider provider;

    public StatisticsChartStore(Component component, IPluginContext context, IPluginConfig config) {
        super(component);

        String statisticsProviderClass = config.getString(STATISTICS_PROVIDER_CLASS);
        if (statisticsProviderClass == null) {
            log.error("Report configuration '{}' is missing the required string property '{}' ",
                    config.getName(), STATISTICS_PROVIDER_CLASS);
            return;
        }

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
    }

    @Override
    protected JSONArray getData() throws JSONException {
        List<Cluster> clusters = new ArrayList<Cluster>();
        Map<String, Long> chartData = provider.getChartData();
        for (String key : chartData.keySet()) {
            clusters.add(new Cluster(key, chartData.get(key)));
        }
        return getSectorData(clusters);
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

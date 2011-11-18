package org.onehippo.forge.externalresource.reports.plugins.statistics.chart;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.json.JSONException;
import org.onehippo.cms7.reports.plugins.ReportPanel;
import org.onehippo.cms7.reports.plugins.ReportUtil;
import org.onehippo.forge.externalresource.reports.temp.AbstractChartStore;
import org.onehippo.forge.externalresource.reports.temp.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtBundle;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.lang.reflect.Constructor;

/**
 * @version $Id$
 */
@ExtClass("Hippo.Reports.StatisticsChartPanel")
public class StatisticsChartPanel extends ReportPanel {

    private final static Logger log = LoggerFactory.getLogger(StatisticsChartPanel.class);
    private static final String NO_DATA_TRANSLATOR_KEY = "no-data";
    private static final String TITLE_TRANSLATOR_KEY = "title";
    private static final String X_AXIS_TITLE_TRANSLATOR_KEY = "x-axis-title";
    private static final String Y_AXIS_TITLE_TRANSLATOR_KEY = "y-axis-title";
    private static final String CONFIG_WIDTH = "width";
    private static final String CONFIG_HEIGHT = "height";
    private static final String CONFIG_CHART_TYPE = "chart.type";
    private static final String CONFIG_LEGEND_POSITION = "legend.position";

    @SuppressWarnings("unused")
    @ExtProperty
    private final String chartsFlashPath;

    private ExtJsonStore<Cluster> store;

    public StatisticsChartPanel(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(JavascriptPackageResource.getHeaderContribution(AbstractChartStore.class, "Hippo.Reports.Chart-min.js"));
        add(JavascriptPackageResource.getHeaderContribution(StatisticsChartPanel.class, "Hippo.Reports.StatisticsChartPanel.js"));
        String storeClassName = config.getString("store.class");
        try {
            Class<ExtJsonStore<Cluster>> storeClass = (Class<ExtJsonStore<Cluster>>) Class.forName(storeClassName);
            Constructor<ExtJsonStore<Cluster>> storeConstructor = storeClass.getConstructor(Component.class, IPluginContext.class, IPluginConfig.class);
            store = storeConstructor.newInstance(this, context, config);
            add(store);
        } catch (Exception e) {
            log.error("Unable to create store class " + storeClassName, e);
        }
        RequestCycle rc = RequestCycle.get();
        chartsFlashPath = rc.urlFor(new ResourceReference(ExtBundle.class, "resources/charts.swf")).toString();
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(org.json.JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        final String title = ReportUtil.getTranslation(this, TITLE_TRANSLATOR_KEY, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(title)) {
            properties.put("title", title);
        }

        if (config.getString(CONFIG_WIDTH) != null) {
            properties.put("width", config.getLong(CONFIG_WIDTH));
        }
        if (config.getString(CONFIG_HEIGHT) != null) {
            properties.put("height", config.getLong(CONFIG_HEIGHT));
        }
        properties.put("xAxisTitle", ReportUtil.getTranslation(this, X_AXIS_TITLE_TRANSLATOR_KEY, StringUtils.EMPTY));
        properties.put("yAxisTitle", ReportUtil.getTranslation(this, Y_AXIS_TITLE_TRANSLATOR_KEY, StringUtils.EMPTY));
        properties.put("noDataText", ReportUtil.getTranslation(this, NO_DATA_TRANSLATOR_KEY, StringUtils.EMPTY));
        properties.put("chartType", config.getString(CONFIG_CHART_TYPE, "pie"));
        properties.put("legendPosition", config.getString(CONFIG_LEGEND_POSITION, "right"));
        properties.put("store", new JSONIdentifier(store.getJsObjectId()));
    }

}

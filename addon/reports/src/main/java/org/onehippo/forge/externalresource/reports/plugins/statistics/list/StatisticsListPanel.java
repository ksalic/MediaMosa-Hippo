package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.reports.plugins.ReportPanel;
import org.onehippo.cms7.reports.plugins.ReportUtil;
import org.onehippo.forge.externalresource.reports.plugins.statistics.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.JSONIdentifier;

/**
 * @version $Id$
 */
@ExtClass("Hippo.Reports.StatisticsListPanel")
public class StatisticsListPanel extends ReportPanel {

    private static final String CONFIG_COLUMNS = "columns";
    private static final String CONFIG_AUTO_EXPAND_COLUMN = "auto.expand.column";
    private static final String TITLE_TRANSLATOR_KEY = "title";
    private static final String NO_DATA_TRANSLATOR_KEY = "no-data";

    private final Logger log = LoggerFactory.getLogger(StatisticsListPanel.class);
    private final IPluginContext context;
    private final int pageSize;
    private final StatisticsListColumns columns;
    private final ExtJsonStore<Object> store;

    public StatisticsListPanel(final IPluginContext context, final IPluginConfig config, final StatisticsProvider statisticsProvider) {
        super(context, config);

        this.context = context;
        pageSize = config.getInt("page.size", 10);
        columns = new StatisticsListColumns(statisticsProvider.getColumns(config.getStringArray(CONFIG_COLUMNS)));

        //this will only need the dataResolver of the statisticsProvider... awaiting MediaMosaService..for now we pass the provider..
        store = new StatisticsListStore(columns, statisticsProvider, pageSize);
        add(store);

        add(CSSPackageResource.getHeaderContribution(this.getClass(), "Hippo.Reports.StatisticsList.css"));
        add(JavascriptPackageResource.getHeaderContribution(this.getClass(), "Hippo.Reports.StatisticsList.js"));

        addEventListener("rowSelected", new ExtEventListener() {
            @Override
            public void onEvent(final AjaxRequestTarget ajaxRequestTarget) {
                rowSelected(RequestCycle.get().getRequest().getParameter("rowData"));
            }
        });
    }


    //TODO Change the following, it is Mediamosa specific while this is a generic reporting panel
    protected void rowSelected(String rowData) {
        if (rowData == null || rowData.length() == 0) {
            log.debug("No document data to browse to");
            return;
        }

        Matcher docIdentifierMatcher = Pattern.compile("assetId=[^\\|]*").matcher(rowData);
        if (docIdentifierMatcher.find()) {
            String assetId = docIdentifierMatcher.group().split("=")[1];
            try {
                Session session = ((UserSession) getSession()).getJcrSession();
                QueryManager manager = session.getWorkspace().getQueryManager();
                String queryString = String.format("content/videos//element(*,hippomediamosa:resource)[@hippomediamosa:assetid='%s']", assetId);
                Query query = manager.createQuery(queryString, Query.XPATH);
                query.setLimit(1);
                QueryResult result = query.execute();
                if (result.getNodes().hasNext()) {
                    JcrNodeModel nodeModel = new JcrNodeModel(result.getNodes().nextNode());
                    IBrowseService browseService = context.getService("service.browse", IBrowseService.class);
                    browseService.browse(nodeModel);
                }

            } catch (RepositoryException e) {
                log.error("Unable to get the node by asset id " + assetId, e);
            }
        }
    }


    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        properties.put("columns", getColumnsConfig());
        properties.put("store", new JSONIdentifier(store.getJsObjectId()));
        properties.put("pageSize", this.pageSize);
        properties.put("paging", config.getAsBoolean("paging", true));
        properties.put("noDataText", ReportUtil.getTranslation(this, NO_DATA_TRANSLATOR_KEY, StringUtils.EMPTY));

        if (config.containsKey(CONFIG_AUTO_EXPAND_COLUMN)) {
            String autoExpandColumn = config.getString(CONFIG_AUTO_EXPAND_COLUMN);

            if (!columns.containsColumn(autoExpandColumn)) {
                // prevent an auto-expand column that is not an actual column name, otherwise ExtJs stops rendering
                log.warn("Ignoring unknown auto-expand column '{}'", autoExpandColumn);
            } else {
                properties.put("autoExpandColumn", autoExpandColumn);
            }
        }

        final String title = ReportUtil.getTranslation(this, TITLE_TRANSLATOR_KEY, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(title)) {
            properties.put("title", title);
        }
    }

    private JSONArray getColumnsConfig() throws JSONException {
        JSONArray result = new JSONArray();

        for (IStatisticsListColumn column : columns.getAllColumns()) {
            JSONObject config = column.getExtColumnConfig();
            if (config != null) {
                result.put(config);
            }
        }

        return result;
    }

}

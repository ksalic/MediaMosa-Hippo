package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.json.*;
import org.onehippo.forge.externalresource.reports.plugins.statistics.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

/**
 * @version $Id$
 */
public class StatisticsListStore extends ExtJsonStore<Object> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StatisticsListStore.class);

    @SuppressWarnings("unused")
    @ExtProperty
    private boolean autoSave = false;

    private StatisticsListColumns columns;
    private StatisticsProvider statisticsProvider;

    private int pageSize;

    private List localCachedResultSet;


    StatisticsListStore(StatisticsListColumns columns, StatisticsProvider statisticsProvider, int pageSize) {
        super(columns.getAllExtFields());

        this.columns = columns;
        this.statisticsProvider = statisticsProvider;
        this.pageSize = pageSize;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        final JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    @Override
    protected long getTotal() {
        if (localCachedResultSet == null) {
            refreshResultSet();
        }

        return localCachedResultSet.size();
    }

    private void refreshResultSet() {
        localCachedResultSet = statisticsProvider.getListData();
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray result = new JSONArray();

        final RequestCycle requestCycle = RequestCycle.get();
        ServletWebRequest swr = ((ServletWebRequest) requestCycle.getRequest());

        int startIndex = parseIntParameter(swr, "start", 0);
        int amount = parseIntParameter(swr, "limit", this.pageSize);
        int itemsCount = 0;

        if (localCachedResultSet == null) {
            refreshResultSet();
        }

        final Iterator resultsIterator = localCachedResultSet.listIterator(startIndex);

        while (resultsIterator.hasNext() && itemsCount < amount) {
            final Object statsItem = resultsIterator.next();

            final JSONObject document = new JSONObject();
            for (IStatisticsListColumn column : columns.getAllColumns()) {
                final String value = getValue(statsItem, column);
                document.put(column.getExtField().getName(), value);
            }
            result.put(document);
            itemsCount++;
        }
        return result;
    }

    private String getValue(final Object statsItem, final IStatisticsListColumn column) {
        final String value = column.getValue(statsItem);
        if (value != null) {
            return value;
        } else {
            log.info("Skipped property {}, cause: {}", new Object[]{column.getExtField().getName(), "null"});
        }
        return StringUtils.EMPTY;
    }

    private int parseIntParameter(ServletWebRequest request, String name, int defaultValue) {
        String param = request.getParameter(name);
        if (param != null) {
            try {
                return Integer.parseInt(param);
            } catch (NumberFormatException e) {
                log.warn("Value of parameter '" + name + "' is not an integer: '" + param
                        + "', using default value '" + defaultValue + "'");
            }
        }
        return defaultValue;
    }

}

package org.onehippo.forge.externalresource.reports.plugins.synchronization.store;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.reports.plugins.synchronization.column.SynchronizationListColumns;
import org.onehippo.forge.externalresource.reports.temp.IDocumentListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * @version $Id$
 */
public class SynchronizationStore  extends ExtJsonStore<Object> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SynchronizationStore.class);

    @SuppressWarnings("unused")
    @ExtProperty
    private boolean autoSave = false;

    private SynchronizationListColumns columns;
    private String query;
    private int pageSize;
    private HippoNodeIterator hippoNodeIterator;
    private final ExternalResourceService externalService;

    public SynchronizationStore(String query, SynchronizationListColumns columns, int pageSize, ExternalResourceService externalService) {
        super(columns.getAllExtFields());
        this.externalService = externalService;
        this.columns = columns;
        this.query = query;
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
        try {
            executeQuery();
            return this.hippoNodeIterator.getTotalSize();

        } catch (RepositoryException e) {
            log.warn("Could not retrieve total document count, paging is disabled", e);
        }

        return -1;
    }

    private void executeQuery() throws RepositoryException{
        QueryManager queryManager = ((UserSession) org.apache.wicket.Session.get()).getJcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(this.query, Query.XPATH);
        QueryResult queryResult = query.execute();
        this.hippoNodeIterator = (HippoNodeIterator)queryResult.getNodes();
    }

    @Override
    protected JSONArray getData() throws JSONException {

        JSONArray result = new JSONArray();

        final RequestCycle requestCycle = RequestCycle.get();
        ServletWebRequest swr = ((ServletWebRequest) requestCycle.getRequest());
        int startIndex = parseIntParameter(swr, "start", 0);
        int amount = parseIntParameter(swr, "limit", this.pageSize);
        int documentCount = 0;

        try {
            if (this.hippoNodeIterator == null){
                executeQuery();
            }

            this.hippoNodeIterator.skip(startIndex);

            while (hippoNodeIterator.hasNext() && documentCount < amount) {
                final Node node = hippoNodeIterator.nextNode();
                Node canonical = ((HippoNode) node).getCanonicalNode();
                if (canonical == null) {
                    log.warn("Skipped {}, no canonical node available", node.getPath());
                    continue;
                }

                //Synchronizable sync = externalService.getSynchronizableProcessor(canonical.getPrimaryNodeType().getName());
                //sync.check(canonical);

                final JSONObject document = new JSONObject();
                for (IDocumentListColumn column: columns.getAllColumns()) {
                    final String fieldName = column.getExtField().getName();
                    final String value = getValue(canonical, column, fieldName);
                    document.put(fieldName, value);
                }
                result.put(document);
                documentCount++;
            }

        } catch (RepositoryException e) {
            log.error("Error querying data for " + this.query, e);
        }

        return result;
    }




    private String getValue(final Node canonical, final IDocumentListColumn column, String fieldName) throws RepositoryException {
        try {
            final String value = column.getValue(canonical);
            if (value != null) {
                return value;
            }
        } catch (PathNotFoundException e) {
            log.info("Skipped {} of {}, path not found: {}", new Object[]{fieldName, canonical.getPath(), e.getMessage()});

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


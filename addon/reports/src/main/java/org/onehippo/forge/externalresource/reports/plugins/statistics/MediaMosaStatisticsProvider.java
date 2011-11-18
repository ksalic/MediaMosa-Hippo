package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.util.lang.PropertyResolver;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.reports.plugins.statistics.list.IStatisticsListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;

import nl.uva.mediamosa.MediaMosaService;

/**
 * @version $Id$
 */
public class MediaMosaStatisticsProvider<T> extends StatisticsProvider<T> {

    protected MediaMosaService service;
    protected static final String HIPPO_MEDIAMOSA_RESOURCE_MANAGER_ID = "hippomediamosa:resource";

    protected Logger log = LoggerFactory.getLogger(this.getClass());
    protected Map<String, String> statisticsServiceParameters = new HashMap<String, String>();

    //--------- The following are meant to be overridden by extending classes ---------//
    //but enums cannot be ovverriden..
    protected enum ColumnName {
    }

    protected Map<String, IStatisticsListColumn> itemColumnMap = new LinkedHashMap<String, IStatisticsListColumn>();
    //--------------------------------------------------------------------------------//

    public MediaMosaStatisticsProvider(Map<String, String> statisticsServiceParameters) {
        this.statisticsServiceParameters = statisticsServiceParameters;
    }

    @Override
    public void setResourceService(final ExternalResourceService service) {
        if(service == null){
            throw new RuntimeException("Error in statistics provider configuration. Service is null");
        }

        ResourceManager manager = service.getResourceProcessor(HIPPO_MEDIAMOSA_RESOURCE_MANAGER_ID);
        if(manager == null && ! (manager instanceof HippoMediaMosaResourceManager)){
            throw new RuntimeException("Error in statistics provider configuration. ResourceManager is not a HippoMediaMosaResourceManager");
        }

        MediaMosaService mediaMosaService = ((HippoMediaMosaResourceManager)manager).getMediaMosaService();
        if( mediaMosaService == null){
            log.error("MediaMosaService is null");
            throw new RuntimeException("Error in statistics provider configuration. Service is not a MediaMosaService");
        }

        this.service = mediaMosaService;
    }

    @Override
    public List<IStatisticsListColumn> getColumns(final String[] selectedColumns) {
        if (selectedColumns == null || selectedColumns.length == 0) {
            log.warn("No column names specified, expected a comma-separated list with these possible values: {}", allColumnNames());
        }

        List<IStatisticsListColumn> columns = new ArrayList<IStatisticsListColumn>();
        for (String selectedColumn : selectedColumns) {
            try {
                columns.add(this.itemColumnMap.get(selectedColumn));

            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown document list column name: '{}', known names are: {}", selectedColumn, allColumnNames());
            }
        }

        //We always must add any ExtResourceIdentifierColumn(s) so that we support operations on rowSelected
        for(IStatisticsListColumn column : this.itemColumnMap.values()){
            if(column instanceof MediaMosaStatisticsProvider<?>.ExtResourceIdentifierColumn){
                columns.add(column);
            }
        }

        return columns;
    }


    @Override
    public String allColumnNames() {
        StringBuilder result = new StringBuilder();
        String concat = "";
        for (String columnName : itemColumnMap.keySet()) {
            result.append(concat).append('\'').append(columnName).append('\'');
            concat = ",";
        }
        return result.toString();
    }


    @Override
    public List<T> getListData() {
        return null;
    }

    @Override
    public Map<String, Long> getChartData() {
        return null;
    }

    protected String getResourceValue(String key) {
        return new ClassResourceModel(key, this.getClass()).getObject();
    }

    protected String getMMServiceParameter(String name, String defaultValue){
        return statisticsServiceParameters.containsKey(name) ?
                statisticsServiceParameters.get(name) : defaultValue;
    }

    // ==================================== Columns ====================================

    protected class MMPropertyColumn implements IStatisticsListColumn<Object> {

        protected String name;

        protected MMPropertyColumn(String name) {
            this.name = name;
        }

        public ExtField getExtField() {
            return new ExtField(name);
        }

        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", name);
            config.put("id", name);
            config.put("header", getResourceValue("column-" + name + "-header"));
            config.put("width", Integer.parseInt(getResourceValue("column-" + name + "-width")));
            return config;
        }

        public String getValue(final Object statsItem) {
            Object value = getObjectValue(statsItem);
            return value == null ? StringUtils.EMPTY : value.toString();
        }


        public String getName() {
            return name;
        }

        protected Object getObjectValue(final Object statsItem) {
            try {
                //ERP-19 Take advantage of the javabean spec all Mediamosa statistics beans abide to
                return PropertyResolver.getValue(name, statsItem);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    protected class StringPropertyColumn extends MMPropertyColumn {

        public StringPropertyColumn(String name) {
            super(name);
        }
    }

    protected class DatePropertyColumn extends MMPropertyColumn {

        public DatePropertyColumn(String name) {
            super(name);
        }

        @Override
        public String getValue(final Object statsItem) {

            Calendar calendar = (Calendar) getObjectValue(statsItem);
            if (calendar == null) {
                return StringUtils.EMPTY;
            }

            Locale locale = Session.get().getLocale();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("d-MMM-yyyy").withLocale(locale);

            try {
                final DateTime date = new DateTime(calendar);
                return formatter.print(date);
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse property '{}': " + e.getMessage() + ", using empty string instead", name, e.getMessage());
            }

            return StringUtils.EMPTY;
        }
    }


    protected class FilesizePropertyColumn extends MMPropertyColumn {

        public FilesizePropertyColumn(String name) {
            super(name);
        }

        @Override
        public String getValue(final Object statsItem) {

            try {
                int bytes = Integer.parseInt(getObjectValue(statsItem).toString());
                //TODO Turn to mbs?
                return String.valueOf(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return StringUtils.EMPTY;
        }
    }

    protected class ExtResourceIdentifierColumn extends MMPropertyColumn {

        public ExtResourceIdentifierColumn(String name){
            super(name);
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            // never include the identifier as a visible column
            return null;
        }
    }


/*

    private static class NameColumn implements IDocumentListColumn {

        private static final String DATA_INDEX = "name";
        private static final ExtField EXT_FIELD = new ExtField(DATA_INDEX);

        @Override
        public ExtField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", DATA_INDEX);
            config.put("id", DATA_INDEX);
            config.put("header", getResourceValue("column-name-header"));
            config.put("width", Integer.parseInt(getResourceValue("column-name-width")));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            if (node instanceof HippoNode) {
                HippoNode hippoNode = (HippoNode) node;
                return hippoNode.getLocalizedName();
            } else {
                return NodeNameCodec.decode(node.getName());
            }
        }
    }

    private static class PathColumn implements IDocumentListColumn {

        private static final String DATA_INDEX = "path";
        private static final ExtField EXT_FIELD = new ExtField(DATA_INDEX);

        @Override
        public ExtField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            // never include the path as a visible column
            return null;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            return node.getPath();
        }
    }

    private static class ShareColumn implements IDocumentListColumn {

        private static final ExtField EXT_FIELD = new ExtField("share");

        @Override
        public ExtField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("id", "share");
            config.put("header", getResourceValue("column-share-header"));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            // column value is set in Javascript
            return null;
        }
    }
*/
}

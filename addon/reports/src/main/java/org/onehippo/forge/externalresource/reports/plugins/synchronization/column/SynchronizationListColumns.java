package org.onehippo.forge.externalresource.reports.plugins.synchronization.column;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.forge.externalresource.reports.temp.IDocumentListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.*;


public class SynchronizationListColumns implements IClusterable {

    public static enum ColumnName {
        lastSynchronized,
        type,
        synchState,
        name,
        path,
        syncActions,
    }

    ;


    private static final Map<ColumnName, IDocumentListColumn> DOCUMENT_COLUMN_MAP = new EnumMap<ColumnName, IDocumentListColumn>(ColumnName.class);

    static {
        DOCUMENT_COLUMN_MAP.put(ColumnName.name, new NameColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.lastSynchronized, new DatePropertyColumn("lastSynchronized", "hippoexternal:lastModifiedSyncDate"));
        DOCUMENT_COLUMN_MAP.put(ColumnName.type, new TranslatedNodeTypeStringPropertyColumn("type", "jcr:primaryType"));
        DOCUMENT_COLUMN_MAP.put(ColumnName.synchState, new TranslatedStringPropertyColumn("synchState", "hippoexternal:state", "hippoexternal:synchronizable"));
        DOCUMENT_COLUMN_MAP.put(ColumnName.path, new PathColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.syncActions, new SyncActionsColumn());
    }

    private static final Logger log = LoggerFactory.getLogger(SynchronizationListColumns.class);

    private final List<ColumnName> columnNames;

    public SynchronizationListColumns(String[] names) {
        columnNames = new ArrayList<ColumnName>(names.length + 1);

        if (names.length == 0) {
            log.warn("No column names specified, expected a comma-separated list with these possible values: {}",
                    allColumnNames());
        }

        for (String name : names) {
            try {
                final ColumnName columnName = ColumnName.valueOf(name);
                if (!columnName.equals(ColumnName.path)) {
                    //final IDocumentListColumn column = DOCUMENT_COLUMN_MAP.get(columnName);
                    columnNames.add(columnName);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown document list column name: '{}', known names are: {}", name, allColumnNames());
            }
        }

        // always add the path column since it is used internally to identify the document
        columnNames.add(ColumnName.path);
    }

    public List<IDocumentListColumn> getAllColumns() {
        List<IDocumentListColumn> result = new ArrayList<IDocumentListColumn>(columnNames.size());

        for (ColumnName columnName : columnNames) {
            final IDocumentListColumn column = DOCUMENT_COLUMN_MAP.get(columnName);
            result.add(column);
        }

        return result;
    }

    public boolean containsColumn(String name) {
        if (name == null) {
            return false;
        }
        try {
            return columnNames.contains(ColumnName.valueOf(name));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<ExtField> getAllExtFields() {
        List<ExtField> result = new ArrayList<ExtField>(columnNames.size());

        for (IDocumentListColumn column : getAllColumns()) {
            result.add(column.getExtField());
        }

        return result;
    }

    public JSONArray getAllColumnConfigs() throws JSONException {
        JSONArray result = new JSONArray();

        for (IDocumentListColumn column : getAllColumns()) {
            result.put(column.getExtColumnConfig());
        }

        return result;
    }

    private static String allColumnNames() {
        StringBuilder result = new StringBuilder();
        String concat = "";
        for (ColumnName columnName : ColumnName.values()) {
            if (columnName != ColumnName.path) {
                result.append(concat);
                result.append('\'');
                result.append(columnName.name());
                result.append('\'');
                concat = ",";
            }
        }
        return result.toString();
    }

    private static String getResourceValue(String key) {
        return new ClassResourceModel(key, SynchronizationListColumns.class).getObject();
    }

    // ==================================== Columns ====================================

    private static class StringPropertyColumn implements IDocumentListColumn {

        protected final String name;
        protected final String property;

        public StringPropertyColumn(String name, String property) {
            this.name = name;
            this.property = property;
        }

        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", name);
            config.put("id", name);
            config.put("header", getResourceValue("column-" + name + "-header"));
            config.put("width", Integer.parseInt(getResourceValue("column-" + name + "-width")));
            return config;
        }

        public ExtField getExtField() {
            return new ExtField(name);
        }

        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty(property);

            if (prop == null) {
                return StringUtils.EMPTY;
            }

            return prop.getString();
        }

    }

    private static class TranslatedNodeTypeStringPropertyColumn extends StringPropertyColumn {


        public TranslatedNodeTypeStringPropertyColumn(String name, String property) {
            super(name, property);
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty(property);

            if (prop == null) {
                return StringUtils.EMPTY;
            }

            return new TypeTranslator(new JcrNodeTypeModel(prop.getString())).getTypeName().getObject();
        }

    }

    private static class TranslatedNodePropertyTypeStringColumn extends StringPropertyColumn {

        final TypeTranslator translator;

        public TranslatedNodePropertyTypeStringColumn(String name, String property, String nodeType) {
            super(name, property);
            translator = new TypeTranslator(new JcrNodeTypeModel(nodeType));
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty(property);
            if (prop == null) {
                return StringUtils.EMPTY;
            }
            return translator.getValueName(property, new Model(prop.getString())).getObject();
        }


    }

    private static class TranslatedStringPropertyColumn extends StringPropertyColumn {

        final TypeTranslator translator;

        public TranslatedStringPropertyColumn(String name, String property, String nodeType) {
            super(name, property);
            translator = new TypeTranslator(new JcrNodeTypeModel(nodeType));
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty(property);

            if (prop == null) {
                return StringUtils.EMPTY;
            }

            return translator.getValueName(property, new Model(prop.getString())).getObject();
            //return prop.getString();
        }


    }

    private static class DatePropertyColumn extends StringPropertyColumn {

        public DatePropertyColumn(String name, String property) {
            super(name, property);
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property dateProperty = node.getProperty(property);

            if (dateProperty == null) {
                return StringUtils.EMPTY;
            }

            Locale locale = Session.get().getLocale();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("d-MMM-yyyy hh:mm:ss").withLocale(locale);

            try {
                final DateTime date = new DateTime(dateProperty.getDate());
                return formatter.print(date);
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse property '{}': " + e.getMessage() + ", using empty string instead", property, e.getMessage());
            }

            return StringUtils.EMPTY;
        }
    }

    private static class NameColumn implements IDocumentListColumn {

        private static final String DATA_INDEX = "name";
        private static final ExtField EXT_FIELD = new ExtField(DATA_INDEX);

        public ExtField getExtField() {
            return EXT_FIELD;
        }

        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", DATA_INDEX);
            config.put("id", DATA_INDEX);
            config.put("header", getResourceValue("column-name-header"));
            config.put("width", Integer.parseInt(getResourceValue("column-name-width")));
            return config;
        }

        public String getValue(final Node node) throws RepositoryException {
            if (node instanceof HippoNode) {
                HippoNode hippoNode = (HippoNode) node;
                return NodeNameCodec.decode(hippoNode.getLocalizedName());
            } else {
                return NodeNameCodec.decode(node.getName());
            }
        }
    }

    private static class PathColumn implements IDocumentListColumn {

        private static final String DATA_INDEX = "path";
        private static final ExtField EXT_FIELD = new ExtField(DATA_INDEX);

        public ExtField getExtField() {
            return EXT_FIELD;
        }

        public JSONObject getExtColumnConfig() throws JSONException {
            // never include the path as a visible column
            return null;
        }

        public String getValue(final Node node) throws RepositoryException {
            return node.getPath();
        }
    }


    private static class SyncActionsColumn implements IDocumentListColumn {

        private static final ExtField EXT_FIELD = new ExtField("syncActions");

        public ExtField getExtField() {
            return EXT_FIELD;
        }

        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("id", "syncActions");
            config.put("header", getResourceValue("column-syncActions-header"));
            return config;
        }

        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty("hippoexternal:state");

            if (prop == null) {
                return StringUtils.EMPTY;
            }

            return prop.getString();
        }
    }


}

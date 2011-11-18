package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;

/**
 * @version $Id$
 */
public class StatisticsListColumns {

    private static final Logger log = LoggerFactory.getLogger(StatisticsListColumns.class);

    private final List<IStatisticsListColumn> columns;

    public StatisticsListColumns(List<IStatisticsListColumn> columns) {
        this.columns = columns;
    }

    public List<IStatisticsListColumn> getAllColumns() {
        return this.columns;
    }

    public boolean containsColumn(String name) {
        if (name == null) {
            return false;
        }

        for (IStatisticsListColumn column: this.columns) {
            if(column.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    public List<ExtField> getAllExtFields() {
        List<ExtField> result = new ArrayList<ExtField>(columns.size());

        for (IStatisticsListColumn column: this.columns) {
            result.add(column.getExtField());
        }

        return result;
    }

    public JSONArray getAllColumnConfigs() throws JSONException {
        JSONArray result = new JSONArray();

        for (IStatisticsListColumn column: this.columns) {
            result.put(column.getExtColumnConfig());
        }

        return result;
    }

}

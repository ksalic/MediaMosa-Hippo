package org.onehippo.forge.externalresource.reports.plugins.statistics.list;

import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.data.ExtField;


/**
 * @version $Id$
 */
public interface IStatisticsListColumn<T> {

    public ExtField getExtField();

    public JSONObject getExtColumnConfig() throws JSONException;

    public String getValue(T statsItem);

    public String getName();

}

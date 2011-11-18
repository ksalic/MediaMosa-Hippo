package org.onehippo.forge.externalresource.reports.temp;

import org.apache.wicket.Component;
import org.apache.wicket.util.lang.PropertyResolver;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractChartStore extends ExtJsonStore<Cluster> {

    protected final static String NO_PIE_CHART_DATA = "noPieChartData";
    
    protected Component component;

    public AbstractChartStore(Component component) {
        super(Arrays.asList(new ExtField("name"), new ExtField("total")));
        this.component = component;
    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    protected JSONArray getSectorData(List<Cluster> clusters) throws JSONException {
        JSONArray jsonData = new JSONArray();
        for (Cluster record : clusters) {
                JSONObject jsonLine = new JSONObject();
                for (ExtField field : getFields()) {
                    Object value = PropertyResolver.getValue(field.getName(), record);
                    jsonLine.put(field.getName(), value);
                }
                jsonData.put(jsonLine);
            }
        return jsonData;
    }

    public Component getComponent() {
        return component;
    }

}

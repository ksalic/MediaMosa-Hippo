package org.onehippo.forge.externalresource.reports.temp;

import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.data.ExtField;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public interface IDocumentListColumn {

    public ExtField getExtField();

    public JSONObject getExtColumnConfig() throws JSONException;

    public String getValue(Node node) throws RepositoryException;

}

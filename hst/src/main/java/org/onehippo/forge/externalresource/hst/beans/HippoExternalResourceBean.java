package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

import java.util.Date;

/**
 * @version
 */
@Node(jcrType = "hippoexternal:resource")
public class HippoExternalResourceBean extends HippoDocument {

    public String getName() {
        return getProperty("hippoexternal:name");
    }

    public String getMimeType() {
        return getProperty("hippoexternal:mimeType");
    }

    public Date getLastModified() {
        return getProperty("hippoexternal:lastModified");
    }

    public Long getSize() {
        return getProperty("hippoexternal:size");
    }

}

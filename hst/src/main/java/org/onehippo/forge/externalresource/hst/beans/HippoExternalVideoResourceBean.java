package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Node(jcrType = "hippoexternal:video")
public class HippoExternalVideoResourceBean extends HippoExternalResourceBean {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoExternalVideoResourceBean.class);

    public String getTitle() {
        return getProperty("hippoexternal:title");
    }

    public String getDescription() {
        return getProperty("hippoexternal:description");
    }

    public Long getWidth() {
        return getProperty("hippoexternal:width");
    }

    public Long getHeight() {
        return getProperty("hippoexternal:height");
    }

    public String getDuration() {
        return getProperty("hippoexternal:duration");
    }


}

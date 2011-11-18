package org.onehippo.forge.externalresource.api.utils;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * @version $Id$
 */
public class Utils {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(Utils.class);

    private static final String HIPPOEXTERNAL_EMBEDDED = "hippoexternal:embedded";

    public static void addEmbeddedNode(Node node, String embedded) {
        try {
            Node embeddedNode = null;
            if (node.hasNode(HIPPOEXTERNAL_EMBEDDED)) {
                embeddedNode = node.getNode(HIPPOEXTERNAL_EMBEDDED);
            } else {
                embeddedNode = node.addNode(HIPPOEXTERNAL_EMBEDDED, HIPPOEXTERNAL_EMBEDDED);
            }
            embeddedNode.setProperty(HIPPOEXTERNAL_EMBEDDED, embedded);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    public static boolean resolveThumbnailToVideoNode(String imageUrl, Node node) {
        try {
            if (node.isNodeType("hippoexternal:video")) {
                org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
                HttpMethod getMethod = new GetMethod(imageUrl);
                InputStream is = null;
                try {
                    client.executeMethod(getMethod);
                    is = getMethod.getResponseBodyAsStream();
                    String mimeType = getMethod.getResponseHeader("content-type").getValue();
                    if (mimeType.startsWith("image")) {
                        if (node.hasNode("hippoexternal:thumbnail")) {
                            Node thumbnail = node.getNode("hippoexternal:thumbnail");
                            thumbnail.setProperty("jcr:data", node.getSession().getValueFactory().createBinary(is));
                            thumbnail.setProperty("jcr:mimeType", mimeType);
                            thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());
                            node.getSession().save();
                            return true;
                        }
                    }
                } catch (IOException e) {
                    log.error("", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

}

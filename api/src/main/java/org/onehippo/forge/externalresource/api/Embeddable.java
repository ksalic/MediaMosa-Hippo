package org.onehippo.forge.externalresource.api;

import javax.jcr.Node;

/**
 * @version $Id$
 */
public interface Embeddable {

    public String getEmbedded(Node node);
}

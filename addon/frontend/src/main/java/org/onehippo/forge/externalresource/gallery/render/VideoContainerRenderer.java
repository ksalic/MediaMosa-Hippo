package org.onehippo.forge.externalresource.gallery.render;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.repository.api.HippoNodeType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class VideoContainerRenderer extends IconRenderer {

    private static final long serialVersionUID = -4385582873739274710L;

    @Override
    protected ResourceReference getResourceReference(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return new ResourceReference(VideoContainerRenderer.class,
                                    "res/video.png");
        }
        return super.getResourceReference(node);
    }

}

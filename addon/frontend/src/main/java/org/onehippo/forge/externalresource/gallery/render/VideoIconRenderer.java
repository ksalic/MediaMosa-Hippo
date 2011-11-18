package org.onehippo.forge.externalresource.gallery.render;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.repository.api.HippoNodeType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

public class VideoIconRenderer extends IconRenderer {

    private static final long serialVersionUID = -4385582873739274710L;

    private static final Map<String, String> VIDEOTYPE_TO_ICON = new HashMap<String, String>();

    static {
        VIDEOTYPE_TO_ICON.put("hippoyoutube:resource", "res/youtube.png");
        VIDEOTYPE_TO_ICON.put("hipporedfive:resource", "res/red5.png");
        VIDEOTYPE_TO_ICON.put("hippomediamosa:resource", "res/mediamosa.png");
        VIDEOTYPE_TO_ICON.put("default", "res/video-16.png");
    }

    @Override
    protected ResourceReference getResourceReference(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            Node item = node.getNode(node.getName());
            String primaryName = item.getPrimaryNodeType().getName();
            String iconPath;

            if (VIDEOTYPE_TO_ICON.containsKey(primaryName)) {
                iconPath = VIDEOTYPE_TO_ICON.get(primaryName);
            } else {
                iconPath = VIDEOTYPE_TO_ICON.get("default");
            }
            return new ResourceReference(VideoIconRenderer.class,
                    iconPath);
        }
        return super.getResourceReference(node);
    }

}

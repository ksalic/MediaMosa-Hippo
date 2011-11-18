package org.onehippo.forge.externalresource.hst.engine;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoMirrorBean;
import org.onehippo.forge.externalresource.api.Embeddable;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.api.utils.NodePluginConfig;
import org.onehippo.forge.externalresource.hst.beans.HippoExternalVideoResourceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @version $Id$
 */
public class ExternalResourceHstEngine {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourceHstEngine.class);

    private static ExternalResourceHstEngine ourInstance = new ExternalResourceHstEngine();

    public static ExternalResourceHstEngine getInstance() {
        return ourInstance;
    }

    private ExternalResourceHstEngine() {
    }

    private static String location = "/hippo:configuration/hippo:frontend/cms/cms-services/externalResourceService";

    public String getEmbeddableVideo(HippoBean inBean, String relPath, Class<? extends HippoExternalVideoResourceBean> beanMappingClass) {
        HippoBean bean = getLinkedBean(inBean, relPath, beanMappingClass);
        Embeddable embeddable = getEmbeddable(bean);
        return embeddable.getEmbedded(bean.getNode());
    }

    public Embeddable getEmbeddable(HippoBean linkedBean) {
        ResourceManager manager = getResourceManager(linkedBean);
        if (manager instanceof Embeddable) {
            return (Embeddable) manager;
        }
        return null;
    }

    public String getEmbedded(HippoBean linkedBean) {
        Embeddable embeddable = getEmbeddable(linkedBean);
        return embeddable.getEmbedded(linkedBean.getNode());
    }

    private Session getSession(HippoBean bean) throws RepositoryException {
        return bean.getNode().getSession();
    }

    public ResourceManager getResourceManager(HippoBean bean) {
        try {
            String type = bean.getClass().getAnnotation(Node.class).jcrType();
            //String resourcePath = location;//String.format(location, type);
            Session session = getSession(bean);

            IPluginConfig iPluginConfig = new NodePluginConfig(session.getNode(location));
            ExternalResourceService service = new ExternalResourceService(iPluginConfig);

            ResourceManager processor = service.getResourceProcessor(type);
            return processor;
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return null;
    }


    public <T extends HippoBean> T getLinkedBean(HippoBean inBean, String relPath, Class<T> beanMappingClass) {
        HippoMirrorBean mirror = inBean.getBean(relPath, HippoMirrorBean.class);
        if (mirror == null) {
            return null;
        }
        HippoBean bean = mirror.getReferencedBean();
        if (bean == null) {
            return null;
        }
        if (!beanMappingClass.isAssignableFrom(bean.getClass())) {
            log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(),
                    bean.getClass().getName());
            return null;
        }
        return (T) bean;
    }

    public static String getLocation() {
        return location;
    }

    public static void setLocation(String location) {
        ExternalResourceHstEngine.location = location;
    }

}

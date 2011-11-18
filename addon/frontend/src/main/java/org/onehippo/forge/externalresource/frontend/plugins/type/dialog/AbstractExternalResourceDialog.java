package org.onehippo.forge.externalresource.frontend.plugins.type.dialog;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */
public abstract class AbstractExternalResourceDialog<T> extends AbstractDialog<T> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(AbstractExternalResourceDialog.class);

    protected IPluginConfig config;
    protected IPluginContext context;

    protected AbstractExternalResourceDialog(IModel<T> tiModel) {
        super(tiModel);
    }

    public AbstractExternalResourceDialog(IModel<T> model, IPluginContext context, IPluginConfig config) {
        super(model);
        this.context = context;
        this.config = config;
    }

    public class ExternalImageFallback extends Image {

        public ExternalImageFallback(String id, String imageUrl, ResourceReference defaultResource) {
            super(id);
            if ((imageUrl == null || imageUrl.equals(""))) {
                this.setImageResourceReference(defaultResource, null);
            } else {
                add(new AttributeModifier("src", true, new Model(imageUrl)));
            }
        }


    }

     /**
     * Use the IBrowseService to select the node referenced by parameter path
     *
     * @param nodeModel Absolute path of node to browse to
     * @throws javax.jcr.RepositoryException
     */
    protected void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        ((UserSession) org.apache.wicket.Session.get()).getJcrSession().refresh(false);

        getContext().getService(getConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
    }

    protected ExternalResourceService getExternalResourceService() {
        IPluginContext context = getContext();
        ExternalResourceService service = context.getService(getConfig().getString("external.processor.id",
                "external.processor.service"), ExternalResourceService.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    public IPluginConfig getConfig() {
        return config;
    }

    public void setConfig(IPluginConfig config) {
        this.config = config;
    }

    public IPluginContext getContext() {
        return context;
    }

    public void setContext(IPluginContext context) {
        this.context = context;
    }
}

package org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.still.field.plugin;

import org.apache.wicket.Response;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.WebResponse;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.editor.compare.StreamComparer;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.still.StillManagerDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;

/**
 * @version $Id$
 */
public class ImageDisplayPlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    ByteSizeFormatter formatter = new ByteSizeFormatter();

    private IEditor.Mode mode;

    private final DialogAction action;

    public ImageDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogFactory dialogFactory = createDialogFactory();

        this.action = new DialogAction(dialogFactory, getDialogService());

        this.mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        if (mode == IEditor.Mode.COMPARE && config.containsKey("model.compareTo")) {
            IModelReference<Node> baseModelRef = context.getService(config.getString("model.compareTo"),
                    IModelReference.class);
            boolean doCompare = false;
            if (baseModelRef != null) {
                IModel<Node> baseModel = baseModelRef.getModel();
                Node baseNode = baseModel.getObject();
                Node currentNode = getModel().getObject();
                if (baseNode != null && currentNode != null) {
                    try {
                        InputStream baseStream = baseNode.getProperty("jcr:data").getStream();
                        InputStream currentStream = currentNode.getProperty("jcr:data").getStream();
                        StreamComparer comparer = new StreamComparer();
                        if (!comparer.areEqual(baseStream, currentStream)) {
                            doCompare = true;
                        }
                    } catch (RepositoryException e) {
                        log.error("Could not compare streams", e);
                    }
                }
            }
            if (doCompare) {
                Fragment fragment = new Fragment("fragment", "compare", this);
                Fragment baseFragment = createResourceFragment("base", baseModelRef.getModel());
                baseFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-removed"), " "));
                fragment.add(baseFragment);

                Fragment currentFragment = createResourceFragment("current", getModel());
                currentFragment.add(new AttributeAppender("class", new Model<String>("hippo-diff-added"), " "));
                fragment.add(currentFragment);
                add(fragment);
            } else {
                add(createResourceFragment("fragment", getModel()));
            }
        } else {
            add(createResourceFragment("fragment", getModel()));
        }
    }

    private IDialogFactory createDialogFactory() {
        return new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog<JcrNodeModel> createDialog() {
                return new StillManagerDialog(ImageDisplayPlugin.this.getModel(), getPluginContext(), getPluginConfig());
            }
        };
    }

    private Fragment createResourceFragment(String id, IModel<Node> model) {
        final JcrResourceStream resource = new JcrResourceStream(model);
        Fragment fragment = new Fragment(id, "unknown", this);
        try {
            Node node = getModelObject();
            final String filename;
            if (node.getDefinition().getName().equals("*")) {
                filename = node.getName();
            } else {
                filename = node.getParent().getName();
            }
            String mimeType = node.getProperty("jcr:mimeType").getString();
            if (mimeType.indexOf('/') > 0) {
                String category = mimeType.substring(0, mimeType.indexOf('/'));
                if ("image".equals(category)) {
                    JcrImage img = new JcrImage("image", resource);
                    switch (mode) {
                        case EDIT:
                            fragment = new Fragment(id, "clickable-image", this);
                            AjaxLink link = new AjaxLink("image-link") {
                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    action.execute();
                                }
                            };

                            link.add(img);
                            link.add(new Label("image-caption", new StringResourceModel("click", this, null))) ;
                            fragment.add(link);
                            break;
                        default:
                            fragment = new Fragment(id, "image", this);
                            fragment.add(img);
                            break;
                    }
                } else {
                    fragment = new Fragment(id, "embed", this);
                    fragment.add(new Label("filesize", new Model<String>(formatter.format(resource.length()))));
                    fragment.add(new Label("mimetype", new Model<String>(resource.getContentType())));
                    fragment.add(new ResourceLink<Void>("link", new JcrResource(resource) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void configureResponse(Response response) {
                            if (response instanceof WebResponse) {
                                ((WebResponse) response).setHeader("Content-Disposition", "attachment; filename="
                                        + filename);
                            }
                        }
                    }) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onDetach() {
                            resource.detach();
                            super.onDetach();
                        }

                    });
                }
            }
        } catch (RepositoryException
                ex) {
            log.error(ex.getMessage());
        }
        return fragment;
    }

    @Override
    protected void onModelChanged
            () {
        replace(createResourceFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }

}

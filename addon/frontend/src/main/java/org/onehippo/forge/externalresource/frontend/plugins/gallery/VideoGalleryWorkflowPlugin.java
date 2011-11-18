/*
 *  Copyright 2009 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.externalresource.frontend.plugins.gallery;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.MultiFileUploadDialog;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.*;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.imports.MediaMosaImportDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

public class VideoGalleryWorkflowPlugin extends CompatibilityWorkflowPlugin<GalleryWorkflow> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(VideoGalleryWorkflowPlugin.class);

    public class UploadDialog extends MultiFileUploadDialog {
        private static final long serialVersionUID = 1L;

        public UploadDialog(String[] fileExtensions) {
            super(fileExtensions);
        }

        public IModel getTitle() {
            return new StringResourceModel(VideoGalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""),
                    VideoGalleryWorkflowPlugin.this, null);
        }

        @Override
        protected void handleUploadItem(FileUpload upload) {
            createGalleryItem(upload);
        }

        @Override
        protected void onOk() {
            super.onOk();
            afterUploadItems();
        }
    }

    public String type;
    private List<String> newItems;

    public VideoGalleryWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        newItems = new LinkedList<String>();
    }

    @Override
    public void onModelChanged() {
        AbstractView<StdWorkflow> add;
        addOrReplace(add = new AbstractView<StdWorkflow>("new", createListDataProvider()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                item.add((StdWorkflow) item.getModelObject());
            }
        });
        add.populate();
    }

    private void createGalleryItem(FileUpload upload) {
        try {
            //VideoService service = getVideoService();
            //this is where the magic starts
            String filename = upload.getClientFileName();
            String mimetype;

            mimetype = upload.getContentType();
            InputStream istream = upload.getInputStream();
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            HippoNode node = null;
            try {
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this
                        .getDefaultModel();
                GalleryWorkflow workflow = (GalleryWorkflow) manager
                        .getWorkflow((WorkflowDescriptor) workflowDescriptorModel.getObject());
                String nodeName = getNodeNameCodec().encode(filename);
                String localName = getLocalizeCodec().encode(filename);
                //here is where it goes wrong
                Document document = workflow.createGalleryItem(nodeName, type);
                ((UserSession) Session.get()).getJcrSession().refresh(true);

                JcrNodeModel nodeModel = new JcrNodeModel(new JcrItemModel(document.getIdentity()));

                node = (HippoNode) nodeModel.getNode();

                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
            } catch (WorkflowException ex) {
                VideoGalleryWorkflowPlugin.log.error(ex.getMessage());
                error(ex);
            } catch (MappingException ex) {
                VideoGalleryWorkflowPlugin.log.error(ex.getMessage());
                error(ex);
            } catch (RepositoryException ex) {
                VideoGalleryWorkflowPlugin.log.error(ex.getMessage());
                error(ex);
            }
            if (node != null) {
                try {
                    node.setProperty("hippoexternal:mimeType", mimetype);
                    // node.setProperty("hippoexternal:lastModified", Calendar.getInstance());
                    ExternalResourceService service = getExternalResourceService();
                    ResourceManager processor = service.getResourceProcessor(node.getPrimaryNodeType().getName());
                    processor.create(node, istream, mimetype);
                    node.getSession().save();
                    processor.afterSave(node);
                } catch (Exception ex) {
                    VideoGalleryWorkflowPlugin.log.info(ex.getMessage());
                    error(ex);
                    try {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        defaultWorkflow.delete();
                    } catch (WorkflowException e) {
                        VideoGalleryWorkflowPlugin.log.error(e.getMessage());
                    } catch (MappingException e) {
                        VideoGalleryWorkflowPlugin.log.error(e.getMessage());
                    } catch (RepositoryException e) {
                        VideoGalleryWorkflowPlugin.log.error(e.getMessage());
                    }
                    try {
                        node.getSession().refresh(false);
                    } catch (RepositoryException e) {
                        // deliberate ignore
                    }
                }
                newItems.add(node.getPath());
            }
        } catch (IOException ex) {
            VideoGalleryWorkflowPlugin.log.info("upload of image truncated");
            error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
        } catch (RepositoryException e) {
            VideoGalleryWorkflowPlugin.log.error("upload of image failed", e);
            error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
        }
    }

    private void afterUploadItems() {
        int threshold = getPluginConfig().getAsInteger("select.after.create.threshold", 1);
        if (newItems.size() <= threshold) {
            for (String path : newItems) {
                select(new JcrNodeModel(path));
            }
        }
        newItems.clear();
    }

     protected ExternalResourceService getExternalResourceService() {
        IPluginContext context = getPluginContext();
        ExternalResourceService service = context.getService(getPluginConfig().getString("external.processor.id",
                "external.processor.service"), ExternalResourceService.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    protected IDataProvider<StdWorkflow> createListDataProvider() {
        List<StdWorkflow> list = new LinkedList<StdWorkflow>();
        list.add(0,
                new WorkflowAction("add", new StringResourceModel(getPluginConfig().getString("option.label", "add"),
                        this,
                        null,
                        "Add")
                ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ResourceReference getIcon() {
                        return new ResourceReference(getClass(), "film-add-icon.png");
                    }

                    @Override
                    protected Dialog createRequestDialog() {
                        return createUploadDialog();
                    }
                });
        //delegate to the WorkflowitemManager
        list.add(1,
                new WorkflowAction("import",
                        new StringResourceModel(getPluginConfig().getString("option.label.import", "import-video-label"),
                                this,
                                null,
                                "Add")
                ) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ResourceReference getIcon() {
                        return new ResourceReference(getClass(), "import-16.png");
                    }

                    @Override
                    protected Dialog createRequestDialog() {
                        return createImportDialog();
                    }
                });
        return new ListDataProvider<StdWorkflow>(list);
    }

    private Dialog createImportDialog() {
        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this
                .getDefaultModel();
        Node node = null;
        try {
            node = workflowDescriptorModel.getNode();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        JcrNodeModel nodeModel = new JcrNodeModel(node);

        return new MediaMosaImportDialog(nodeModel, getPluginContext(), getPluginConfig());
    }

    private Dialog createUploadDialog() {
        List<String> galleryTypes = null;
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this
                    .getDefaultModel();
            GalleryWorkflow workflow = (GalleryWorkflow) manager
                    .getWorkflow((WorkflowDescriptor) workflowDescriptorModel.getObject());
            if (workflow == null) {
                VideoGalleryWorkflowPlugin.log.error("No gallery workflow accessible");
            } else {
                galleryTypes = workflow.getGalleryTypes();
            }
        } catch (MappingException ex) {
            VideoGalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
        } catch (RepositoryException ex) {
            VideoGalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            VideoGalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
        }

        Component typeComponent = null;
        if (galleryTypes != null && galleryTypes.size() > 1) {
            DropDownChoice folderChoice;
            type = galleryTypes.get(0);
            typeComponent = new DropDownChoice("type", new PropertyModel(this, "type"), galleryTypes,
                    new TypeChoiceRenderer(this)).setNullValid(false).setRequired(true);
        } else if (galleryTypes != null && galleryTypes.size() == 1) {
            type = galleryTypes.get(0);
            typeComponent = new Label("type", type).setVisible(false);
        } else {
            type = null;
            typeComponent = new Label("type", "default").setVisible(false);
        }

        String[] fileExtensions = new String[0];
        if (getPluginConfig().containsKey("file.extensions")) {
            fileExtensions = getPluginConfig().getStringArray("file.extensions");
        }

        UploadDialog dialog = new UploadDialog(fileExtensions);
        dialog.add(typeComponent);
        return dialog;
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        if (browser != null) {
            try {
                if (nodeModel.getNode() != null
                        && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode()
                        .isNodeType(HippoNodeType.NT_HANDLE))) {
                    if (browser != null) {
                        browser.browse(nodeModel);
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }
    }

}
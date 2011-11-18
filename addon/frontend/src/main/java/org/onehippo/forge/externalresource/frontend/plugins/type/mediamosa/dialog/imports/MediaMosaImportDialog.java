package org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.imports;

import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetType;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.model.AbstractCheckBoxModel;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.*;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.*;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.onehippo.forge.externalresource.frontend.plugins.type.dialog.AbstractExternalResourceDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @version $Id$
 */
public class MediaMosaImportDialog extends AbstractExternalResourceDialog implements IHeaderContributor {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaImportDialog.class);

    private MediaMosaService service;
    private int current;
    private int count;
    private static final int pageSize = 5;
    private String search = "";
    private List<AssetType> assetsToBeImported;

    public MediaMosaImportDialog(IModel model, final IPluginContext context, IPluginConfig config) {
        super(model, context, config);
        setOutputMarkupId(true);
        //this.service = service;
        HippoMediaMosaResourceManager manager = (HippoMediaMosaResourceManager) getExternalResourceService().getResourceProcessor("hippomediamosa:resource");
        if (manager == null) {
            return;
        }
        this.service = manager.getMediaMosaService();
        this.assetsToBeImported = new ArrayList<AssetType>();
        setOkLabel(new StringResourceModel("import", this, null));

        TextField<String> searchText = new TextField<String>("search", new PropertyModel<String>(this, "search"));
        searchText.setOutputMarkupId(true);
        add(setFocus(searchText));

        AjaxButton button = new AjaxButton("searchbutton", new StringResourceModel("search", this, null)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form<?> form) {
                ajaxRequestTarget.addComponent(MediaMosaImportDialog.this);
            }
        };
        add(button);

        Label label = new Label("result", new AbstractReadOnlyModel() {

            @Override
            public Object getObject() {

                int i = ((count - current) < pageSize) ? (count) : current + (pageSize - 1);

                return new StringResourceModel("result", MediaMosaImportDialog.this, null, new Object[]{
                        current,
                        i,
                        count,
                        search}).getString();
            }
        });
        label.setOutputMarkupId(true);
        add(label);

        IDataProvider<AssetType> provider = new IDataProvider<AssetType>() {

            private Map<String, Object> searchMap = new HashMap<String, Object>();

            //todo retrieve from cache
            public Iterator<? extends AssetType> iterator(int first, int count) {
                MediaMosaImportDialog.this.current = (first + 1);
                List<AssetType> list = new ArrayList<AssetType>();
                try {
                    populateSearchMap();
                    list = service.getAssets(count, first, searchMap);
                } catch (ServiceException e) {
                    log.error("Service exception on retrieving assets for the MediaMosa Import dialog", e);
                    error(e.getLocalizedMessage());
                } catch (IOException e) {
                    log.error("IO exception on retrieving assets for the MediaMosa Import dialog", e);
                    error(e.getLocalizedMessage());
                }
                return list.iterator();
            }

            private void populateSearchMap() {
                if (StringUtils.isNotEmpty(search)) {
                    try {
                        searchMap.put("title[]", URLEncoder.encode(search, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        log.error("", e);
                    }
                } else {
                    searchMap.clear();
                }
            }

            //todo cache this:
            //todo idea fire thread for caching 1-100 10x10 results for keyword
            public int size() {
                try {
                    populateSearchMap();
                    MediaMosaImportDialog.this.count = (int) service.getAssetCount(200, searchMap);
                    return MediaMosaImportDialog.this.count;
                } catch (IOException e) {
                    log.error("Service exception on retrieving total asset size for the MediaMosa Import dialog", e);
                    error(e.getLocalizedMessage());
                } catch (ServiceException e) {
                    log.error("IO exception on retrieving total asset size for the MediaMosa Import dialog", e);
                    error(e.getLocalizedMessage());
                }
                return 0;
            }

            public IModel<AssetType> model(AssetType object) {
                return new Model(object);
            }

            public void detach() {
            }
        };

        DataView<AssetType> view = new DataView<AssetType>("item", provider, pageSize) {

            private final Format formatter = new SimpleDateFormat("dd MMM yyyy");

            @Override
            protected void populateItem(Item<AssetType> assetTypeItem) {
                final AssetType asset = assetTypeItem.getModelObject();
                //todo properymodel this
                assetTypeItem.add(new ExternalImageFallback("thumb", asset.getVpxStillUrl(), NO_THUMB));

                String duration = "";
                if (asset.getMediafileDuration() != null) {
                    duration = asset.getMediafileDuration().toXMLFormat();
                }

                assetTypeItem.add(new Label("duration", new Model(duration)));

                assetTypeItem.add(new Label("title", new Model(asset.getDublinCore().getTitle())));
                assetTypeItem.add(new Label("description", new Model(asset.getDublinCore().getDescription())));

                String date = "";
                if (asset.getVideotimestamp() != null) {
                    date = formatter.format(asset.getVideotimestamp().getTime());
                }

                assetTypeItem.add(new Label("facets",
                        new StringResourceModel("facets",
                                MediaMosaImportDialog.this, null, new Object[]{
                                asset.getOwnerId(),
                                date,
                                asset.getViewed()})));


                Fragment fragment = null;

                if (findAsset(asset)) {
                    fragment = new Fragment("actions", "browse", MediaMosaImportDialog.this);

                    AjaxButton browseButton = new AjaxButton("browse-button", new StringResourceModel("browse", this, null)) {
                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            try {
                                MediaMosaImportDialog.this.browseTo(getAsset(asset));
                                MediaMosaImportDialog.this.closeDialog();
                            } catch (RepositoryException e) {
                                log.error("Problem with browsing to asset, probably doesn't exist anymore", e);
                            }
                        }
                    };
                    fragment.add(browseButton);

                } else {
                    fragment = new Fragment("actions", "import", MediaMosaImportDialog.this);
                    AbstractCheckBoxModel cModel = new AbstractCheckBoxModel() {
                        @Override
                        public boolean isSelected() {
                            for (AssetType type : assetsToBeImported) {
                                if (type.getAssetId().equals(asset.getAssetId())) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public void select() {
                            assetsToBeImported.add(asset);
                        }

                        @Override
                        public void unselect() {
                            assetsToBeImported.remove(asset);
                        }
                    };
                    AjaxCheckBox checkBox = new AjaxCheckBox("import-checkbox", cModel) {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                        }
                    };
                    checkBox.setOutputMarkupId(true);
                    fragment.add(checkBox);
                }

                assetTypeItem.add(fragment);
            }

        };
        add(view);
        add(new AjaxPagingNavigator("navigator", view));
    }

    //todo check which is faster
    private boolean findAsset(AssetType assetTypeItem) {
        try {
            //todo cache this, or create different setup
            Session session = ((UserSession) getSession()).getJcrSession();
            QueryManager manager = session.getWorkspace().getQueryManager();
            String quers = String.format("content/videos//element(*,hippomediamosa:resource)[@hippomediamosa:assetid='%s']", assetTypeItem.getAssetId());
            Query query = manager.createQuery(quers, Query.XPATH);
            query.setLimit(1);
            QueryResult result = query.execute();
            return result.getNodes().hasNext();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    private JcrNodeModel getAsset(AssetType assetType) {
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            QueryManager manager = session.getWorkspace().getQueryManager();
            String queryString = String.format("content/videos//element(*,hippomediamosa:resource)[@hippomediamosa:assetid='%s']", assetType.getAssetId());
            Query query = manager.createQuery(queryString, Query.XPATH);
            query.setLimit(1);
            QueryResult result = query.execute();
            if (result.getNodes().hasNext()) {
                return new JcrNodeModel(result.getNodes().nextNode());
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return null;
    }

    private boolean findAsset2(AssetType assetTypeItem) {
        try {
            //todo cache this, or create different setup
            Session session = ((UserSession) getSession()).getJcrSession();
            QueryManager manager = session.getWorkspace().getQueryManager();
            Query query = manager.createQuery("", Query.XPATH);
            query.setLimit(1);
            QueryResult result = query.execute();
            return result.getNodes().hasNext();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    //todo check which is faster
    private List<String> getExistingAssets(List<String> list) {
        List<String> existingAssets = new ArrayList<String>();
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            QueryManager manager = session.getWorkspace().getQueryManager();

            StringBuilder builder = new StringBuilder();
            builder.append("content/videos//element(*,hippomediamosa:resource)[");

            for (String assetId : list) {
                builder.append("@hippomediamosa:assetid='").append(assetId).append("'").append((list.indexOf(assetId) == list.size()) ? "'" : "' or ");
            }

            builder.append("]");

            Query query = manager.createQuery(builder.toString(), Query.XPATH);
            query.setLimit(list.size());
            QueryResult result = query.execute();
            NodeIterator it = result.getNodes();
            while (it.hasNext()) {
                Node existingAsset = it.nextNode();
                //list.remove(existingAsset.getProperty("hippomediamosa:assetid").getString());
                existingAssets.add(existingAsset.getProperty("hippomediamosa:assetid").getString());
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return existingAssets;
    }


    @Override
    public IValueMap getProperties() {
        return CUSTOM;
    }

    public IModel getTitle() {
        String folderName = "unknown";
        try {
            folderName = ((HippoNode) getDefaultModelObject()).getLocalizedName();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return new StringResourceModel("import-title", MediaMosaImportDialog.this, null, new Object[]{folderName});

    }

    protected final static IValueMap CUSTOM = new ValueMap("width=835,height=650").makeImmutable();
    private static final ResourceReference CSS = new CompressedResourceReference(MediaMosaImportDialog.class, "MediaMosaImportDialog.css");
    private static final ResourceReference NO_THUMB = new ResourceReference(MediaMosaImportDialog.class, "no-thumb.jpg");

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(CSS);
    }

    @Override
    protected void onOk() {
        try {
            Node folder = (Node) getDefaultModelObject();
            HippoWorkspace workspace = (HippoWorkspace) folder.getSession().getWorkspace();
            GalleryWorkflow workflow = (GalleryWorkflow) workspace.getWorkflowManager().getWorkflow("gallery", folder);
            for (AssetType type : assetsToBeImported) {
                String title = type.getDublinCore().getTitle();
                if (StringUtils.isEmpty(title)) {
                    title = type.getAssetId();
                } else {
                    title = NodeNameCodec.encode(title, true);
                }
                Document doc = workflow.createGalleryItem(title, "hippomediamosa:resource");
                if (folder.getSession().itemExists(doc.getIdentity())) {
                    Node node = folder.getSession().getNode(doc.getIdentity()); //getnode from doc
                    node.setProperty("hippomediamosa:assetid", type.getAssetId());
                    node.getSession().save();
                    Synchronizable synchronizer = getExternalResourceService().getSynchronizableProcessor("hippomediamosa:resource");
                    SynchronizedActionsWorkflow synchronizedActionsWorkflow = (SynchronizedActionsWorkflow) workspace.getWorkflowManager().getWorkflow("synchronization", node);
                    synchronizedActionsWorkflow.update(synchronizer);
                    log.info("just created: " + type.getAssetId());
                }
            }
        } catch (RepositoryException
                e) {
            log.error("", e);
        } catch (RemoteException
                e) {
            log.error("", e);
        } catch (WorkflowException e) {
            log.error("", e);
        }
    }
}

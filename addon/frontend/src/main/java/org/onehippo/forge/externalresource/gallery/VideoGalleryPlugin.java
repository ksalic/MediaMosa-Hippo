/*
 *  Copyright 2008 Hippo.
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
package org.onehippo.forge.externalresource.gallery;

import org.onehippo.forge.externalresource.gallery.columns.FallbackVideoGalleryListColumnProvider;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.list.DocumentsProvider;
import org.hippoecm.frontend.plugins.standards.list.ExpandCollapseListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.IListColumnProvider;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.JsFunction;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.widget.WidgetBehavior;
import org.hippoecm.frontend.plugins.yui.widget.WidgetSettings;
import org.hippoecm.frontend.widgets.LabelWithTitle;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;

import static org.onehippo.forge.externalresource.gallery.VideoGalleryPlugin.Mode.LIST;
import static org.onehippo.forge.externalresource.gallery.VideoGalleryPlugin.Mode.THUMBNAILS;

public class VideoGalleryPlugin extends ExpandCollapseListingPlugin<Node> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    final static Logger log = LoggerFactory.getLogger(VideoGalleryPlugin.class);

    private static final String VIDEO_BANK_CSS = "VideoGalleryPlugin.css";
    private static final String TOGGLE_LIST_IMG = "toggle_list.png";
    private static final String TOGGLE_THUMBNAIL_IMG = "toggle_thumb.png";

    private static final String VIDEO_FOLDER_TYPE = "hippoexternal:folder";


    enum Mode {
        LIST, THUMBNAILS
    }

    private Mode mode = THUMBNAILS;

    private WebMarkupContainer videoList;
    private AjaxLink<String> toggleLink;
    private Image toggleImage;
   //only thing for this one is maybe to change some css class names!
    public VideoGalleryPlugin(final IPluginContext context, final IPluginConfig config) throws RepositoryException {
        super(context, config);

        add(new AbstractYuiBehavior() {

            private static final long serialVersionUID = -263770450388652348L;

            @Override
            public void addHeaderContribution(IYuiContext context) {
                context.addModule(HippoNamespace.NS, "accordionmanager");
            }
        });


        this.setClassName("hippo-video-images");
        getSettings().setAutoWidthClassName("gallery-name");

        add(videoList = new WebMarkupContainer("video-list"));
        videoList.setOutputMarkupId(true);
        videoList.setVisible(false);
        videoList.add(new VideoItemView("video-item"));

        WidgetSettings settings = new WidgetSettings();
        settings.setCalculateWidthAndHeight(new JsFunction(
                "function(sizes) {return {width: sizes.wrap.w, height: sizes.wrap.h-25};}"));
        videoList.add(new WidgetBehavior(settings));

        addButton(toggleLink = new AjaxLink<String>("toggle", new Model<String>()) {

            private static final long serialVersionUID = 4491421913280564773L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                mode = mode == LIST ? THUMBNAILS : LIST;
                redraw();

            }
        });
        toggleLink.setOutputMarkupId(true);

        toggleImage = new Image("toggleimg", TOGGLE_LIST_IMG);
        toggleImage.setOutputMarkupId(true);
        toggleLink.add(toggleImage);
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        if (mode == LIST) {
            this.dataTable.setVisible(true);
            this.videoList.setVisible(false);
            toggleImage = new Image("toggleimg", TOGGLE_LIST_IMG);
        } else {
            this.dataTable.setVisible(false);
            this.videoList.setVisible(true);
            toggleImage = new Image("toggleimg", TOGGLE_THUMBNAIL_IMG);
        }

        toggleLink.replace(toggleImage);
    }

    @Override
    public void renderHead(HtmlHeaderContainer container) {
        super.renderHead(container);

        ResourceReference cssResourceReference = new ResourceReference(VideoGalleryPlugin.class, VIDEO_BANK_CSS);
        container.getHeaderResponse().renderCSSReference(cssResourceReference);
    }

    @Override
    protected ISortableDataProvider<Node> newDataProvider() {
        return new DocumentsProvider(getModel(), new DocumentListFilter(getPluginConfig()),
                getTableDefinition().getComparators());
    }

    @Deprecated
    @Override
    protected IListColumnProvider getDefaultColumnProvider() {
        return new FallbackVideoGalleryListColumnProvider();
    }

    private class VideoItemView extends RefreshingView<Node> {

        private static final long serialVersionUID = 9009788253056596052L;
        private org.apache.wicket.markup.repeater.Item<Node> previousSelected;

        public VideoItemView(String id) {
            super(id);

            setOutputMarkupId(true);

            setItemReuseStrategy(new ReuseIfModelsEqualStrategy());
        }

        @Override
        protected Iterator<IModel<Node>> getItemModels() {
            ArrayList<IModel<Node>> nodeModels = new ArrayList<IModel<Node>>();

            IDataProvider<Node> dataProvider = VideoGalleryPlugin.this.dataTable.getDataProvider();
            if (dataProvider != null) {
                try {
                    Iterator<? extends Node> iterator = dataProvider.iterator(0, dataProvider.size());
                    while (iterator.hasNext()) {
                        Node node = iterator.next();
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            if (node.hasNode(node.getName())) {
                                Node videoSet = node.getNode(node.getName());
                                try {
                                    Item primItem = JcrHelper.getPrimaryItem(videoSet);
                                    if (primItem.isNode()) {
                                        if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                            nodeModels.add(new JcrNodeModel(node));
                                        } else {
                                            log.warn("primary item of video set must be of type "
                                                    + HippoNodeType.NT_RESOURCE);
                                        }
                                    }
                                } catch (ItemNotFoundException e) {
                                    log.debug("videoSet must have a primary item. " + node.getPath()
                                            + " probably not of correct video set type");
                                }
                            }
                        } else if (node.isNodeType(VIDEO_FOLDER_TYPE)) {
                            nodeModels.add(new JcrNodeModel(node));
                        } else {
                            log.info("invalid node type, not adding to the list of items");
                        }
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
            }
            return nodeModels.iterator();
        }

        @Override
        protected void populateItem(final org.apache.wicket.markup.repeater.Item<Node> listItem) {
            listItem.add(new AttributeAppender("class", true, new Model<String>("selected"), " ") {
                private static final long serialVersionUID = 7296628905659498502L;

                @Override
                public boolean isEnabled(Component component) {
                    IModel<Node> selectedModel = getSelectedModel();
                    boolean selected = selectedModel != null && selectedModel.equals(listItem.getDefaultModel());
                    if (selected && previousSelected == null) {
                        previousSelected = listItem;
                    }
                    return selected;
                }
            });
            listItem.setOutputMarkupId(true);

            final JcrNodeModel imgNodeModel = (JcrNodeModel) listItem.getDefaultModel();
            Node node = imgNodeModel.getNode();

            try {
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    if (node.hasNode(node.getName())) {
                        Node videoSet = node.getNode(node.getName());
                        try {
                            Item primItem = JcrHelper.getPrimaryItem(videoSet);
                            if (primItem.isNode()) {
                                if (((Node) primItem).isNodeType(HippoNodeType.NT_RESOURCE)) {
                                    AjaxLink itemLink = new AjaxLink("itemLink") {
                                        private static final long serialVersionUID = 5875761797488127764L;

                                        @Override
                                        public void onClick(AjaxRequestTarget target) {
                                            handleSelect(listItem, target);
                                        }
                                    };

                                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                                    folderIcon.setVisible(false);
                                    itemLink.add(folderIcon);
                                    //todo big image fallback
                                    itemLink.add(new VideoContainer("thumbnail", new JcrNodeModel((Node) primItem),
                                            getPluginContext(), getPluginConfig()));

                                    itemLink.add(new LabelWithTitle("title", new NodeTranslator(new JcrNodeModel(node))
                                            .getNodeName()));
                                    listItem.add(itemLink);
                                } else {
                                    log.warn("primary item of video set must be of type " + HippoNodeType.NT_RESOURCE);
                                }
                            }
                        } catch (ItemNotFoundException e) {
                            log.debug("videoSet must have a primary item. " + node.getPath()
                                    + " probably not of correct video set type");
                        }
                    }

                } else if (node.isNodeType(VIDEO_FOLDER_TYPE)) {
                    AjaxLink itemLink = new AjaxLink("itemLink") {
                        private static final long serialVersionUID = -129729352294418582L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            handleSelect(listItem, target);
                        }
                    };

                    Panel thumbnail = new EmptyPanel("thumbnail");
                    Image folderIcon = new Image("folder-icon", "hippo-gallery-folder.png");
                    itemLink.add(folderIcon);
                    itemLink.add(thumbnail);
                    itemLink.add(new Label("title", new NodeTranslator(new JcrNodeModel(node)).getNodeName()));
                    listItem.add(itemLink);
                }

            } catch (RepositoryException e) {
                listItem.add(new EmptyPanel("thumbnail"));

            }
        }

        private void handleSelect(org.apache.wicket.markup.repeater.Item<Node> listItem, AjaxRequestTarget target) {
            setSelectedModel(listItem.getModel());

            if (previousSelected != null) {
                target.addComponent(previousSelected);
            }
            target.addComponent(listItem);
            target.focusComponent(listItem);

            previousSelected = listItem;
        }
    }

}

/*
 *  Copyright 2010 Hippo.
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
package org.onehippo.forge.externalresource.gallery.columns;

import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.columns.compare.CalendarComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.LongPropertyComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.MimeTypeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.compare.SizeComparator;
import org.hippoecm.frontend.plugins.gallery.columns.render.DatePropertyRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.SizeRenderer;
import org.hippoecm.frontend.plugins.gallery.columns.render.StringPropertyRenderer;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.plugins.standards.list.comparators.NameComparator;
import org.hippoecm.frontend.plugins.standards.list.resolvers.DocumentTypeIconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.EmptyRenderer;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.onehippo.forge.externalresource.gallery.render.VideoIconRenderer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

public class VideoGalleryColumnProviderPlugin extends AbstractListColumnProviderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String primaryItemName;
    private String iconRenderer;

    public VideoGalleryColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        primaryItemName = config.getString("primaryItemName");
        iconRenderer = config.getString("documentTypeIconRenderer");
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return CSSPackageResource.getHeaderContribution(VideoGalleryColumnProviderPlugin.class, "VideoGalleryStyle.css");
    }

    public List<ListColumn<Node>> getColumns() {
        List<ListColumn<Node>> columns = new ArrayList<ListColumn<Node>>();

        //image icon
        ListColumn<Node> column = new ListColumn<Node>(new Model<String>(""), null);
        if ("cssIconRenderer".equals(iconRenderer)) {
            column.setRenderer(new EmptyRenderer<Node>());
            column.setAttributeModifier(new DocumentTypeIconAttributeModifier());
        } else if ("resourceIconRenderer".equals(iconRenderer)) {
            column.setRenderer(new IconRenderer());
            column.setAttributeModifier(new IconAttributeModifier());
        } else {
            column.setRenderer(new VideoIconRenderer());
            column.setAttributeModifier(new IconAttributeModifier());
        }
        //column.setAttributeModifier(new DocumentTypeIconAttributeModifier());
        column.setCssClass("video-gallery-icon");
        columns.add(column);

        //node name
        column = new ListColumn<Node>(new ClassResourceModel("video-name", Translations.class), "name");
        column.setComparator(new NameComparator());
        column.setCssClass("video-name");
        columns.add(column);

        return columns;
    }

    /**
     * We have to be careful with adding another column here; the current implementation allows for only one column that
     * can contain really long values which will be clipped so the UI doesn't break. To allow for more columns that
     * behave like this, while keeping performance acceptable we will have to go for a *real* widget.
     */
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();

        //width
        ListColumn<Node> column = new ListColumn<Node>(new ClassResourceModel("video-width", Translations.class), "width");
        column.setRenderer(new StringPropertyRenderer("hippoexternal:width", primaryItemName) {
            private static final long serialVersionUID = 3101814681153621254L;

            @Override
            protected String getValue(Property p) throws RepositoryException {
                return super.getValue(p) + "px";
            }
        });
        column.setComparator(new LongPropertyComparator("hippoexternal:width", primaryItemName));
        column.setCssClass("video-width");
        columns.add(column);

        //height
        column = new ListColumn<Node>(new ClassResourceModel("video-height", Translations.class), "height");
        column.setRenderer(new StringPropertyRenderer("hippoexternal:height", primaryItemName) {
            private static final long serialVersionUID = -6372044277538266404L;

            @Override
            protected String getValue(Property p) throws RepositoryException {
                return super.getValue(p) + "px";
            }
        });
        column.setComparator(new LongPropertyComparator("hippoexternal:height", primaryItemName));
        column.setCssClass("video-height");
        columns.add(column);

        //Mimetype
        column = new ListColumn<Node>(new ClassResourceModel("video-mimetype", Translations.class), "mimetype");
        column.setRenderer(new StringPropertyRenderer("hippoexternal:mimeType", primaryItemName));
        column.setComparator(new MimeTypeComparator("hippoexternal:mimeType", primaryItemName));
        column.setCssClass("video-mimetype");
        columns.add(column);

        //filesize
        column = new ListColumn<Node>(new ClassResourceModel("video-size", Translations.class), "size");
        column.setRenderer(new SizeRenderer("hippoexternal:size", primaryItemName));
        column.setComparator(new SizeComparator("hippoexternal:size", primaryItemName));
        column.setCssClass("video-size");
        columns.add(column);

        //Last modified date
        column = new ListColumn<Node>(new ClassResourceModel("video-lastmodified", Translations.class),
                "lastmodified");
        column.setRenderer(new DatePropertyRenderer("hippoexternal:lastModified", primaryItemName));
        column.setComparator(new CalendarComparator("hippoexternal:lastModified", primaryItemName));
        column.setCssClass("video-lastmodified");
        columns.add(column);

        return columns;
    }

}

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

import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.standards.image.JcrImage;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.dragdrop.DragSettings;
import org.hippoecm.frontend.plugins.yui.dragdrop.ImageNodeDragBehavior;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.onehippo.forge.externalresource.gallery.render.VideoContainerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class VideoContainer extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id:";

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(VideoContainer.class);

    private JcrResourceStream stream;
    private int width;
    private int height;

    private static final int FIRST_IMAGE_IN_FILE = 0;

    public VideoContainer(String wicketId, JcrNodeModel model, IPluginContext pluginContext,
                          final IPluginConfig pluginConfig) {
        super(wicketId, model);

        stream = new JcrResourceStream(model);

        int i = pluginConfig.getAsInteger("videobank.thumbnail.size", 60);

        width = i;
        height = i;
               //todo big image fallback
        Image img = null;
        if (stream.getContentType().startsWith("image")) {
            img = new JcrImage("image", stream, width, height);
        } else {
            //todo
            img = new Image("image", new ResourceReference(VideoContainerRenderer.class, "res/video.png")) {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    if (width > 0) {
                        tag.put("width", width);
                    }
                    if (height > 0) {
                        tag.put("height", height);
                    }
                }
            };
        }

        //injectDimensions(model);
        img.add(new ImageNodeDragBehavior(new DragSettings(YuiPluginHelper.getConfig(pluginConfig)), model));
        add(img);
    }

    private void injectDimensions(JcrNodeModel model) {
        ImageReader reader = null;
        try {
            String mimeType = model.getNode().getProperty(JcrConstants.JCR_MIMETYPE).getString();
            reader = ImageUtils.getImageReader(mimeType);

            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(stream.getInputStream());
            reader.setInput(mciis);
            BufferedImage originalImage = reader.read(FIRST_IMAGE_IN_FILE);

            width = originalImage.getWidth();
            height = originalImage.getHeight();

            mciis.close();
        } catch (ValueFormatException ignored) {
            log.debug("Ignoring illegal long value of image property", ignored.getMessage());
        } catch (PathNotFoundException ignored) {
            log.debug("Ignoring missing image property", ignored.getMessage());
        } catch (RepositoryException ignored) {
            log.debug("Ignoring error while reading image property", ignored.getMessage());
        } catch (IOException e) {
            log.error("", e);
        } catch (ResourceStreamNotFoundException e) {
            log.error("", e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    @Override
    public void onDetach() {
        stream.detach();
        super.onDetach();
    }
}

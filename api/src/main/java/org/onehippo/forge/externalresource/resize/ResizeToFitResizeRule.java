package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @version
 */
public class ResizeToFitResizeRule extends ImageProcessorRule {

    private int width;
    private int height;

    public ResizeToFitResizeRule(IPluginConfig config) {
        super(config);
        if (config.containsKey("width")) {
            this.width = config.getInt("width");
        }
        if (config.containsKey("height")) {
            this.height = config.getInt("height");
        }
    }

     public ResizeToFitResizeRule(int width, int height) {
        this.width=width;
        this.height=height;
    }

    public BufferedImage apply(BufferedImage originalImage) {

        return ImageUtils.scaleImage(originalImage, width, height, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
    }
}
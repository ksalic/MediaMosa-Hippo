package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @version
 */
public class RegularWidthResizeRule extends ImageProcessorRule {

    private int width;

    public RegularWidthResizeRule(IPluginConfig config) {
        super(config);
        if (config.containsKey("width")) {
            this.width = config.getInt("width");
        }
    }

    public int getWidth() {
        return width;
    }

    public BufferedImage apply(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double resizeRatio = calcResizeRatio(width, originalWidth);

        int resizeWidth = (int) (originalWidth * resizeRatio);
        int resizeHeight = (int) (originalHeight * resizeRatio);

        BufferedImage scaledImage;
        if (resizeRatio < 1.0d) {
            scaledImage = ImageUtils.scaleImage(originalImage, resizeWidth, resizeHeight,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        } else {
            scaledImage = ImageUtils.scaleImage(originalImage, resizeWidth, resizeHeight,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR, false);
        }

        return scaledImage;
    }

    private double calcResizeRatio(int maxSize, double originalWidth) {

        return maxSize / originalWidth;
    }
}
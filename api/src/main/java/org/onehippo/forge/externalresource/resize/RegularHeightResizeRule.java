package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @version
 */
public class RegularHeightResizeRule extends ImageProcessorRule {

    private int height;

    public RegularHeightResizeRule(IPluginConfig config) {
        super(config);
        if (config.containsKey("height")) {
            this.height = config.getInt("height");
        }
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage apply(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double resizeRatio = calcResizeRatio(height, originalHeight);

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

    private double calcResizeRatio(int maxSize, double originalHeight) {

        return maxSize / originalHeight;
    }
}
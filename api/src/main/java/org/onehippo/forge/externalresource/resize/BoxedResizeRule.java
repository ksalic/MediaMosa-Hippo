package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * User: ksalic
 * Date: 11/4/10
 * Time: 2:01 PM
 */
public class BoxedResizeRule extends ImageProcessorRule {

    private int width;
    private int height;

    public BoxedResizeRule(IPluginConfig config) {
        super(config);
        if (config.containsKey("width")) {
            this.width = config.getInt("width");
        }
        if (config.containsKey("height")) {
            this.height = config.getInt("height");
        }
    }

    public BoxedResizeRule(int width, int height) {
        this.width=width;
        this.height=height;
    }


    public BufferedImage apply(BufferedImage originalImage) {
        double originalWidth = originalImage.getWidth();
        double originalHeight = originalImage.getHeight();

        double heightRatio = height /originalHeight  ;
        double widthRatio = width /originalWidth ;

        double resizeRatio;

        if (heightRatio < widthRatio) {
            resizeRatio = heightRatio;
        } else {
            resizeRatio = widthRatio;
        }

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
}
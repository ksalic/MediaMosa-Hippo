package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

import java.awt.image.BufferedImage;

/**
 * @version
 */
public class CropRule extends ImageProcessorRule {

    private int x;
    private int y;
    private int width;
    private int height;


    public CropRule(IPluginConfig config) {
        super(config);
        if (config.containsKey("x")) {
            this.x = config.getInt("x");
        }
        if (config.containsKey("y")) {
            this.y = config.getInt("y");
        }
        if (config.containsKey("width")) {
            this.width = config.getInt("width");
        }
        if (config.containsKey("height")) {
            this.height = config.getInt("height");
        }
    }

    public BufferedImage apply(BufferedImage originalImage) {

        return originalImage.getSubimage(x,y,width,height);
    }
}
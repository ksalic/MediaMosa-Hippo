package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

import java.awt.image.BufferedImage;

/**
 * @version
 */
public abstract class ImageProcessorRule {

    //private IPluginConfig config;

    protected String type;

    public ImageProcessorRule(IPluginConfig config) {
        //this.config = config;
         if (config.containsKey("type")) {
            this.type = config.getString("type");
        }
    }

   /* public Object get(String property) {
        if (config.containsKey(property)) {
            return config.get("width");
        }
        return null;
    }*/

    public ImageProcessorRule() {
    }

    public String getType() {
        return type;
    }

    public abstract BufferedImage apply(BufferedImage originalImage);
}

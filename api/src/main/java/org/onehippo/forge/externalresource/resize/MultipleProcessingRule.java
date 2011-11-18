package org.onehippo.forge.externalresource.resize;

import org.hippoecm.frontend.plugin.config.IPluginConfig;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * @version
 */
public class MultipleProcessingRule extends ImageProcessorRule {

    private IPluginConfig config;

    public MultipleProcessingRule(IPluginConfig config) {
        super(config);
        this.config = config;
    }

    public BufferedImage apply(BufferedImage originalImage) {
        Set<IPluginConfig> pluginSet = config.getPluginConfigSet();

        for(IPluginConfig iPluginConfig : pluginSet){
          String resizeRuleClass = iPluginConfig.getString("plugin.class");

            if (resizeRuleClass != null) {
                try {
                    Class resizeRule = Class.forName(resizeRuleClass);
                    Constructor con = resizeRule.getConstructor(new Class[]{IPluginConfig.class});
                    ImageProcessorRule rule = (ImageProcessorRule) con.newInstance(new Object[]{iPluginConfig});
                    originalImage = rule.apply(originalImage);
                } catch (Exception e) {
                    // ignore?
                }
            }
        }
        return originalImage;
    }

}
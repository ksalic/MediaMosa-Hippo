package org.onehippo.forge.externalresource.resize;

import java.awt.image.BufferedImage;

/**
 * @version
 */
public class NoActionRule extends ImageProcessorRule {

    public NoActionRule() {
    }

    public BufferedImage apply(BufferedImage originalImage) {
        return originalImage;
    }

}
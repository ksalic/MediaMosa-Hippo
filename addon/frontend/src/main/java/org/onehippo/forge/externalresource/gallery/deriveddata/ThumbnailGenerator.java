package org.onehippo.forge.externalresource.gallery.deriveddata;

import org.onehippo.forge.externalresource.resize.BoxedResizeRule;
import org.onehippo.forge.externalresource.resize.ImageProcessorRule;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.hippoecm.repository.ext.DerivedDataFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;

/**
 * @version
 */
@Deprecated
public class ThumbnailGenerator extends DerivedDataFunction {

    private static final int FIRST_IMAGE_IN_FILE = 0;

    private static Logger log = LoggerFactory.getLogger(ThumbnailGenerator.class);

    @Override
    public Map<String, Value[]> compute(Map<String, Value[]> stringMap) {
        if (stringMap.containsKey("preview")) {
            try {
                Value preview = (Value) stringMap.get("preview")[0];
                if (preview != null) {

                    InputStream stream = preview.getBinary().getStream();

                    ImageProcessorRule rule = new BoxedResizeRule(60, 60);

                    String mimeType = stringMap.get("mimeType")[0].getString();

                    InputStream out = createThumbnailWithResizeRule(stream, rule, mimeType);

                    stringMap.put("thumbnail", new Value[]{
                            getValueFactory().createValue(out)
                    });

                    Calendar calendar = Calendar.getInstance();

                    stringMap.put("date", new Value[]{
                            getValueFactory().createValue(calendar)
                    });
                }
            } catch (RepositoryException e) {
                log.error("", e);
            } catch (GalleryException e) {
                log.error("", e);
            }
        } else{

        }
        return stringMap;
    }


    /**
     * Creates a thumbnail version of an image.
     * The maxsize parameter determines how the image is scaled: it is the maximum size
     * of both the width and height. If the original height is greater than the original
     * width, then the height of the scaled image will be equal to max size. The same
     * goes for the original width: if it is greater than the original height, the width of
     * the scaled image is equal to maxsize.
     *
     * @param imageData the original image data
     * @param rule      the rule for scaling the image
     * @param mimeType  the mime type of the image
     * @return the scaled image stream
     */
    public InputStream createThumbnailWithResizeRule(InputStream imageData, ImageProcessorRule rule, String mimeType) throws GalleryException {
        if (rule == null) {
            throw new IllegalArgumentException("A rule should be implemented");
        }
        if (mimeType == null || "".equals(mimeType.trim())) {
            throw new IllegalArgumentException("A mime type should be provided");
        }
        if (imageData == null) {
            throw new IllegalArgumentException("We cannot create a thumbnail for a NULL input stream");
        }

        //IE uploads jpeg files with the non-standard mimetype image/pjpeg for which ImageIO
        //doesn't have an ImageReader. Simply replacing the mimetype with image/jpeg solves this.
        //For more info see http://www.iana.org/assignments/media-types/image/ and
        //http://groups.google.com/group/comp.infosystems.www.authoring.images/msg/7706603e4bd1d9d4?hl=en
        if (mimeType.equals(ResourceHelper.MIME_IMAGE_PJPEG)) {
            mimeType = ResourceHelper.MIME_IMAGE_JPEG;
        }

        ImageReader reader = null;
        ImageWriter writer = null;
        try {
            reader = ImageUtils.getImageReader(mimeType);
            writer = ImageUtils.getImageWriter(mimeType);

            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(imageData);
            reader.setInput(mciis);

            BufferedImage originalImage = reader.read(FIRST_IMAGE_IN_FILE);

            BufferedImage newImage = rule.apply(originalImage);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            writer.write(newImage);
            ios.flush();
            ios.close();
            mciis.close();

            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new GalleryException("Could not resize image", e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (writer != null) {
                writer.dispose();
            }
        }
    }
}

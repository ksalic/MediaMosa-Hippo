package org.onehippo.forge.externalresource.api;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.imageutil.ImageUtils;
import org.hippoecm.frontend.plugins.gallery.model.GalleryException;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.onehippo.forge.externalresource.resize.ImageProcessorRule;
import org.onehippo.forge.externalresource.resize.ResizeToFitResizeRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.jcr.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Calendar;

/**
 * @version $Id$
 */

public class HippoRedFiveResourceManager extends ResourceManager {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoRedFiveResourceManager.class);

    private String url;
    private String username;
    private String password;

    public HippoRedFiveResourceManager(IPluginConfig config, ResourceInvocationType type) {
        super(config, type);
        if (config.containsKey("url")) {
            this.url = config.getString("url");
        }
        if (config.containsKey("username")) {
            this.username = config.getString("username");
        }
        if (config.containsKey("password")) {
            this.password = config.getString("password");
        }
    }

    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {

        String response = submitFile(istream, getUrl(), mimetype, node.getName());

        Document document = parseXmlFile(response, true);

        String videoUrl = document.getElementsByTagName("url").item(0).getTextContent();
        String imageUrl = document.getElementsByTagName("image").item(0).getTextContent();

        org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
        HttpMethod getMethod = new GetMethod(imageUrl);

        client.executeMethod(getMethod);

        InputStream is = getMethod.getResponseBodyAsStream();
        String mimeType = getMethod.getResponseHeader("content-type").getValue();

        Node preview;
        if (node.hasNode("hippoexternal:preview")) {
            preview = node.getNode("hippoexternal:preview");
        } else {
            preview = node.addNode("hippoexternal:preview", "hippo:resource");
        }

        preview.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(is));
        preview.setProperty("jcr:mimeType", mimeType);
        preview.setProperty("jcr:lastModified", Calendar.getInstance());

        Node thumbnail;
        if (node.hasNode("hippoexternal:thumbnail")) {
            thumbnail = node.getNode("hippoexternal:thumbnail");
        } else {
            thumbnail = node.addNode("hippoexternal:thumbnail", "hippo:resource");
        }

        InputStream stream = preview.getProperty("jcr:data").getBinary().getStream();

        ImageProcessorRule rule = new ResizeToFitResizeRule(60, 60);
        InputStream out = createThumbnailWithResizeRule(stream, rule, mimeType);
        thumbnail.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(out));
        thumbnail.setProperty("jcr:mimeType", mimeType);
        thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());

        node.setProperty("hipporedfive:url", videoUrl);
    }

    @Override
    public void afterSave(Node node) {
    }

    @Override
    public void delete(Node node) {
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

            BufferedImage originalImage = reader.read(0);

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


    public static String submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        String red5Url = EntityUtils.toString(resEntity);
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return red5Url;
    }

    public static Document parseXmlFile(String string, boolean validating) {
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(string)));
            return doc;
        } catch (SAXException e) {
            // A parsing error occurred; the xml input is not valid
        } catch (ParserConfigurationException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

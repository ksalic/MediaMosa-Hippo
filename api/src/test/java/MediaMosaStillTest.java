import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.AssetType;
import nl.uva.mediamosa.model.StillDetailType;
import nl.uva.mediamosa.model.StillType;
import nl.uva.mediamosa.util.ServiceException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobContext;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobListener;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobScheduler;
import org.onehippo.forge.externalresource.api.utils.NodePluginConfig;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id$
 */
@Ignore(value = "no external stuff for tests")
public class MediaMosaStillTest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaStillTest.class);

    MediaMosaService mediaMosaService;
    private Session session;

    @Before
    public void setUp() throws Exception {
        mediaMosaService = new MediaMosaService("http://10.10.100.164/mediamosa");
        try {
            mediaMosaService.setCredentials("Hippo", "VERttUnK2fWlc0F3IceA21awRj");
        } catch (ServiceException e) {
            log.error("e {}", e);
        }

        if (session == null) {
            try {
                final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
                session = repository.login("admin", "admin".toCharArray());
            } catch (RepositoryException e) {
                log.error("Error connecting to the hippo repository", e);
            }
        }
    }

    private String userId = "Hippo";
    private String password = "VERttUnK2fWlc0F3IceA21awRj";
    private String url = "http://10.10.100.164/mediamosa";

    @Test
    public void testStills() throws Exception {

        Node resourcemanagerNode = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/externalResourceService/hippomediamosa:resource");


        HippoMediaMosaResourceManager resourceManager = new HippoMediaMosaResourceManager(new JcrPluginConfig(new JcrNodeModel(resourcemanagerNode)), ResourceInvocationType.CMS);


        AssetType assetType = mediaMosaService.getAssets().get(20);

        AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetType.getAssetId());

        final String assetId = assetDetailsType.getAssetId();
        final String mediaId = assetDetailsType.getMediafiles().getMediafile().get(0).getMediafileId();

        final StillType stillType = mediaMosaService.getStills(assetId, userId, null);

        String defaultId = null;

        for (StillDetailType detailType : stillType.getStills()) {
            if (detailType.isStillDefault()) {
                defaultId = detailType.getStillId();
            }
            System.out.println(detailType.getStillId() + "__" + detailType.getStillTimeCode() + "__" + detailType.isStillDefault());
        }
        Map map = new HashMap();
        map.put("still_type", "second");
        //map.put("still_per_mediafile", 1);
        map.put("still_every_second", 1);
        map.put("start_time", 12);
        map.put("end_time", 12);


        MediaMosaJobContext context = new MediaMosaJobContext();

        MediaMosaJobListener listener = new MediaMosaJobListener() {
            public void whileInprogress(String assetId) {
                System.out.println("inprogress");
            }

            public void onFinished(String assetId) {
                System.out.println("finished");

                StillType stillTypeAf = null;
                try {
                    stillTypeAf = mediaMosaService.getStills(assetId, userId, null);
                } catch (ServiceException e) {
                    log.error("", e);
                } catch (IOException e) {
                    log.error("", e);
                }

                System.out.println("before:" + stillType.getStills().size());

                for (StillDetailType detailType : stillType.getStills()) {
                    System.out.println(detailType.getStillId() + "__" + detailType.getStillTimeCode() + "__" + detailType.isStillDefault());
                }

                System.out.println("after:" + stillTypeAf.getStills().size());

                System.out.println("expected to be 12 seconds");

                for (StillDetailType detailType : stillTypeAf.getStills()) {
                    System.out.println(detailType.getStillId() + "__" + detailType.getStillTimeCode() + "__" + detailType.isStillDefault());
                }
            }

            public void whileWaiting(String assetId) {
                System.out.println("waiting");
            }

            public void onFailed(String assetId) {
                System.out.println("failed");
            }

            public void onCancelled(String assetId) {
                System.out.println("cancelled");
            }
        };

        context.add(listener);
        context.setAssetId(assetId);
        context.setJobId(String.valueOf(mediaMosaService.createStill(assetId, mediaId,userId, map).getJobId()));
        context.setResourceManager(resourceManager);

        MediaMosaJobScheduler.getInstance().offer(context);

        Thread.sleep(1000000);

    }

    @Test
    public void testNodeConfig() throws Exception {
        Node node = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService");

        IPluginConfig iPluginConfig = new NodePluginConfig(node);

        System.out.println(iPluginConfig.getString("gallery.processor.id"));
        System.out.println(iPluginConfig.size());
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getBoolean("upscaling"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getAsBoolean("upscaling", false));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getAsBoolean("test", false));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getBoolean("test"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getBoolean("upscaling"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getLong("width"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getInt("width"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getInt("width2", 200));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getAsInteger("width"));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getAsInteger("width2", 100));
        System.out.println(iPluginConfig.getPluginConfig("dcrgallery:banner384w").getAsInteger("width2", 100));

    }
}

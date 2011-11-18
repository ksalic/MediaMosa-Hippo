import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.AssetType;
import nl.uva.mediamosa.model.JobType;
import nl.uva.mediamosa.util.ServiceException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobListener;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @version $Id$
 */
@Ignore(value = "no external stuff for tests")
public class SchedualingTest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(SchedualingTest.class);

    private final static ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(3);

    private final static Timer timer = new Timer();

    private Session session;

    @Before
    public void setUp() throws Exception {
        if (session == null) {
            try {
                final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
                session = repository.login("admin", "admin".toCharArray());
            } catch (RepositoryException e) {
                log.error("Error connecting to the hippo repository", e);
            }
        }
    }

    private Map<String, ScheduledFuture> map = new ConcurrentHashMap<String, ScheduledFuture>();

    @After
    public void tearDown() throws Exception {
        if (session != null) {
            session.logout();
        }
    }

    @Test
    public void testConfigPlugin() throws Exception {
        Node resourcemanagerNode = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/externalResourceService");


        JcrClusterConfig config = new JcrClusterConfig(new JcrNodeModel(resourcemanagerNode));

       // System.out.println(config);

        //NodePluginConfig config = new NodePluginConfig(resourcemanagerNode);

        System.out.println(config.getPluginConfigSet().size())  ;

        System.out.println(config);

        for(IPluginConfig pluginConfig: config.getPluginConfigSet()){
            System.out.println(pluginConfig.getName());
        }


    }

    @Test
    public void testShedualing() throws Exception {
        Node resourcemanagerNode = session.getNode("/hippo:configuration/hippo:frontend/cms/cms-services/externalResourceService/hippomediamosa:resource");

        HippoMediaMosaResourceManager resourceManager = new HippoMediaMosaResourceManager(new JcrPluginConfig(new JcrNodeModel(resourcemanagerNode)), ResourceInvocationType.CMS);


        MediaMosaJobListener listener = new MediaMosaJobListener() {

            public void whileInprogress(String assetId) {
                System.out.println("inprogress" + assetId);
            }

            public void onFinished(String assetId) {
                System.out.println("finished" + assetId);
            }

            public void whileWaiting(String assetId) {
                System.out.println("waiting" + assetId);
            }

            public void onFailed(String assetId) {
                System.out.println("failed" + assetId);
            }

            public void onCancelled(String assetId) {
                System.out.println("cancelled" + assetId);
            }
        };

        MediaMosaService service = resourceManager.getMediaMosaService();

        List<AssetType> list = service.getAssets();

        AssetType asset = list.get(2);

        Map map = new HashMap();
        map.put("still_type", "normal");
        map.put("still_per_mediafile", 1);
        map.put("start_time", 2);

        String assetId = asset.getAssetId();

        AssetDetailsType detailsType = service.getAssetDetails(assetId);

        JobType type = service.createStill(detailsType.getAssetId(), detailsType.getMediafiles().getMediafile().get(0).getMediafileId(), "Hippo", map);

        MediaMosaJobContext context = new MediaMosaJobContext();
        context.add(listener);
        context.setResourceManager(resourceManager);
        context.setJobId(String.valueOf(type.getJobId()));
        context.setAssetId(assetId);


        //offer(context);
        offer2(context);
        Thread.sleep(1000000);
    }

    private synchronized void offer(MediaMosaJobContext context) {
        JobFutureTask jobtask = new JobFutureTask(context);
        map.put(context.getJobId(), scheduler.scheduleAtFixedRate(jobtask, 10, 10, TimeUnit.SECONDS));
    }

    private synchronized void offer2(MediaMosaJobContext context) {
        JobFutureTaskTimer jobtask = new JobFutureTaskTimer(context);
        timer.schedule(jobtask, 10000, 10000);


    }

    private synchronized boolean kill(String jobId) {
        ScheduledFuture future = map.remove(jobId);
        log.info("contains {}", map.containsKey(jobId));
        return future.cancel(true);
    }

    public enum MediaMosaJobState {
        WAITING("WAITING"),
        INPROGRESS("INPROGRESS"),
        FINISHED("FINISHED"),
        CANCELLED("CANCELLED"),
        FAILED("FAILED"),
        UNKNOWN("UNKOWN");

        public static final Map<String, MediaMosaJobState> STATE_TYPE_MAP = new HashMap<String, MediaMosaJobState>();

        static {
            STATE_TYPE_MAP.put(MediaMosaJobState.WAITING.getState(), MediaMosaJobState.WAITING);
            STATE_TYPE_MAP.put(MediaMosaJobState.INPROGRESS.getState(), MediaMosaJobState.INPROGRESS);
            STATE_TYPE_MAP.put(FINISHED.getState(), FINISHED);
            STATE_TYPE_MAP.put(CANCELLED.getState(), CANCELLED);
            STATE_TYPE_MAP.put(FAILED.getState(), FAILED);
        }

        private final String state;

        MediaMosaJobState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }

        public static MediaMosaJobState getType(final String state) {
            MediaMosaJobState type = STATE_TYPE_MAP.get(state);
            if (type != null) {
                return type;
            }
            return UNKNOWN;
        }
    }

    private class JobFutureTaskTimer extends TimerTask {

        MediaMosaJobContext context;
        private boolean done;

        private JobFutureTaskTimer(MediaMosaJobContext context) {
            this.context = context;
        }

        public void run() {
            HippoMediaMosaResourceManager resourceManager = (HippoMediaMosaResourceManager) context.getResourceManager();
            try {
                String stateType = resourceManager.getMediaMosaService().getJobStatus(context.getJobId(), "Hippo").getStatus();
                if (!done) {
                    Iterator<MediaMosaJobListener> it = context.iterator();

                    while (it.hasNext()) {
                        MediaMosaJobListener listener = it.next();
                        switch (MediaMosaJobState.getType(stateType)) {
                            case FINISHED:
                                listener.onFinished(context.assetId);
                                done = true;
                                break;
                            case INPROGRESS:
                                listener.whileInprogress(context.assetId);
                                break;
                            case CANCELLED:
                                done = true;
                                break;
                            case FAILED:
                                done = true;
                                break;
                            case WAITING:
                                listener.whileWaiting(context.assetId);
                                break;
                            default:
                                done = true;
                                break;
                        }
                    }
                }
                if (done) {
                    log.info("job killed {}", cancel());
                }
            } catch (IOException e) {
                log.error("", e);
                cancel();
            } catch (ServiceException e) {
                cancel();
                log.error("", e);
            }
        }
    }


    private class JobFutureTask implements Runnable {

        MediaMosaJobContext context;
        private boolean done;

        private JobFutureTask(MediaMosaJobContext context) {
            this.context = context;
        }

        public void run() {
            HippoMediaMosaResourceManager resourceManager = (HippoMediaMosaResourceManager) context.getResourceManager();
            try {
                String stateType = resourceManager.getMediaMosaService().getJobStatus(context.getJobId(), "Hippo").getStatus();
                if (!done) {
                    Iterator<MediaMosaJobListener> it = context.iterator();

                    while (it.hasNext()) {
                        MediaMosaJobListener listener = it.next();
                        switch (MediaMosaJobState.getType(stateType)) {
                            case FINISHED:
                                listener.onFinished(context.assetId);
                                done = true;
                                break;
                            case INPROGRESS:
                                listener.whileInprogress(context.assetId);
                                break;
                            case CANCELLED:
                                done = true;
                                break;
                            case FAILED:
                                done = true;
                                break;
                            case WAITING:
                                listener.whileWaiting(context.assetId);
                                break;
                            default:
                                done = true;
                                break;
                        }
                    }
                }
                if (done) {
                    log.info("job killed {}", kill(context.getJobId()));
                }
            } catch (IOException e) {
                log.error("", e);
                kill(context.getJobId());
            } catch (ServiceException e) {
                log.error("", e);
                kill(context.getJobId());
            }
        }
    }


    private class MediaMosaJobContext {
        private List<MediaMosaJobListener> listenerList;
        private String jobId;
        private String assetId;
        private ResourceManager resourceManager;

        public MediaMosaJobContext(ResourceManager resourceManager, String jobId, List<MediaMosaJobListener> listenerList) {
            this.resourceManager = resourceManager;
            this.jobId = jobId;
            this.listenerList = listenerList;
        }

        public String getAssetId() {
            return assetId;
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        public Iterator<MediaMosaJobListener> iterator() {
            return listenerList.iterator();
        }

        public MediaMosaJobContext() {
        }

        public boolean add(MediaMosaJobListener mediaMosaJobListener) {
            if (listenerList == null) {
                listenerList = new ArrayList<MediaMosaJobListener>();
            }
            return listenerList.add(mediaMosaJobListener);
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public ResourceManager getResourceManager() {
            return resourceManager;
        }

        public void setResourceManager(ResourceManager resourceManager) {
            this.resourceManager = resourceManager;
        }
    }
}

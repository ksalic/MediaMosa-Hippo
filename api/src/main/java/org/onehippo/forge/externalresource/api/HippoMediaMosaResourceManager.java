package org.onehippo.forge.externalresource.api;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobContext;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobListener;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaJobScheduler;
import org.onehippo.forge.externalresource.api.scheduale.mediamosa.MediaMosaThumbnailJob;
import org.onehippo.forge.externalresource.api.scheduale.synchronization.SynchronizationExecutorJob;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.utils.Utils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * @version $Id:
 */
public class HippoMediaMosaResourceManager extends ResourceManager implements Embeddable, Synchronizable {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoMediaMosaResourceManager.class);

    private String url;
    private String username;
    private String password;
    private String responseType;
    private int width;
    private boolean createThumbnail;
    private final MediaMosaService mediaMosaService;
    private CacheManager singletonManager = CacheManager.create();
    private static final String EMBED_CACHE = "EMBEDDED_CACHING_ENGINE";
    private static final int EMBEDED_CACHE_SIZE = 500;
    private static final int TIME_TO_LIVE = 30;
    public static final String queryString = "content/videos//element(*,hippomediamosa:resource)[@hippomediamosa:assetid='%s']";

    private static final String MASS_SYNC_JOB = "MediaMosaMassSyncJob";
    private static final String MASS_SYNC_JOB_TRIGGER = MASS_SYNC_JOB + "Trigger";
    private static final String MASS_SYNC_JOB_TRIGGER_GROUP = MASS_SYNC_JOB_TRIGGER + "Group";
    private static final String MASS_SYNC_JOB_GROUP = MASS_SYNC_JOB + "Group";

    private final static Map<String, String> map = new HashMap<String, String>();

    static {
        map.put("hippomediamosa:title", "title");
        map.put("hippomediamosa:description", "description");
    }

    public HippoMediaMosaResourceManager(IPluginConfig config, ResourceInvocationType type) {
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
        if (config.containsKey("responseType")) {
            this.responseType = config.getString("responseType");
        }
        if (config.containsKey("width")) {
            this.width = config.getInt("width", 320);
        }
        if (config.containsKey("createThumbnail")) {
            this.createThumbnail = config.getBoolean("createThumbnail");
        }
        this.mediaMosaService = new MediaMosaService(getUrl());
        try {
            this.mediaMosaService.setCredentials(getUsername(), getPassword());
        } catch (ServiceException e) {
            log.error("Service exception on authenticating media mosa credentials", e);
        }

        /*todo can make cache configurable */
        if (!singletonManager.cacheExists(EMBED_CACHE)) {
            Cache cache = new Cache(
                    new CacheConfiguration(EMBED_CACHE, EMBEDED_CACHE_SIZE)
                            .overflowToDisk(false)
                            .eternal(false)
                            .timeToLiveSeconds(TIME_TO_LIVE)
                            .timeToIdleSeconds(TIME_TO_LIVE)
            );
            singletonManager.addCache(cache);
            log.info("creating cache 'EMBEDDED_CACHING_ENGINE' : {}", cache);
        }

    }

    @Override
    public void initSitePlugin() {
    }

    @Override
    public void initCmsPlugin() {
        super.initCmsPlugin();
        try {
            if (getPluginConfig().getAsBoolean("synchronization.enabled", false)) {
                if (getPluginConfig().containsKey("synchronization.cronexpression")) {
                    JobDataMap dataMap = new JobDataMap();
                    dataMap.put("resourcemanager", this);
                    dataMap.put("synchronizable", this);
                    JobDetail jobDetail = new JobDetail(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP, SynchronizationExecutorJob.class);
                    CronTrigger trigger = new CronTrigger(MASS_SYNC_JOB_TRIGGER, MASS_SYNC_JOB_TRIGGER_GROUP, MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP, getPluginConfig().getString("synchronization.cronexpression"));
                    jobDetail.setJobDataMap(dataMap);
                    if (triggerExists(trigger)) {
                        if (triggerChanged(trigger)) {
                            resourceScheduler.rescheduleJob(MASS_SYNC_JOB_TRIGGER, MASS_SYNC_JOB_TRIGGER_GROUP, trigger);
                        }
                    } else {
                        resourceScheduler.scheduleJob(jobDetail, trigger);
                    }
                }
            } else {
                resourceScheduler.unscheduleJob(MASS_SYNC_JOB_TRIGGER, MASS_SYNC_JOB_TRIGGER_GROUP);
            }
        } catch (ParseException e) {
            log.error("", e);
        } catch (SchedulerException e) {
            log.error("", e);
        }
    }


    public MediaMosaService getMediaMosaService() {
        return mediaMosaService;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {
        try {

            String userId = getUsername();

            String assetId = mediaMosaService.createAsset(userId);
            String mediaFile = mediaMosaService.createMediafile(assetId, userId);

            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);

            String uploadUrl = uploadTicketType.getAction();

            int code = submitFile(istream, uploadUrl, mimetype, node.getName());
            //create image
            node.setProperty("hippomediamosa:assetid", assetId);
            node.setProperty("hippomediamosa:mediaid", mediaFile);

            AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);

            MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);

            int size = mediafileDetails.getMetadata().getFilesize();
            int height = mediafileDetails.getMetadata().getHeight();
            int width = mediafileDetails.getMetadata().getWidth();

            node.setProperty("hippoexternal:size", size);
            node.setProperty("hippoexternal:height", height);
            node.setProperty("hippoexternal:width", width);

            //node.setProperty("hippoexternal:state", "inprogress");
            log.debug(assetDetails.getVideotimestampmodified().toString());
            log.debug(assetDetails.getVideotimestamp().toString());

            Calendar modified = assetDetails.getVideotimestampmodified();
            node.setProperty("hippoexternal:lastModifiedSyncDate", modified);

            LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), this.width);

            Utils.addEmbeddedNode(node, embedLink.getOutput());

            Map map = new HashMap();
            map.put("still_type", "NORMAL");
            map.put("still_per_mediafile", 6);

            JobType job = mediaMosaService.createStill(assetId, mediaFile, getUsername(), map);

            MediaMosaJobListener listener = new MediaMosaJobListener() {
                public void whileInprogress(String assetId) {
                }

                public void onFinished(String assetId) {
                    JobDataMap dataMap = new JobDataMap();
                    dataMap.put("assetId", assetId);
                    dataMap.put("resourceManager", HippoMediaMosaResourceManager.this);
                    scheduleNowOnce(MediaMosaThumbnailJob.class, dataMap);
                }

                public void whileWaiting(String assetId) {
                }

                public void onFailed(String assetId) {
                }

                public void onCancelled(String assetId) {
                }
            };

            MediaMosaJobContext context = new MediaMosaJobContext();
            context.add(listener);
            context.setResourceManager(this);
            context.setJobId(String.valueOf(job.getJobId()));
            context.setAssetId(assetId);

            MediaMosaJobScheduler.getInstance().offer(context);
            log.debug("trying to request still creation");
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @Override
    public void afterSave(Node node) {
        log.debug("starting aftersave");
        log.debug("done with aftersave");
    }

    @Override
    public void delete(Node node) {
        try {
            mediaMosaService.deleteAsset(node.getProperty("hippomediamosa:assetid").getString(), getUsername(), true);
            log.debug("deleting asset");
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public String getEmbedded(Node node) {
        String embedded = "<p>something happened, can't show video</p>";
        try {
            if (node.hasProperty("hippomediamosa:assetid")) {
                String assetId = node.getProperty("hippomediamosa:assetid").getString();
                String cache = cacheRetrieve(assetId);
                if (cache == null) {
                    AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                    MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
                    LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), this.width);
                    if (embedLink != null) {
                        embedded = embedLink.getOutput();
                        cacheStore(assetId, embedded);
                    }
                } else {
                    embedded = cache;
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        } catch (ServiceException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        } catch (IOException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        }
        return embedded;
    }

    private String cacheRetrieve(String assetId) {
        log.info("trying to retrieving from cache with assetId: {}", assetId);
        Cache cache = singletonManager.getCache(EMBED_CACHE);
        Element element = cache.get(assetId);
        if (element == null) {
            log.info("trying failed with assetId: {} .. return null", assetId);
            return null;
        } else {
            log.info("trying succeeded to retrieving from cache with assetId: {}", assetId);
            return (String) element.getObjectValue();
        }
    }

    private void cacheStore(String assetId, String embeddedCode) {
        log.info("storing to cache with assetId: {} and embedded code : {}", assetId, embeddedCode);
        Cache cache = singletonManager.getCache(EMBED_CACHE);
        Element element = new Element(assetId, embeddedCode);
        cache.put(element);
    }


    public static int submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        int statusCode = response.getStatusLine().getStatusCode();
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return statusCode;
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

    public boolean update(Node node) {
        InputStream is = null;
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            Calendar external = assetDetailsType.getVideotimestampmodified();

            MediafileDetailsType mediafileDetails = assetDetailsType.getMediafiles().getMediafile().get(0);
            node.setProperty("hippomediamosa:mediaid", mediafileDetails.getMediafileId());
            node.setProperty("hippoexternal:title", mediafileDetails.getMediafileId());
            node.setProperty("hippoexternal:width", mediafileDetails.getMetadata().getWidth());
            node.setProperty("hippoexternal:height", mediafileDetails.getMetadata().getHeight());
            node.setProperty("hippoexternal:mimeType", mediafileDetails.getMetadata().getMimeType());
            node.setProperty("hippoexternal:size", mediafileDetails.getMetadata().getFilesize());

            node.setProperty("hippoexternal:title", assetDetailsType.getDublinCore().getTitle());
            node.setProperty("hippoexternal:description", assetDetailsType.getDublinCore().getDescription());
            node.setProperty("hippoexternal:lastModified", external);

            if (assetDetailsType.getMediafileDuration() != null) {
                node.setProperty("hippoexternal:duration", assetDetailsType.getMediafileDuration().toXMLFormat());
            }

            node.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
            node.setProperty("hippoexternal:lastModifiedSyncDate", external);

            if (StringUtils.isNotEmpty(assetDetailsType.getVpxStillUrl())) {
                String url = assetDetailsType.getVpxStillUrl();
                //Utils.resolveThumbnailToVideoNode(url, node);
                org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
                HttpMethod getMethod = new GetMethod(url);
                client.executeMethod(getMethod);
                String mimeType = getMethod.getResponseHeader("content-type").getValue();
                if (!mimeType.startsWith("image")) {
                    log.error("Illegal mimetype used: {}", mimeType);
                    throw new IllegalArgumentException();
                }

                is = getMethod.getResponseBodyAsStream();
                if (node.hasNode("hippoexternal:thumbnail")) {
                    Node thumbnail = node.getNode("hippoexternal:thumbnail");
                    thumbnail.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(is));
                    thumbnail.setProperty("jcr:mimeType", mimeType);
                    thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());
                }
            }
            node.getSession().save();
            return true;
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return false;
    }

    public boolean commit(Node node) {
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            Map map = new HashMap();
            map.put("title", node.getProperty("hippoexternal:title").getString());
            map.put("description", node.getProperty("hippoexternal:description").getString());
            //map.put("action", "replace");
            try {
                Response r = mediaMosaService.setMetadata(assetId, getUsername(), map);
                log.debug(r.getHeader().getRequestResult());

                //lastsyncdate up!
                AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                Calendar modified = assetDetails.getVideotimestampmodified();
                node.setProperty("hippoexternal:lastModifiedSyncDate", modified);
                node.getSession().save();
                return true;
            } catch (ServiceException e) {
                log.error("", e);
            } catch (IOException e) {
                log.error("", e);
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    public SynchronizationState check(Node node) {
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            SynchronizationState currentState = SynchronizationState.getType(node.getProperty("hippoexternal:state").getString());
            if (assetDetailsType != null) {
                Calendar external = assetDetailsType.getVideotimestampmodified();
                Calendar local = null;
                if (node.hasProperty("hippoexternal:lastModifiedSyncDate")) {
                    local = node.getProperty("hippoexternal:lastModifiedSyncDate").getDate();
                }
                if (local.getTime().equals(external.getTime())) {
                    log.debug("check is correct");
                    if (!currentState.equals(SynchronizationState.SYNCHRONIZED)) {
                        node.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
                        node.getSession().save();
                    }
                    return SynchronizationState.SYNCHRONIZED;
                } else {
                    if (!currentState.equals(SynchronizationState.UNSYNCHRONIZED)) {
                        node.setProperty("hippoexternal:state", SynchronizationState.UNSYNCHRONIZED.getState());
                        node.getSession().save();
                    }
                    log.debug("check is not correct");
                    return SynchronizationState.UNSYNCHRONIZED;
                }
            } else {
                if (!currentState.equals(SynchronizationState.BROKEN)) {
                    node.setProperty("hippoexternal:state", SynchronizationState.BROKEN.getState());
                    node.getSession().save();
                }
                return SynchronizationState.BROKEN;
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return SynchronizationState.UNKNOWN;
    }


}

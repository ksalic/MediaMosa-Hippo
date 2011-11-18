import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.impl.MediaMosaImpl;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;


/**
 * @version $Id$
 */
@Ignore(value = "no external stuff for tests")
public class MediaMosaTest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaTest.class);

    MediaMosaService mediaMosaService;

    /*@Before
    public void setUp() throws Exception {
        mediaMosaService = new MediaMosaService("http://10.10.100.164/mediamosa");
        try {
            mediaMosaService.setCredentials("Hippo", "VERUnK2fWlc0F3IceA21awRj");
        } catch (ServiceException e) {
            log.error("e {}", e);
        }
    }*/

    @Before
    public void setUp() throws Exception {
        mediaMosaService = new MediaMosaService("http://10.10.100.164/mediamosa");
        try {
            mediaMosaService.setCredentials("Hippo", "VERttUnK2fWlc0F3IceA21awRj");
        } catch (ServiceException e) {
            log.error("e {}", e);
        }
    }

    @Test
    public void testInstanceTest() throws Exception {
       // System.out.println(mediaMosaService.getVersion());
        System.out.println(mediaMosaService.doGetRequestString("/statistics/playedstreams?month=11&year=2011"));
    }

    @Test
    public void testUpload() throws Exception {
        System.out.println(mediaMosaService.doGetRequestString("/user"));
    }

    private String userId = "wle";
    private String password = "VERttUnK2fWlc0F3IceA21awRj";
    private String url = "http://10.10.100.164/mediamosa";

    public MediafileDetailsType getMediaFile(AssetDetailsType assetDetailsType) {
        return assetDetailsType.getMediafiles().getMediafile().get(0);
    }

    @Test
    public void testChangeOfAsset() throws Exception {
        Map map = new HashMap();
        map.put("title", "Andere titel");
        mediaMosaService.setMetadata("ZM4T7zQBRXmSqiZf8HPNZhdo", "Hippo", map);
        //mediaMosaService.setMetadata("55e6ThPTXgwvcCPngDFzt3ae", "Hippo", map);
    }

    @Test
    public void testAlterMethod() throws Exception {
        Map map = new HashMap();
        map.put("title", "Test Titel");
        map.put("description", "Description");
        map.put("type", "typevideo");
        map.put("contributor", "contributor");
        //map.put("action", "append");
        mediaMosaService.setMetadata("Yie9OYM8Ld6QEMLvDlsStbqy", "Hippo", map);

        AssetDetailsType detail = mediaMosaService.getAssetDetails("Yie9OYM8Ld6QEMLvDlsStbqy");

        detail.getDublinCore().getTitle();
    }

    private CacheManager singletonManager = CacheManager.create();
    private static final String EMBED_CACHE = "EMBEDDED_CACHING_ENGINE";
    private static final int EMBEDED_CACHE_SIZE = 500;
    private static final int TIME_TO_LIVE = 30;

    @Test
    public void testCacheTest() throws Exception {

        if (!singletonManager.cacheExists(EMBED_CACHE)) {
            Cache cache = new Cache(
                    new CacheConfiguration(EMBED_CACHE, EMBEDED_CACHE_SIZE)
                            .overflowToDisk(false)
                            .eternal(false)
                            .timeToLiveSeconds(TIME_TO_LIVE)
                            //.timeToIdleSeconds(TIME_TO_LIVE)
            );
            singletonManager.addCache(cache);
            log.info("creating cache 'EMBEDDED_CACHING_ENGINE' : {}", cache);
        }


        cacheStore("123", "abc");

        Thread.sleep(10000);

        assertTrue(cacheRetrieve("123").equals("abc"));

        Thread.sleep(10000);

        assertTrue(cacheRetrieve("123").equals("abc"));

        Thread.sleep(11000);

        assertNull(cacheRetrieve("123"));

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

    @Test
    public void testList() throws Exception {
        mediaMosaService.getAssets();
    }

    @Test
    public void testPlayedStrems() throws Exception {
        List<StatsPlayedstreamsType> list = mediaMosaService.getStatsPlayedStreams(2011, 11, "object", null, null, 200, 0);
    }

    @Test
    public void testFirst8() throws Exception {

        List<Integer> integerList = new ArrayList<Integer>();
        integerList.add(4);


        int i = 0;
        for (AssetType assetType : mediaMosaService.getAssets()) {
            if (integerList.contains(i)) {
                System.out.println(i);
                mediaMosaService.deleteAsset(assetType.getAssetId(), "Hippo", true);
                integerList.remove(new Integer(i));
            }
            if (integerList.isEmpty()) {
                break;
            }
            System.out.println(i);
            i++;
        }

        System.out.println("done");
    }

    @Test
    public void testAuthorization() throws Exception {
        System.out.println(mediaMosaService.doGetRequestString("/autorisation_group?limit=200"));

    }

    @Test
    public void testStillUploadTicket() throws Exception {

        //case 1 create asset and mediafile id and create uploadticket .
        //case 2 using existing media and assetid.
        //case 3: mashup, use existing assetid and create mediafileid
        //String assetId = mediaMosaService.createAsset(userId);


        AssetType assetType = mediaMosaService.getAssets().get(5);

        String assetId = assetType.getAssetId();
        AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetType.getAssetId());

        //String mediaFile2 = mediaMosaService.createMediafile(assetType.getAssetId(), userId);
        //String assetId = assetDetailsType.getAssetId();
        String mediaFile = assetDetailsType.getMediafiles().getMediafile().get(0).getMediafileId();


        StillType stills = mediaMosaService.getStills(assetId, userId, null);
        int amount = stills.getStills().size();


        UploadTicketType ticket = mediaMosaService.createUploadTicket(mediaFile, userId, true);
        String action = ticket.getAction();
        //String uploadTicket = action.substring(action.indexOf("?"));


        String url = String.format("/asset/%s/still/upload", assetId);

        String uploadTicket = action.substring(action.indexOf("?") + 1);
        String serverURL = url;
        System.out.println("*****************" + serverURL);

        FileInputStream inputStream = new FileInputStream("C:\\Users\\ksalic\\Pictures\\Untitled.png");

        MediaMosaImpl impl = new MediaMosaImpl();
        impl.setHostname(this.url);
        impl.setCredentials(userId, password);
        impl.login();
        impl.doPostRequest(serverURL, uploadTicket + "&mediafile_id=" + mediaFile, inputStream, "image/png", "Untitled.png");


        StillType stills2 = mediaMosaService.getStills(assetId, userId, null);

        int amountIncremented = stills2.getStills().size();

        System.out.println(amount + "-" + amountIncremented);
    }

    @Test
    public void testScanner() throws Exception {
        String postParams = "&mediafile_id=8F6tfCJRMMBQWScDVeoua52N&upload_ticket=upload_ticket=6QVqn1AsRqGEQ0RsazkFTrcc";
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        URLEncodedUtils.parse(nvps, new Scanner(postParams), HTTP.UTF_8);

        System.out.println(nvps);
    }

    public int submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName, String parameters) throws Exception {


        HttpClient httpclient = new DefaultHttpClient();

        HttpPost httppost = new HttpPost(serverUrl);

        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        URLEncodedUtils.parse(nvps, new Scanner(parameters), HTTP.UTF_8);

        mpEntity.addPart("file", cbFile);

        for (NameValuePair nameValuePair : nvps) {
            mpEntity.addPart(new FormBodyPart(nameValuePair.getName(), new StringBody(nameValuePair.getValue())));
        }

        httppost.setEntity(mpEntity);

        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        System.out.println("**************");
        System.out.println(convertStreamToString(resEntity.getContent()));
        System.out.println("**************");

        int statusCode = response.getStatusLine().getStatusCode();
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return statusCode;
    }

    public static String convertStreamToString(InputStream is)
            throws IOException {
        /*
         * To convert the InputStream to String we use the
         * Reader.read(char[] buffer) method. We iterate until the
         * Reader return -1 which means there's no more data to
         * read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    @Test
    public void testDataUsageStats() throws Exception {
        // List<StatsDatausagevideoType> list = mediaMosaService.getStatsDatausageVideo(2011, 10, "container", 200, 0);

        //System.out.println(list);

        List<StatsDatausagevideoType> list = mediaMosaService.getStatsDatausageVideo(2011, 10, "group", 200, 0);

        list = mediaMosaService.getStatsDatausageVideo(2011, 10, "user", 200, 0);
        System.out.println(list);
    }

    @Test
    public void testStatistics() throws Exception {
        List<StatsPopularstreamsType> list = mediaMosaService.getStatsPopularStreams(200, 0);
        System.out.println(mediaMosaService.doGetRequestString("/statistics/popularmediafiles"));


    }

    @Test
    public void testModifyData() throws Exception {
        Map map = new HashMap();
        map.put("title", "test1********dfsdfsdfsdf NEWNENWNENW");

        System.out.println("bla");

        //mediaMosaService.createMediaFileStill()
        //mediaMosaService.getAssets();
        List<AssetType> lsit = mediaMosaService.getAssets();
        int i = 0;
        for (AssetType asset : lsit) {
            System.out.println(asset.getAssetId());
            //mediaMosaService.deleteAsset(asset.getAssetId(), "Hippo", true);
            //mediaMosaService.getAssetDetails(asset.getAssetId())
            mediaMosaService.setMetadata(asset.getAssetId(), "Hippo", map);
            //mediaMosaService.getAssetDetails(asset.getAssetId()).get
            i++;
            if (i == 4)
                break;

        }
    }

    @Test
    public void stillCreationTest() {
        try {
            List<AssetType> lsit = mediaMosaService.getAssets();

            Map map = new HashMap();
            map.put("still_type", "normal");
            map.put("still_per_mediafile", 6);
            //map.put("start_time", 1);
            //map.put("end_time", 8);

            JobType job = mediaMosaService.createStill(lsit.get(3).getAssetId(), mediaMosaService.getAssetDetails(lsit.get(3).getAssetId()).getMediafiles().getMediafile().get(0).getMediafileId(), userId, map);

            while (true) {
                JobDetailsType jobDetailsType = mediaMosaService.getJobStatus(String.valueOf(job.getJobId()), userId);
                System.out.println(jobDetailsType.getProgress());
                System.out.println(jobDetailsType.getStatus());
                if (jobDetailsType.getProgress() == 1.0) {
                    break;
                }
                Thread.sleep(4000);
            }

            StillType list = mediaMosaService.getStills(lsit.get(3).getAssetId(), userId, null);

            for (StillDetailType detail : list.getStills()) {
                System.out.println(detail.getStillTicket() + " " + detail.isStillDefault() + " " + detail.getFilesize());
            }
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }


    public String requestForVideo() {
        try {
            String assetId = mediaMosaService.createAsset(userId);

            String mediaFile = mediaMosaService.createMediafile(assetId, userId);

            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);

            String uploadUrl = uploadTicketType.getAction();
            return uploadUrl;
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    public String requestUrlForStill(String mediaFile) {
        try {
            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);
            String uploadUrl = uploadTicketType.getAction();
            return uploadUrl;
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    public int submitFile(final String serverUrl, final InputStream inputStream, String mimeType, String fileName) throws Exception {
        //String serverUrl = requestForVideo();

        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);
        //mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        int statusCode = response.getStatusLine().getStatusCode();
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return statusCode;
    }


}

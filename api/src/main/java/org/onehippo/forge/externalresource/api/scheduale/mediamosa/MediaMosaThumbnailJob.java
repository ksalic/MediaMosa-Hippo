package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.quartz.JCRSchedulingContext;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.scheduale.ExternalResourceSchedular;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.io.IOException;
import java.io.InputStream;

/**
 * @version $Id$
 */
public class MediaMosaThumbnailJob implements Job {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaThumbnailJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDetail jobDetail = context.getJobDetail();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            ExternalResourceSchedular scheduler = (ExternalResourceSchedular) context.getScheduler();
            Session session = ((JCRSchedulingContext) scheduler.getCtx()).getSession();
            synchronized (session) {
                session.refresh(false);

                String assetId = (String) jobDataMap.get("assetId");
                HippoMediaMosaResourceManager resourceManager = (HippoMediaMosaResourceManager) jobDataMap.get("resourceManager");

                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(String.format(HippoMediaMosaResourceManager.queryString, assetId), "xpath");
                NodeIterator it = query.execute().getNodes();
                while (it.hasNext()) {
                    Node mediamosaAsset = it.nextNode();
                    AssetDetailsType detail = resourceManager.getMediaMosaService().getAssetDetails(assetId);
                    if (StringUtils.isNotBlank(detail.getVpxStillUrl())) {
                        String imageUrl = detail.getVpxStillUrl();
                        //Utils.resolveThumbnailToVideoNode(imageUrl, mediamosaAsset);
                        org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
                        HttpMethod getMethod = new GetMethod(imageUrl);
                        InputStream is = null;
                        try {
                            client.executeMethod(getMethod);
                            is = getMethod.getResponseBodyAsStream();
                            String mimeType = getMethod.getResponseHeader("content-type").getValue();
                            if (mimeType.startsWith("image")) {
                                if (mediamosaAsset.hasNode("hippoexternal:thumbnail")) {
                                    Node thumbnail = mediamosaAsset.getNode("hippoexternal:thumbnail");
                                    thumbnail.setProperty("jcr:data", session.getValueFactory().createBinary(is));
                                    thumbnail.setProperty("jcr:mimeType", mimeType);
                                    thumbnail.setProperty("jcr:lastModified", java.util.Calendar.getInstance());
                                   // mediamosaAsset.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
                                    session.save();
                                }
                            }
                        } catch (IOException e) {
                            log.error("", e);
                        } finally {
                            IOUtils.closeQuietly(is);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            throw new JobExecutionException(ex.getClass().getName() + ": " + ex.getMessage());
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }
}

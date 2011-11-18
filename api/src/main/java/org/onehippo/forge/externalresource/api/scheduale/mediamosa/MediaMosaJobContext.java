package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

import org.onehippo.forge.externalresource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @version $Id$
 */
public class MediaMosaJobContext {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaJobContext.class);

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

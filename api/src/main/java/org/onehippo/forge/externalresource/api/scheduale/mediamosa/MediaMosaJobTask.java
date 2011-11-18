package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

import nl.uva.mediamosa.util.ServiceException;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * @version $Id$
 */
public class MediaMosaJobTask implements Runnable {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaJobTask.class);

    private MediaMosaJobContext context;
    private boolean done;

    public MediaMosaJobTask(MediaMosaJobContext context) {
        this.context = context;
    }

    public void run() {
        HippoMediaMosaResourceManager resourceManager = (HippoMediaMosaResourceManager) context.getResourceManager();
        try {
            String stateType = resourceManager.getMediaMosaService().getJobStatus(context.getJobId(), resourceManager.getUsername()).getStatus();
            if (!done) {
                Iterator<MediaMosaJobListener> it = context.iterator();

                while (it.hasNext()) {
                    MediaMosaJobListener listener = it.next();
                    switch (MediaMosaJobState.getType(stateType)) {
                        case FINISHED:
                            listener.onFinished(context.getAssetId());
                            done = true;
                            break;
                        case INPROGRESS:
                            listener.whileInprogress(context.getAssetId());
                            break;
                        case CANCELLED:
                            listener.onCancelled(context.getAssetId());
                            done = true;
                            break;
                        case FAILED:
                            listener.onFailed(context.getAssetId());
                            done = true;
                            break;
                        case WAITING:
                            listener.whileWaiting(context.getAssetId());
                            break;
                        default:
                            done = true;
                            break;
                    }
                }
            }
            if (done) {
                log.info("job killed {}", MediaMosaJobScheduler.getInstance().kill(context.getJobId()));
            }
        } catch (IOException e) {
            log.error("", e);
            MediaMosaJobScheduler.getInstance().kill(context.getJobId());
        } catch (ServiceException e) {
            log.error("", e);
            MediaMosaJobScheduler.getInstance().kill(context.getJobId());
        }
        log.info("Just ran for {}", context.getJobId());
    }


}


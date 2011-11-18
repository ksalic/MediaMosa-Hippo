package org.onehippo.forge.externalresource.api.scheduale.mediamosa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @version $Id$
 */
public class MediaMosaJobScheduler {

    private static Logger log = LoggerFactory.getLogger(MediaMosaJobScheduler.class);

    private static MediaMosaJobScheduler ourInstance = new MediaMosaJobScheduler();

    private final static ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(3);

    private Map<String, ScheduledFuture> map = new ConcurrentHashMap<String, ScheduledFuture>();

    public static MediaMosaJobScheduler getInstance() {
        return ourInstance;
    }

    private MediaMosaJobScheduler() {
    }

    public synchronized void offer(MediaMosaJobContext context) {
        MediaMosaJobTask jobtask = new MediaMosaJobTask(context);
        map.put(context.getJobId(), scheduler.scheduleAtFixedRate(jobtask, 10, 10, TimeUnit.SECONDS));
    }

    public synchronized boolean kill(String jobId) {
        ScheduledFuture future = map.remove(jobId);
        log.info("contains {}", map.containsKey(jobId));
        return future.cancel(true);
    }

}

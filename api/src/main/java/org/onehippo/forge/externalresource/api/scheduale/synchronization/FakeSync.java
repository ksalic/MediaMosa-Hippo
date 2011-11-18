package org.onehippo.forge.externalresource.api.scheduale.synchronization;

import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @version $Id$
 */
public class FakeSync implements Job {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(FakeSync.class);


    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDetail jobDetail = context.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        String uuid = (String) jobDataMap.get("identifier");

        SynchronizationState state = getRandomSyncState();

        switch (state) {
            case UNSYNCHRONIZED:
                System.out.println(state.getState() + uuid);
                break;
            case BROKEN:
                System.out.println(state.getState() + uuid);
                break;
            default:
                System.out.println(state.getState() + uuid);
                break;
        }

    }

    public SynchronizationState getRandomSyncState() {
        int pick = new Random().nextInt(SynchronizationState.values().length);
        return SynchronizationState.values()[pick];
    }
}

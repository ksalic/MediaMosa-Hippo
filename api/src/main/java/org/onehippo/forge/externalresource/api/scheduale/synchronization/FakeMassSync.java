package org.onehippo.forge.externalresource.api.scheduale.synchronization;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @version $Id$
 */
public class FakeMassSync implements Job {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(FakeMassSync.class);

    public static final String FakeSyncJob = "FakeSyncJob";
    public static final String FakeSyncGroup = "FakeSyncGroup";


    public void execute(JobExecutionContext context) throws JobExecutionException {
        Scheduler scheduler = context.getScheduler();
        System.out.println("starting mass sync");


        for (int i = 0; i < 10; i++) {
            try {
                JobDataMap dataMap = new JobDataMap();
                String id = UUID.randomUUID().toString();
                dataMap.put("identifier", id);
                JobDetail jobDetail = new JobDetail(FakeSyncJob + id, FakeSyncGroup, FakeSync.class);
                jobDetail.setJobDataMap(dataMap);
                System.out.println(String.format("schualing - #%s - id:%s", String.valueOf(i), id));
                Trigger trigger = createImmediateTrigger();
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                log.error("", e);
            }
        }

        System.out.println("ending mass sync");


    }


    Trigger createImmediateTrigger() {
        Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 1);
        trigger.setName(UUID.randomUUID().toString());
        return trigger;
    }


}

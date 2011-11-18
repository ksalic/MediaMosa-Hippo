package org.onehippo.forge.externalresource.api;

import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.externalresource.api.utils.ExresDaemonModule;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.io.InputStream;
import java.util.UUID;

/**
 * @version $Id$
 */
abstract public class ResourceManager extends Plugin {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ResourceManager.class);

    protected String type;
    protected Scheduler resourceScheduler;

    public ResourceManager(IPluginConfig config, ResourceInvocationType invocationType) {
        super(null, config);
        if (config.containsKey("type")) {
            this.type = config.getString("type");
        }
        switch (invocationType) {
            case CMS:
                initCmsPlugin();
                break;
            case SITE:
                initSitePlugin();
                break;
            default:
                break;
        }
    }

    public void initSitePlugin() {
    }

    public void initCmsPlugin() {
        initScheduler();
    }

    protected void initScheduler() {
        this.resourceScheduler = ExresDaemonModule.getScheduler();
    }

    protected Scheduler getResourceScheduler() {
        return resourceScheduler;
    }

    static Trigger createImmediateTrigger() {
        Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 1);
        trigger.setName(UUID.randomUUID().toString());
        return trigger;
    }

    static JobDetail createImmediateJobDetail(Class<? extends Job> jobClass) {
        return new JobDetail(UUID.randomUUID().toString(), jobClass);
    }

    public void scheduleNowOnce(Class<? extends Job> jobClass, JobDataMap dataMap) {
        JobDetail jobDetail = createImmediateJobDetail(jobClass);
        jobDetail.setJobDataMap(dataMap);
        try {
            resourceScheduler.scheduleJob(jobDetail, createImmediateTrigger());
        } catch (SchedulerException e) {
            log.error("",e);
        }
    }

    protected boolean triggerChanged(Trigger checkTrigger) {
        try {
            Trigger trigger = resourceScheduler.getTrigger(checkTrigger.getName(), checkTrigger.getGroup());
            if (trigger != null && trigger instanceof CronTrigger) {
                return !((CronTrigger) trigger).getCronExpression().equals(((CronTrigger) checkTrigger).getCronExpression());
            }
        } catch (SchedulerException e) {
            log.error("", e);
        }
        return false;
    }

    protected boolean triggerExists(Trigger checkTrigger) {
        try {
            Trigger trigger = resourceScheduler.getTrigger(checkTrigger.getName(), checkTrigger.getGroup());
            return (trigger != null);
        } catch (SchedulerException e) {
            log.error("", e);
        }
        return false;
    }

    abstract public void create(Node node, InputStream istream, String mimetype) throws Exception;

    abstract public void afterSave(Node node);

    abstract public void delete(Node node);
}

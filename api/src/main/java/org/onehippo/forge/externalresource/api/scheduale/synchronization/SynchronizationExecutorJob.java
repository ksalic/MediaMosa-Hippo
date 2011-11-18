package org.onehippo.forge.externalresource.api.scheduale.synchronization;

import org.hippoecm.repository.quartz.JCRSchedulingContext;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.scheduale.ExternalResourceSchedular;
import org.quartz.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * @version $Id$
 */
public class SynchronizationExecutorJob implements Job {

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDetail jobDetail = context.getJobDetail();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
           // String query = (String) jobDataMap.get("synchronization-query"); //"content/documents//element(*,hippoexternal:synchronizable)"
            ResourceManager resourceManager = (ResourceManager) jobDataMap.get("resourcemanager");
            ExternalResourceSchedular scheduler = (ExternalResourceSchedular) context.getScheduler();
            Session session = ((JCRSchedulingContext) scheduler.getCtx()).getSession();

            javax.jcr.NodeIterator it = session.getWorkspace()
                    .getQueryManager()
                    .createQuery("content/videos//element(*,hippoexternal:synchronizable)", Query.XPATH)
                    .execute()
                    .getNodes();

            while (it.hasNext()) {
                Node node = it.nextNode();
                jobDataMap.put("identifier", node.getIdentifier());
                resourceManager.scheduleNowOnce(SynchronizationJob.class, jobDataMap);
            }
            if(jobDataMap.containsKey("listener")){
                SynchronizationListener listener = (SynchronizationListener) jobDataMap.get("listener");
                listener.onFinished();
            }
        } catch (RepositoryException e) {
        }

    }

}

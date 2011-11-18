package org.onehippo.forge.externalresource.api.scheduale.synchronization;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.quartz.JCRSchedulingContext;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.scheduale.ExternalResourceSchedular;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public class SynchronizationJob implements Job {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(SynchronizationJob.class);

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            JobDetail jobDetail = context.getJobDetail();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            String uuid = (String) jobDataMap.get("identifier");
            Synchronizable synchronizable = (Synchronizable) jobDataMap.get("synchronizable");
            ResourceManager resourceManager = (ResourceManager) jobDataMap.get("resourcemanager");
            ExternalResourceSchedular scheduler = (ExternalResourceSchedular) context.getScheduler();
            Session session = ((JCRSchedulingContext) scheduler.getCtx()).getSession();

            Node node = ((HippoNode) session.getNodeByIdentifier(uuid)).getCanonicalNode();
            log.debug(uuid);
            SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow("synchronization", node);
            SynchronizationState state = workflow.check(synchronizable);

            switch (state) {
                case UNSYNCHRONIZED:
                    workflow.update(synchronizable);
                    break;
                case BROKEN:
                    //workflow.delete(resourceManager);
                    break;
                default:
                    break;
            }
        } catch (RepositoryException e) {
        } catch (RemoteException e) {
        } catch (WorkflowException e) {
        }

    }

}

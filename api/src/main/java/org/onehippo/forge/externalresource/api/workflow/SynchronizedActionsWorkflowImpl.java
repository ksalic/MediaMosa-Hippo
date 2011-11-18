package org.onehippo.forge.externalresource.api.workflow;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflowImpl;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public class SynchronizedActionsWorkflowImpl extends DefaultWorkflowImpl implements SynchronizedActionsWorkflow {
    @SuppressWarnings({"UnusedDeclaration"})
    //private static Logger log = LoggerFactory.getLogger(SynchronizedActionsWorkflowImpl.class);

    protected String state;

    protected Document document;
    protected Session rootSession;

    public SynchronizedActionsWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RepositoryException {
        super(context, userSession, rootSession, subject);
        this.document = new Document(subject.getUUID());
        this.rootSession = rootSession;
    }


    public SynchronizationState check(Synchronizable synchronizable) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        SynchronizationState synchronizationState  = synchronizable.check(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        this.state = synchronizationState.getState();
        return synchronizationState;
    }

    public Document update(Synchronizable synchronizable) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        synchronizable.update(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        return document;
    }

    public Document commit(Synchronizable synchronizable) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        synchronizable.commit(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        return document;
    }

    public void delete(ResourceManager resourceManager) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        super.delete();
        resourceManager.delete(this.rootSession.getNodeByIdentifier(document.getIdentity()));
    }
}

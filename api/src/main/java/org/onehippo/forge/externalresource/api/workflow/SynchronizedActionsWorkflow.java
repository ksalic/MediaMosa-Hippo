package org.onehippo.forge.externalresource.api.workflow;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

import javax.jcr.RepositoryException;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public interface SynchronizedActionsWorkflow extends DefaultWorkflow, EditableWorkflow {

    public SynchronizationState check(Synchronizable synchronizable)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document update(Synchronizable synchronizable)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public Document commit(Synchronizable synchronizable)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public void delete(ResourceManager manager)
            throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
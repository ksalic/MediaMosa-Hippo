package org.onehippo.forge.externalresource.api;

import org.hippoecm.addon.workflow.StdWorkflow;

import javax.jcr.Node;
import java.util.List;

/**
 * @version $Id$
 */
public interface WorkflowItemManager {

    List<StdWorkflow> processList(List<StdWorkflow> list, Node node);
}

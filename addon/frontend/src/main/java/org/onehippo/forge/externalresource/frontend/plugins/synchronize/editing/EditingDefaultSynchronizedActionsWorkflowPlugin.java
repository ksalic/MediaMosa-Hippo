package org.onehippo.forge.externalresource.frontend.plugins.synchronize.editing;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorFilter;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.Workflow;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.List;

/**
 * @version $Id$
 */
public class EditingDefaultSynchronizedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {


    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingDefaultSynchronizedActionsWorkflowPlugin.class);

    private boolean isValid = true;

    protected ExternalResourceService getExternalResourceService() {
        IPluginContext context = getPluginContext();

        ExternalResourceService service = context.getService(getPluginConfig().getString("external.processor.id",
                "external.processor.service"), ExternalResourceService.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    public EditingDefaultSynchronizedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        IEditor editor = context.getService(getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditor.class);
        context.registerService(new IEditorFilter() {
            public void postClose(Object object) {
                // nothing to do
            }

            public Object preClose() {
                try {
                    ((WorkflowDescriptorModel) getDefaultModel()).getNode().save();
                    return new Object();
                } catch (RepositoryException ex) {
                    log.info(ex.getMessage());
                }
                return null;
            }
        }, context.getReference(editor).getServiceId());

        add(new WorkflowAction("save", new StringResourceModel("save", this, null, "Save").getString(),
                new ResourceReference(EditingDefaultSynchronizedActionsWorkflowPlugin.class, "document-save-16.png")) {
            @Override
            protected String execute(Workflow wf) throws Exception {
                validate();
                if (!isValid()) {
                    return null;
                }
                Node document = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                document.getSession().save();

                Synchronizable synchronizable = getExternalResourceService().getSynchronizableProcessor(document.getPrimaryNodeType().getName());
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.commit(synchronizable);

                return null;
            }
        });

        add(new WorkflowAction("done", new StringResourceModel("done", this, null, "Done").getString(),
                new ResourceReference(getClass(), "document-saveclose-16.png")) {

            @Override
            protected String execute(Workflow wf) throws Exception {
                validate();
                if (!isValid()) {
                    return null;
                }
                Node docNode = ((WorkflowDescriptorModel) EditingDefaultSynchronizedActionsWorkflowPlugin.this.getDefaultModel())
                        .getNode();
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(IEditor.Mode.VIEW);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }

                Node document = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                document.getSession().save();

                Synchronizable synchronizable = getExternalResourceService().getSynchronizableProcessor(document.getPrimaryNodeType().getName());
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.commit(synchronizable);

                return null;
            }
        });
    }

    void validate() throws ValidationException {
        isValid = true;
        List<IValidationService> validators = getPluginContext().getServices(
                getPluginConfig().getString(IValidationService.VALIDATE_ID), IValidationService.class);
        if (validators != null) {
            for (IValidationService validator : validators) {
                validator.validate();
                IValidationResult result = validator.getValidationResult();
                isValid = isValid && result.isValid();
            }
        }
    }

    boolean isValid() {
        return isValid;
    }
}
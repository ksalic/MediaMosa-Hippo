package org.onehippo.forge.externalresource.reports.plugins.synchronization;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.reports.AbstractExtRenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtComponent;
import org.wicketstuff.js.ext.ExtPanel;

/**
 * @version $Id$
 */
public class SynchronizationListPlugin extends AbstractExtRenderPlugin {

    private static final String PROP_QUERY = "query";

    private Logger log = LoggerFactory.getLogger(SynchronizationListPlugin.class);

    private ExtPanel panel;

    public SynchronizationListPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String query = config.getString(PROP_QUERY);
        if (query == null) {
            log.error("Report configuration '{}' is missing the required string property '{}' ",
                    config.getName(), PROP_QUERY);
            return;
        }

        panel = new SynchronizationListPanel(context, config, query);
        add(panel);
    }

    public ExtComponent getExtComponent() {
        return panel;
    }

}
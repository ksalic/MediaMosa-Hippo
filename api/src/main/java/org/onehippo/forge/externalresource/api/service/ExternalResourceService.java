/**
 * Copyright (C) 2010 Hippo B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.externalresource.api.service;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.externalresource.api.Embeddable;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExternalResourceService extends Plugin implements IClusterable{

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ExternalResourceService.class);

    private Map<String, ResourceManager> processMap = new HashMap<String, ResourceManager>();

    @SuppressWarnings({"ConstantConditions", "RedundantArrayCreation", "unchecked"})
    public ExternalResourceService(IPluginConfig config) {
        this(null, config, ResourceInvocationType.SITE);
    }

    protected void handleResourceManager(IPluginConfig config, ResourceInvocationType type) {
        String resourceProcesorClass = config.getString("plugin.class");

        if (resourceProcesorClass != null) {
            try {
                Class processorClass = Class.forName(resourceProcesorClass);
                Constructor con = processorClass.getConstructor(new Class[]{IPluginConfig.class, ResourceInvocationType.class});
                ResourceManager processor = (ResourceManager) con.newInstance(new Object[]{config, type});
                processMap.put(config.getString("type"), processor);
            } catch (Exception e) {
                log.error(e.getClass().getName().substring(e.getClass().getName().lastIndexOf('.') + 1) +
                        " instantiating plugin.class", e);
            }
        }
    }

    public ExternalResourceService(IPluginContext context, IPluginConfig config, ResourceInvocationType type) {
        super(context,config);
        Set<IPluginConfig> pluginSet = config.getPluginConfigSet();

        for (IPluginConfig iPluginConfig : pluginSet) {
            handleResourceManager(iPluginConfig, type);
        }
    }

    @SuppressWarnings({"ConstantConditions", "RedundantArrayCreation", "unchecked"})
    public ExternalResourceService(IPluginContext context, IPluginConfig config) {
        this(context, config, ResourceInvocationType.CMS);

        context.registerService(this, config.getString("external.processor.id", "external.processor.service"));
    }


    public ResourceManager getResourceProcessor(String type) {
        if (processMap.containsKey(type)) {
            return processMap.get(type);
        }
        return null;
    }

    public Synchronizable getSynchronizableProcessor(String type) {
        if (processMap.containsKey(type)) {
            if (processMap.get(type) instanceof Synchronizable) {
                return (Synchronizable) processMap.get(type);
            }
        }
        return null;
    }

    public Embeddable getEmbeddableProcessor(String type) {
        if (processMap.containsKey(type)) {
            if (processMap.get(type) instanceof Embeddable) {
                return (Embeddable) processMap.get(type);
            }
        }
        return null;
    }

    public void start() {
        log.info("Initializing External Resource Service");
    }

    public void stop() {
        log.info("Stopping External Resource Service");
    }
}

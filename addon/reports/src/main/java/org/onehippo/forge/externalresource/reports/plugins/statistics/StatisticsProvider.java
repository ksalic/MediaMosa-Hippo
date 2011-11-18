package org.onehippo.forge.externalresource.reports.plugins.statistics;


import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.reports.plugins.statistics.list.IStatisticsListColumn;

/**
 * @version $Id$
 */
public abstract class StatisticsProvider<T> implements IClusterable {

    public abstract void setResourceService(ExternalResourceService service);

    public abstract List<IStatisticsListColumn> getColumns(String[] selectedColumns);

    public abstract List<T> getListData();

    public abstract Map<String, Long> getChartData();

    public abstract String allColumnNames();

}

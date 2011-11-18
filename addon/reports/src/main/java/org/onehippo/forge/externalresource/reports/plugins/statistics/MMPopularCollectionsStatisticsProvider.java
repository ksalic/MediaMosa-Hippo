package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.uva.mediamosa.model.StatsPopularcollectionsType;

/**
 * @version $Id$
 */
public class MMPopularCollectionsStatisticsProvider extends MediaMosaStatisticsProvider<StatsPopularcollectionsType> {

    protected enum ColumnName {collId, ownerId, title, description, created, count}

    public MMPopularCollectionsStatisticsProvider(final Map<String, String> statisticsServiceParameters) {
        super(statisticsServiceParameters);

        itemColumnMap.put(ColumnName.collId.name(), new StringPropertyColumn("collId"));
        itemColumnMap.put(ColumnName.ownerId.name(), new StringPropertyColumn("ownerId"));
        itemColumnMap.put(ColumnName.title.name(), new StringPropertyColumn("title"));
        itemColumnMap.put(ColumnName.description.name(), new StringPropertyColumn("description"));
        itemColumnMap.put(ColumnName.created.name(), new DatePropertyColumn("created"));
        itemColumnMap.put(ColumnName.count.name(), new StringPropertyColumn("count"));
    }

     //We override this to let super deal with all Mediamosa labels.
    //If we don't, we must provide a properties file named after this class
    @Override
    protected String getResourceValue(String key) {
        return super.getResourceValue(key);
    }

    @Override
    public List<StatsPopularcollectionsType> getListData() {
        try {
            return service.getStatsPopularCollections();

        } catch (Exception e) {
            log.error("Error invoking MediaMosa service.", e);
        }
        return new ArrayList<StatsPopularcollectionsType>();
    }

    @Override
    public Map<String, Long> getChartData() {
        return null;
    }
}

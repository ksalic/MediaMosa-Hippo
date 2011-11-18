package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nl.uva.mediamosa.model.StatsPopularstreamsType;

/**
 * @version $Id$
 */
public class MMPopularMediaFilesStatisticsProvider extends MediaMosaStatisticsProvider<StatsPopularstreamsType> {

    protected enum ColumnName {mediafileId, ownerId, groupId, filename, assetId, requested, appId}

    public MMPopularMediaFilesStatisticsProvider(final Map<String, String> statisticsServiceParameters) {
        super(statisticsServiceParameters);

        //this is not visible but will be used to enable navigation to the video document in the cms
        itemColumnMap.put(ColumnName.assetId.name(), new ExtResourceIdentifierColumn("assetId"));

        itemColumnMap.put(ColumnName.mediafileId.name(), new StringPropertyColumn("mediafileId"));
        itemColumnMap.put(ColumnName.ownerId.name(), new StringPropertyColumn("ownerId"));
        itemColumnMap.put(ColumnName.groupId.name(), new StringPropertyColumn("groupId"));
        itemColumnMap.put(ColumnName.filename.name(), new StringPropertyColumn("filename"));
        itemColumnMap.put(ColumnName.requested.name(), new StringPropertyColumn("requested"));
        itemColumnMap.put(ColumnName.appId.name(), new StringPropertyColumn("appId"));
    }

     //We override this to let super deal with all Mediamosa labels.
    //If we don't, we must provide a properties file named after this class
    @Override
    protected String getResourceValue(String key) {
        return super.getResourceValue(key);
    }

    @Override
    public List<StatsPopularstreamsType> getListData() {
        try {

            /* NOT implemented yet
            orderBy
            orderDirection
            */
            return service.getStatsPopularStreams(
                    Integer.parseInt(getMMServiceParameter("limit", "200")),
                    Integer.parseInt(getMMServiceParameter("offset", "0")));

        } catch (Exception e) {
            log.error("Error invoking MediaMosa service.", e);
        }
        return new ArrayList<StatsPopularstreamsType>();
    }

    @Override
    public Map<String, Long> getChartData() {
        return null;
    }
}

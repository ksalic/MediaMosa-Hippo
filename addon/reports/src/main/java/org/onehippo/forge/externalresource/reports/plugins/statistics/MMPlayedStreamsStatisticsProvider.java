package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.uva.mediamosa.model.StatsPlayedstreamsType;


/**
 * @version $Id$
 */
public class MMPlayedStreamsStatisticsProvider extends MediaMosaStatisticsProvider<StatsPlayedstreamsType> {

    protected enum ColumnName {mediafileId, assetId, appId, ownerId, filesize, containerType, playType, played}

    public MMPlayedStreamsStatisticsProvider(final Map<String, String> statisticsServiceParameters) {
        super(statisticsServiceParameters);

        //this is not visible but will be used to enable navigation to the video document in the cms
        itemColumnMap.put(ColumnName.assetId.name(), new ExtResourceIdentifierColumn("assetId"));

        itemColumnMap.put(ColumnName.mediafileId.name(), new StringPropertyColumn("mediafileId"));
        itemColumnMap.put(ColumnName.appId.name(), new StringPropertyColumn("appId"));
        itemColumnMap.put(ColumnName.ownerId.name(), new StringPropertyColumn("ownerId"));
        itemColumnMap.put(ColumnName.filesize.name(), new FilesizePropertyColumn("filesize"));
        itemColumnMap.put(ColumnName.containerType.name(), new StringPropertyColumn("containerType"));
        itemColumnMap.put(ColumnName.playType.name(), new StringPropertyColumn("playType"));
        itemColumnMap.put(ColumnName.played.name(), new DatePropertyColumn("played"));
    }

     //We override this to let super deal with all Mediamosa labels.
    //If we don't, we must provide a properties file named after this class
    @Override
    protected String getResourceValue(String key) {
        return super.getResourceValue(key);
    }

    @Override
    public List<StatsPlayedstreamsType> getListData() {
        try {

            Calendar tempCalendar = null;

            String year = getMMServiceParameter("year", null);
            if (year == null || "{currentYear}".equals(year)) {
                year = String.valueOf((tempCalendar = Calendar.getInstance()).get(Calendar.YEAR));
            }

            String month = getMMServiceParameter("month", null);
            if (month == null || "{currentMonth}".equals(month)) {
                month = String.valueOf((tempCalendar == null ? Calendar.getInstance() : tempCalendar).get(Calendar.MONTH) + 1);
            }

            return service.getStatsPlayedStreams(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    getMMServiceParameter("playType", "object"),
                    getMMServiceParameter("groupId", null),
                    getMMServiceParameter("ownerId", null),
                    Integer.parseInt(getMMServiceParameter("limit", "200")),
                    Integer.parseInt(getMMServiceParameter("offset", "0")));

        } catch (Exception e) {
            log.error("Error invoking MediaMosa service.", e);
        }
        return new ArrayList<StatsPlayedstreamsType>();
    }

    @Override
    public Map<String, Long> getChartData() {
        List<StatsPlayedstreamsType> playedStreams = getListData();
        Map<String, Long> chartData = new HashMap<String, Long>();
        for(StatsPlayedstreamsType stream : playedStreams){
            if(chartData.containsKey(stream.getContainerType())){
                chartData.put(stream.getContainerType(), chartData.get(stream.getContainerType()) + 1);
            } else {
                chartData.put(stream.getContainerType(), 1L);
            }
        }
        
        return chartData;
    }

}

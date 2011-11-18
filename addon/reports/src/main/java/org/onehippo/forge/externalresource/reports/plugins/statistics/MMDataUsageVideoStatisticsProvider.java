package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import nl.uva.mediamosa.model.StatsDatausagevideoType;

/**
 * @version $Id$
 */
public class MMDataUsageVideoStatisticsProvider extends MediaMosaStatisticsProvider<StatsDatausagevideoType> {

    protected enum ColumnName {containerType, userId, groupId, type, appId, diskspaceMb}

    public MMDataUsageVideoStatisticsProvider(final Map<String, String> statisticsServiceParameters) {
        super(statisticsServiceParameters);

        itemColumnMap.put(ColumnName.containerType.name(), new StringPropertyColumn("containerType"));
        itemColumnMap.put(ColumnName.userId.name(), new StringPropertyColumn("userId"));
        itemColumnMap.put(ColumnName.groupId.name(), new StringPropertyColumn("groupId"));
        itemColumnMap.put(ColumnName.type.name(), new StatsDatausagevideoTypePropertyColumn("type"));
        itemColumnMap.put(ColumnName.appId.name(), new StringPropertyColumn("appId"));
        itemColumnMap.put(ColumnName.diskspaceMb.name(), new FilesizePropertyColumn("diskspaceMb"));
    }

    //We override this to let super deal with all Mediamosa labels.
    //If we don't, we must provide a properties file named after this class
    @Override
    protected String getResourceValue(String key) {
        return super.getResourceValue(key);
    }

    @Override
    public List<StatsDatausagevideoType> getListData() {
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

            return service.getStatsDatausageVideo(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    getMMServiceParameter("type", "container"),
                    Integer.parseInt(getMMServiceParameter("limit", "200")),
                    Integer.parseInt(getMMServiceParameter("offset", "0")));

        } catch (Exception e) {
            log.error("Error invoking MediaMosa service.", e);
        }
        return new ArrayList<StatsDatausagevideoType>();
    }



    @Override
    public Map<String, Long> getChartData() {
        List<StatsDatausagevideoType> listData = getListData();
        Map<String, Long> chartData = new HashMap<String, Long>();
        for (StatsDatausagevideoType usageItem : listData) {
            chartData.put(usageItem.getContainerType(), (long) usageItem.getDiskspaceMb());
        }
        return chartData;
    }



    protected class StatsDatausagevideoTypePropertyColumn extends MMPropertyColumn {

        public StatsDatausagevideoTypePropertyColumn(String name) {
            super(name);
        }

        @Override
        public String getValue(final Object statsItem) {

            StatsDatausagevideoType.TypeType type = (StatsDatausagevideoType.TypeType) getObjectValue(statsItem);
            if (type == null) {
                return StringUtils.EMPTY;
            }

            return type.value();
        }
    }


}

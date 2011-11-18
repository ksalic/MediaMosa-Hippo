package org.onehippo.forge.externalresource.reports.plugins.statistics;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.uva.mediamosa.model.StatsDatauploadType;

/**
 * @version $Id$
 */
public class MMDataUploadStatisticsProvider extends MediaMosaStatisticsProvider<StatsDatauploadType> {

    protected enum ColumnName {appId, ownerId, groupId, fileSize, timestamp}

    public MMDataUploadStatisticsProvider(final Map<String, String> statisticsServiceParameters) {
        super(statisticsServiceParameters);

        itemColumnMap.put(ColumnName.appId.name(), new StringPropertyColumn("appId"));
        itemColumnMap.put(ColumnName.ownerId.name(), new StringPropertyColumn("ownerId"));
        itemColumnMap.put(ColumnName.groupId.name(), new StringPropertyColumn("groupId"));
        itemColumnMap.put(ColumnName.fileSize.name(), new FilesizePropertyColumn("fileSize"));
        itemColumnMap.put(ColumnName.timestamp.name(), new DatePropertyColumn("timestamp"));
    }

    //We override this to let super deal with all Mediamosa labels.
    //If we don't, we must provide a properties file named after this class
    @Override
    protected String getResourceValue(String key) {
        return super.getResourceValue(key);
    }

    @Override
    public List<StatsDatauploadType> getListData() {
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

            return service.getStatsDataUpload(
                    Integer.parseInt(year),
                    Integer.parseInt(month),
                    getMMServiceParameter("userId", null),
                    getMMServiceParameter("groupId", null),
                    Integer.parseInt(getMMServiceParameter("limit", "200")),
                    Integer.parseInt(getMMServiceParameter("offset", "0")));

        } catch (Exception e) {
            log.error("Error invoking MediaMosa service.", e);
        }
        return new ArrayList<StatsDatauploadType>();
    }

    @Override
    public Map<String, Long> getChartData() {
        List<StatsDatauploadType> uploadData = getListData();
        Map<String, Long> chartData = new HashMap<String, Long>();
        for(StatsDatauploadType upload : uploadData){
            if(chartData.containsKey(upload.getGroupId())){
                chartData.put(upload.getGroupId(), chartData.get(upload.getGroupId()) + upload.getFileSize());
            } else {
                chartData.put(upload.getGroupId(), new Long(upload.getFileSize()));
            }
        }

        return chartData;
    }
}

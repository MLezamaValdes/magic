package tsdb.streamdb;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tsdb.util.DataEntry;
import tsdb.util.DataRow;
import tsdb.util.TimeConverter;
import tsdb.util.TimeSeriesMask;
import tsdb.util.Util;
import tsdb.util.iterator.TimestampSeries;
import tsdb.util.iterator.TsIterator;

public class StreamStorageStreamDB implements StreamStorage {

	private static final Logger log = LogManager.getLogger();

	private StreamDB streamdb;

	public StreamStorageStreamDB(String streamdbPathPrefix) {		
		this.streamdb = new StreamDB(streamdbPathPrefix);
	}

	@Override
	public void clear() {
		streamdb.clear();		
	}

	@Override
	public void close() {
		streamdb.close();		
	}

	@Override
	public void insertData(String streamName, TreeMap<Long, DataRow> eventMap, String[] sensorNames) {
		ArrayList<DataEntry> sensorData = new ArrayList<DataEntry>(eventMap.size());
		for(int i=0;i<sensorNames.length;i++) {
			sensorData.clear();
			for(DataRow event:eventMap.values()) {
				float value = (float) event.data[i];
				if(!Float.isNaN(value)&&!(value==-9999f)&&(-999999f<value)&&(value<999999f)) { // NaN some files (in AET06)
					if(value<-9999f||value>9999f) {
						log.trace(value+"                     "+sensorNames[i]+"                "+streamName);
					}
					sensorData.add(new DataEntry((int) event.timestamp,value));
				}
			}
			if(!sensorData.isEmpty()) {
				streamdb.insertSensorData(streamName, sensorNames[i], sensorData.toArray(new DataEntry[0]));
			}
		}
	}

	@Override
	public void insertEventList(String streamName, List<DataRow> eventList,long first, long last, String[] sensorNames) {
		ArrayList<DataEntry> sensorData = new ArrayList<DataEntry>(eventList.size());
		for(int i=0;i<sensorNames.length;i++) {
			sensorData.clear();
			for(DataRow event:eventList) {
				float value = (float) event.data[i];
				if(!Float.isNaN(value)) {
					sensorData.add(new DataEntry((int) event.timestamp,value));
				}
			}
			if(!sensorData.isEmpty()) {
				streamdb.insertSensorData(streamName, sensorNames[i], sensorData.toArray(new DataEntry[0]));
			}
		}	
	}

	@Override
	public TsIterator getRawIterator(String stationName, String[] sensorNames, Long start, Long end) {
		log.info("StreamDB get "+stationName+" with "+Util.arrayToString(sensorNames)+"     at "+TimeConverter.oleMinutesToText(start)+" - "+TimeConverter.oleMinutesToText(end));
		int minTimestamp;
		int maxTimestamp;
		if(start==null) {
			minTimestamp = Integer.MIN_VALUE;
		} else {
			minTimestamp = (int)(long)start;
		}
		if(end==null) {
			maxTimestamp = Integer.MAX_VALUE;
		} else {
			maxTimestamp = (int)(long)end;
		}		
		return streamdb.getTsIterator(stationName, sensorNames, minTimestamp, maxTimestamp);
	}

	@Override
	public StreamIterator getRawSensorIterator(String stationName, String sensorName, Long start, Long end) {
		log.info("StreamDB get raw sensor "+stationName+" with "+sensorName+"     at "+TimeConverter.oleMinutesToText(start)+" - "+TimeConverter.oleMinutesToText(end));
		int minTimestamp;
		int maxTimestamp;
		if(start==null) {
			minTimestamp = Integer.MIN_VALUE;
		} else {
			minTimestamp = start.intValue();
		}
		if(end==null) {
			maxTimestamp = Integer.MAX_VALUE;
		} else {
			maxTimestamp = end.intValue();
		}
		return streamdb.getSensorIterator(stationName, sensorName, minTimestamp, maxTimestamp);
	}

	@Override
	public void getInfo() {
		//TODO		
	}

	@Override
	public long[] getTimeInterval(String streamName) {
		int[] interval = streamdb.getTimeInterval(streamName);
		if(interval==null) {
			return null;
		}
		return new long[]{interval[0],interval[1]};
	}

	@Override
	public String[] getSensorNames(String stationName) {
		NavigableSet<String> set = streamdb.getSensorNames(stationName);
		if(set==null) {
			return null;
		}
		return set.toArray(new String[set.size()]);
	}

	@Override
	public TimeSeriesMask getTimeSeriesMask(String stationName, String sensorName) {
		return streamdb.getSensorTimeSeriesMask(stationName, sensorName, false);
	}

	@Override
	public void setTimeSeriesMask(String stationName, String sensorName, TimeSeriesMask timeSeriesMask) {
		streamdb.setSensorTimeSeriesMask(stationName, sensorName, timeSeriesMask);
		streamdb.commit();
	}

	@Override
	public void insertTimestampSeries(TimestampSeries timestampSeries) {
		log.info("streamDB insert TimestampSeries "+timestampSeries.name);
		String stationName = timestampSeries.name;
		for(String sensorName:timestampSeries.sensorNames) {
			DataEntry[] data = timestampSeries.toDataEntyArray(sensorName);
			if(data!=null&&data.length>0) {
				//System.out.println("insert in station "+stationName+" sensor "+sensorName+"  elements "+data.length);
				streamdb.insertSensorData(stationName, sensorName, data);
			}
		}

	}

	public void insertDataEntyArray(String stationName, String sensorName, DataEntry[] dataEntries) {
		log.info("streamDB insert DataEntyArray "+stationName+"/"+sensorName);
		if(dataEntries!=null&&dataEntries.length>0) {
			streamdb.insertSensorData(stationName, sensorName, dataEntries);
		}
	}

	public NavigableSet<String> getStationNames() {
		return streamdb.getStationNames();		
	}

}

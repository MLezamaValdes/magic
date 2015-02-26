package tsdb;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import tsdb.raw.TimestampSeries;
import tsdb.streamdb.StreamIterator;
import tsdb.util.iterator.TsIterator;

/**
 * This class encapsulates EventStore and provides basic methods store and query time series data. 
 * @author woellauer
 *
 */
public interface StreamStorage {

	public void clear();

	/**
	 * closes all open files and flushes pending data to disk.
	 */
	public void close();

	/**
	 * Saves new data on disk. If some timestamps exist already in database, no data is overwritten.
	 * @param streamName
	 * @param eventMap
	 * @param sensorNames 
	 */
	public void insertData(String streamName, TreeMap<Long,DataRow> eventMap, String[] sensorNames);
	
	/**
	 * insert ordered list of data with time span first to last
	 * @param streamName
	 * @param eventList
	 * @param first
	 * @param last
	 * @param sensorNames 
	 */
	public void insertEventList(String streamName, List<DataRow> eventList, long first, long last, String[] sensorNames);

	public TsIterator getRawIterator(String stationName, String[] sensorNames, Long minTimestamp, Long maxTimestamp, String[] eventSchema);
	
	public void getInfo();

	long[] getTimeInterval(String streamName);

	String[] getSensorNames(String stationName);

	StreamIterator getRawSensorIterator(String stationName, String sensorName, Long start, Long end);

	TimeSeriesMask getTimeSeriesMask(String stationName, String sensorName);

	void setTimeSeriesMask(String stationName, String sensorName, TimeSeriesMask timeSeriesMask);

	public void insertTimestampSeries(TimestampSeries timestampSeries);
}

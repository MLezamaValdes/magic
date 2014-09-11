package tsdb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;

import tsdb.aggregated.AggregationType;
import tsdb.raw.iterator.EventConverterIterator;
import tsdb.util.TimestampInterval;
import tsdb.util.Util;
import tsdb.util.iterator.TsIterator;
import de.umr.jepc.store.Event;

/**
 * This class contains metadata that is associated with a station (plotID).
 * @author woellauer
 *
 */
public class Station extends TsDBClient {
	
	/**
	 * Stream name of this station
	 */
	public final String stationID;
	
	public final LoggerType loggerType;	

	/**
	 * 
	 * list of property map for Kili
	 */
	public List<TimestampInterval<StationProperties>>   propertiesList;

	/**
	 * translation map: input sensor name -> database sensor name
	 * This map contains only entries that are specific for this Station (or plotID)
	 */
	public Map<String,String> sensorNameTranlationMap;
	
	//*** start of fields that are used if this station is identical to one plot ***
	public final boolean isPlot;
	public double geoPoslongitude;
	public double geoPosLatitude;
	/**
	 * The general name of this plotID for example HEG03 it is HEG
	 * This name belongs to a GeneralStation Object
	 */
	public final GeneralStation generalStation;

	/**
	 * list of stations of same general station id ordered by position difference to this station
	 */
	public List<Station> nearestStations;
	
	/**
	 * serial number of station: A19557, A2277, ...
	 * not used currently - station is identified with plotID
	 */
	public String alternativeID = null;
	//*** end of fields that are used if this station is identical to one plot ***
	
	public Station(TsDB tsdb, GeneralStation generalStation, String stationID, LoggerType loggerType, List<StationProperties> propertyMapList, boolean isPlot) {
		super(tsdb);
		this.isPlot = isPlot;
		this.generalStation = generalStation;
		this.stationID = stationID;
		this.propertiesList = StationProperties.createIntervalList(propertyMapList);
		this.geoPoslongitude = Float.NaN;
		this.geoPosLatitude = Float.NaN;
		this.loggerType = loggerType;
		sensorNameTranlationMap = new HashMap<String, String>();
		if(isPlot) {
			if(propertiesList.size()!=1) {
				log.warn("station that is plot can only have one StationProperties: "+propertiesList.size());
				if(!stationID.equals(propertyMapList.get(0).get_plotid())) {
					log.warn("stationID is not equal to plotID for station that is plot: "+stationID+"  "+propertyMapList.get(0).get_plotid());
				}
			}
		} else {
			for(StationProperties property:propertyMapList) {
				if(!stationID.equals(property.get_serial())) {
					log.warn("stationID does not equal to serial: "+stationID+"  "+property.get_serial());
				}
				if(!loggerType.typeName.equals(property.get_logger_type_name())) {
					log.warn("station logger does not equal to property logger: "+loggerType.typeName+"  "+property.get_logger_type_name()+" in "+stationID);
				}
			}
		}
	}	

	/**
	 * This method determines the database sensor name out of an input sensor name.
	 * Steps:
	 * 1. check if there an entry in the plotID specific sensorNameTranlationMap
	 * 2. else check if there is an entry in the general station  sensorNameTranlationMap
	 * 3. else check if there is an entry in the logger type sensorNameTranlationMap
	 * 4. else return null (either input sensor name and database sensor name are identical or sensor name is unknown / not used)
	 * @param sensorName
	 * @return
	 */
	public String translateInputSensorName(String sensorName, boolean useGeneralstation) {
		String resultName = sensorNameTranlationMap.get(sensorName);
		if(resultName!=null) {
			return resultName;
		}
		if(useGeneralstation) {
			resultName = generalStation.sensorNameTranlationMap.get(sensorName);
			if(resultName!=null) {
				return resultName;
			}
		}
		resultName = loggerType.sensorNameTranlationMap.get(sensorName);
		if(resultName!=null) {
			return resultName;
		}
		String[] schemaSensorNames = loggerType.sensorNames;
		for(String schemaSensorName:schemaSensorNames) {
			if(schemaSensorName.equals(sensorName)) {
				return sensorName;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return stationID+"("+loggerType.typeName+")";
	}

	public TsIterator queryRaw(String[] querySchema, Long start, Long end) {		
		Iterator<Event> rawEventIterator = tsdb.streamStorage.queryRawEvents(stationID,start,end);
		if(rawEventIterator==null) {
			return null;
		}		
		String[] inputSchema = loggerType.sensorNames;		
		if(querySchema==null) {
			return new EventConverterIterator(inputSchema, rawEventIterator, inputSchema);
		} else {			
			return new EventConverterIterator(inputSchema, rawEventIterator, querySchema);
		}
	}

	/*public String[] getValidSchemaEntries(String[] querySchema) {
		Map<String, Integer> schemaMap = Util.stringArrayToMap(loggerType.sensorNames);
		ArrayList<String> resultList = new ArrayList<String>();
		for(String sensorName:querySchema) {
			if(schemaMap.containsKey(sensorName)) {
				resultList.add(sensorName);
			}
		}
		if(resultList.size()==0) {
			return null;
		}
		return resultList.toArray(new String[0]);
	}*/
	
	public String[] getValidSchemaEntries(String[] querySchema) {		
		return Util.getValidEntries(querySchema, loggerType.sensorNames);
	}
	
	public boolean isValidSchema(String[] querySchema) {
		return !(querySchema==null||querySchema.length==0||!Util.isContained(querySchema, loggerType.sensorNames));
	}
	
	public boolean isValidBaseSchema(String[] querySchema) {
		if(!isValidSchema(querySchema)) {
			return false;
		}
		for(String name:querySchema) {
			if(tsdb.getSensor(name).baseAggregationType==AggregationType.NONE) {
				return false;
			}
		}
		return true;
	}

	public List<Station> getNearestStationsWithSensor(String sensorName) {
		ArrayList<Station> result = new ArrayList<Station>();
		for(Station station:nearestStations) {
			for(String name:station.loggerType.sensorNames) {
				if(sensorName.equals(name)) {
					result.add(station);
					break;
				}				
			}
		}
		return result;
	}
	
	public StationProperties getProperties(long intervalStart, long intervalEnd) {
		StationProperties properties = null;
		for(TimestampInterval<StationProperties> interval:propertiesList) {
			if((interval.start==null || interval.start<=intervalStart) && (interval.end==null || intervalEnd<=interval.end)) {
				if(properties!=null) {
					log.warn("multiple properties for one time interval: "+intervalStart+" "+intervalEnd);
				}
				properties = interval.value;
			}
		}
		return properties;
	}
	
	public String[] getSchema() {
		return loggerType.sensorNames;
	}
}
package tsdb;

import java.io.Serializable;

import tsdb.aggregated.AggregationType;

/**
 * This class contains metadata that is associated with a sensor.
 * Only sensors that are part of base aggregation are created.
 * @author woellauer
 *
 */
public class Sensor implements Serializable {
	
	private static final long serialVersionUID = -4139931796468207965L;

	/**
	 * parameter name of this sensor
	 */
	public String name;
	
	/**
	 * physical minimum
	 */
	public float physicalMin;
	
	/**
	 * physical maximum
	 */
	public float physicalMax;
	
	/**
	 * minimal change within a base aggregation time interval
	 */
	public float stepMin;
	
	/**
	 * maximum change within a base aggregation time interval
	 */
	public float stepMax;
	
	/**
	 * Type of aggregation for base aggregation
	 */
	public AggregationType baseAggregationType;
	
	/**
	 * fill gaps in time series of this sensor
	 */
	public boolean useInterpolation;
	
	/**
	 * maximum difference of empirical value, may be null if not used.
	 */
	public Float empiricalDiff;
	
	public String description;
	
	public String unitDescription;
	
	public Sensor(String name) {
		this.name = name;
		physicalMin = -Float.MAX_VALUE;
		physicalMax = Float.MAX_VALUE;
		stepMin = 0.0f;
		stepMax = Float.MAX_VALUE;
		baseAggregationType = AggregationType.NONE;
		useInterpolation = false;
		empiricalDiff = null;
	}
	
	/**
	 * checks if value is in physical range
	 * @param value  value == NaN  ==> false
	 * @return if false value should not be included in further processing
	 */
	public boolean checkPhysicalRange(float value) {
		if(Float.isNaN(value)) {
			return false;
		}
		return physicalMin<=value&&value<=physicalMax;		
	}

	/**
	 * precondition: prevValue and value are valid (no NaN values)
	 * @param prevValue
	 * @param value
	 * @return
	 */
	public boolean checkStepRange(float prevValue, float value) {
		float step = Math.abs(value-prevValue);		
		return stepMin<=step&&step<=stepMax;
	}
}

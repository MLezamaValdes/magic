package timeseriesdatabase;

import java.util.Map;

/**
 * This class contains metadata that is associated with a group of stations like HEG or HEW.
 * @author Stephan W�llauer
 *
 */
public class GeneralStation {
	
	public String name;
	
	public Map<String,String> sensorNameTranlationMap;
	
	public GeneralStation(String name) {
		this.name = name;
	}

}

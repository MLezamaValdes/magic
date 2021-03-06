package dat_decode;

/**
 * Metadata of one sensor.
 * @author W�llauer
 *
 */
public class SensorHeader {
	
	public final String name;
	public final String unit;
	public short dataType;

	public SensorHeader(String name, String unit, short dataType) {
		this.name = name;
		this.unit = unit;
		this.dataType = dataType;
	}

	public void printHeader() {
		System.out.println("sensor: "+name+"\t unit: "+unit);
		
	}
	
	public String getName() {
		return name;
	}
	
	public String getUnit() {
		return unit;
	}
	
	@Override
	public String toString() {
		return name+" "+unit+" "+dataType;
	}

}

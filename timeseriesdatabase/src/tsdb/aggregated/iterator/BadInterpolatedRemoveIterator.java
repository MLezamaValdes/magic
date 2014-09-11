package tsdb.aggregated.iterator;

import java.util.Arrays;
import java.util.List;

import tsdb.Sensor;
import tsdb.TsDB;
import tsdb.raw.TimeSeriesEntry;
import tsdb.util.ProcessingChainEntry;
import tsdb.util.TimeSeriesSchema;
import tsdb.util.TsSchema;
import tsdb.util.iterator.MoveIterator;
import tsdb.util.iterator.TsIterator;

/**
 * Processed all interpolated values and removes all values that are of low quality.
 * @author woellauer
 *
 */
public class BadInterpolatedRemoveIterator extends MoveIterator {
	
	private TsIterator input_iterator;
	private TimeSeriesEntry prev;
	private Sensor[] sensors;
	
	public static TsSchema createSchema(TsSchema input_schema) {
		input_schema.throwNoQualityFlags();
		boolean hasQualityFlags = true;
		input_schema.throwNoInterpolatedFlags();
		boolean hasInterpolatedFlags = true;
		return new TsSchema(input_schema.names, input_schema.aggregation,input_schema.timeStep, input_schema.isContinuous, hasQualityFlags, hasInterpolatedFlags);
	}

	public BadInterpolatedRemoveIterator(TsDB timeSeriesDatabase, TsIterator input_iterator) {
		super(createSchema(input_iterator.getSchema()));
		this.input_iterator = input_iterator;
		this.prev = null;
		this.sensors = timeSeriesDatabase.getSensors(schema.names);
	}

	@Override
	public List<ProcessingChainEntry> getProcessingChain() {
		List<ProcessingChainEntry> result = input_iterator.getProcessingChain();
		result.add(this);
		return result;
	}

	@Override
	protected TimeSeriesEntry getNext() {
		if(input_iterator.hasNext()) {
			TimeSeriesEntry current = input_iterator.next();
			
			boolean someInterpolated= false;
			for(int i=0;i<current.interpolated.length;i++) {
				if(current.interpolated[i]) {
					someInterpolated = true;
					break;
				}
			}
			if(someInterpolated&&prev!=null) {
				
				float[] data = Arrays.copyOf(current.data,current.data.length);
				boolean[] interpolated = Arrays.copyOf(current.interpolated,current.interpolated.length);
				boolean someChecksFailed = false;
				for(int i=0;i<schema.length;i++) {
					if(interpolated[i]) {
						if(!check(i,prev.data[i],data[i])) {
							someChecksFailed = true;
							interpolated[i] = false;
							data[i] = Float.NaN;
						}
					}
				}				
				prev = current;
				if(someChecksFailed) {
					return new TimeSeriesEntry(current.timestamp, data, current.qualityFlag, null, interpolated);
				} else {
					return current;
				}
			} else {
				prev = current;
				return current;
			}			
		} else {
			return null;
		}
	}

	private boolean check(int columnIndex, float prev, float value) {
		if(!sensors[columnIndex].checkPhysicalRange(value)) {
			return false;
		}
		if(!sensors[columnIndex].checkStepRange(prev, value)) {
			return false;
		}
		return true;
	}

}

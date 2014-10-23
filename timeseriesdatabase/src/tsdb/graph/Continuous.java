package tsdb.graph;

import static tsdb.util.AssumptionCheck.throwNull;

import org.apache.logging.log4j.LogManager;

import tsdb.Station;
import tsdb.TimeConverter;
import tsdb.TsDB;
import tsdb.aggregated.BaseAggregationTimeUtil;
import tsdb.aggregated.iterator.NanGapIterator;
import tsdb.util.iterator.TsIterator;


/**
 * This node creates continuous values from a source with gaps in time.
 * @author woellauer
 *
 */
public interface Continuous extends Node {
	
	@Override
	public default boolean isContinuous() {
		return true;
	}
	
	public TsIterator getExactly(long start, long end);
	
	public static Continuous create(Base base) {
		return new Concrete(base);
	}
	
	public abstract class Abstract implements Continuous {
		
		protected final TsDB tsdb; //not null
		
		protected Abstract(TsDB tsdb) {
			throwNull(tsdb);
			this.tsdb = tsdb;
		}	
	}
	
	public class Concrete implements Continuous {
		private final Base source;
		protected Concrete(Base source) {
			throwNull(source);
			this.source = source;
			if(!source.isConstantTimestep()) {
				throw new RuntimeException("source with no constant timestep");
			}
		}		

		@Override
		public TsIterator get(Long start, Long end) {
			TsIterator input_iterator = source.get(start, end);
			if(input_iterator==null||!input_iterator.hasNext()) {
				return null;
			}
			if(start!=null&&!BaseAggregationTimeUtil.isBaseAggregationTimestamp(start)) {
				LogManager.getLogger().warn("start timestamp not alligned: "+start+"   "+TimeConverter.oleMinutesToText(start));
				start = BaseAggregationTimeUtil.calcBaseAggregationTimestamp(start); // TODO ?
			}
			if(end!=null&&!BaseAggregationTimeUtil.isBaseAggregationTimestamp(end)) {
				LogManager.getLogger().warn("end timestamp not alligned: "+end+"   "+TimeConverter.oleMinutesToText(end));
				end = BaseAggregationTimeUtil.calcBaseAggregationTimestamp(end); // TODO ?
			}
			NanGapIterator continuous = new NanGapIterator(input_iterator, start, end);
			if(!continuous.hasNext()) {
				return null;
			}
			return continuous;
		}
		
		@Override
		public TsIterator getExactly(long start, long end) {		
			return get(start,end);
		}

		@Override
		public Station getSourceStation() {
			return source.getSourceStation();
		}

		@Override
		public String[] getSchema() {
			return source.getSchema();
		}

		@Override
		public boolean isConstantTimestep() {
			return true;
		}

	
	}
}

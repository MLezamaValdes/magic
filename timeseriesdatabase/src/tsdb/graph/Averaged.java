package tsdb.graph;

import static tsdb.util.AssumptionCheck.throwNulls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tsdb.Station;
import tsdb.TsDB;
import tsdb.iterator.AverageIterator;
import tsdb.util.iterator.TsIterator;

/**
 * This node creates average values per time step over all sources.
 * @author woellauer
 *
 */
public class Averaged extends Continuous.Abstract {

	private static final Logger log = LogManager.getLogger();

	private final List<Continuous> sources; //not null
	private final String[] schema; //not null
	private final int minCount;
	private final boolean _constant_timestep;

	public Averaged(TsDB tsdb, List<Continuous> sources, String[] schema, int minCount) {
		super(tsdb);
		throwNulls(sources,schema);
		if(sources.isEmpty()) {
			throw new RuntimeException("no sources");	
		}
		if(minCount<1) {
			log.warn("no senseful min count= "+minCount);
		}
		if(sources.size()<minCount) {
			log.warn("insufficient sources with min count= "+minCount+"  "+sources.size());
		}
		this.minCount = minCount;
		this._constant_timestep = sources.get(0).isConstantTimestep();
		for(Continuous source:sources) {
			if(!source.isContinuous() || source.isConstantTimestep()!=_constant_timestep) {
				throw new RuntimeException("different source types");
			}
		}
		this.sources = sources;
		this.schema = schema;
	}

	public static Averaged of(TsDB tsdb, List<Continuous> sources, int minCount) {		
		Set<String> schemaSet = new LinkedHashSet<String>();
		for(Continuous continuous:sources) {
			String[] schema = continuous.getSchema();
			if(schema!=null&&schema.length>0) {
				schemaSet.addAll(Arrays.asList(schema));
			}
		}
		return new Averaged(tsdb, sources, schemaSet.toArray(new String[0]), minCount);
	}

	@Override
	public TsIterator get(Long start, Long end) {
		return getExactly(start,end);
	}

	public TsIterator getExactly(long start, long end) {		
		List<TsIterator> iteratorList = new ArrayList<>();		
		for(Continuous source:sources) {
			TsIterator it = source.get(start, end);
			if(it!=null&&it.hasNext()) {
				iteratorList.add(it);				
			}
		}
		if(iteratorList.size()<minCount) {
			return null;
		}
		return new AverageIterator(schema, iteratorList.toArray(new TsIterator[0]), minCount);		
	}

	@Override
	public Station getSourceStation() { // multiple source stations
		return null;
	}

	@Override
	public String[] getSchema() {
		return schema;
	}

	@Override
	public boolean isConstantTimestep() {
		return _constant_timestep;
	}

}

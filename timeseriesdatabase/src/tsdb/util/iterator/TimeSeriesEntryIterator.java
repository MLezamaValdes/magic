package tsdb.util.iterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tsdb.raw.TimeSeriesEntry;
import tsdb.util.ProcessingChainEntry;
import tsdb.util.TimeSeriesSchema;

public class TimeSeriesEntryIterator extends TsIterator {
	
	private Iterator<TimeSeriesEntry> input_iterator;

	public TimeSeriesEntryIterator(Iterator<TimeSeriesEntry> input_iterator, String[] schema) {
		super(new TimeSeriesSchema(schema).toTsSchema());
		this.input_iterator = input_iterator;
	}

	@Override
	public boolean hasNext() {
		return input_iterator.hasNext();
	}

	@Override
	public TimeSeriesEntry next() {
		return input_iterator.next();
	}

	@Override
	public List<ProcessingChainEntry> getProcessingChain() {
		List<ProcessingChainEntry> result = new ArrayList<ProcessingChainEntry>();
		result.add(this);
		return result;
	}

}

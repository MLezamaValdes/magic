package tsdb.util.iterator;

import tsdb.util.TsSchema;

/**
 * Base class for iterators that process input from one TimeSeriesIterator
 * @author woellauer
 *
 */
public abstract class InputProcessingIterator extends MoveIterator {
	
	protected final TsIterator input_iterator;

	public InputProcessingIterator(TsIterator input_iterator, TsSchema output_schema) {
		super(output_schema);
		this.input_iterator = input_iterator;
	}

	@Override
	public NewProcessingChain getProcessingChain() {		
		return new NewProcessingChainOneSource(input_iterator,this);
	}	
}

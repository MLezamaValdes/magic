package util;

import java.sql.Time;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sun.xml.internal.messaging.saaj.packaging.mime.util.QEncoderStream;

import timeseriesdatabase.DataQuality;
import timeseriesdatabase.QueryProcessor;
import timeseriesdatabase.aggregated.iterator.ApplyIterator;
import timeseriesdatabase.aggregated.iterator.LinearIterpolationIterator;
import timeseriesdatabase.aggregated.iterator.NanGapIterator;
import timeseriesdatabase.aggregated.iterator.ProjectionIterator;
import timeseriesdatabase.raw.TimeSeriesEntry;
import timeseriesdatabase.raw.TimestampSeries;
import util.iterator.TimeSeriesIterator;

/**
 * Builder creates a Factory to create a chain of TimeseriesIterators
 * @author woellauer
 *
 */
public class Builder implements Iterable<TimeSeriesEntry> {
	
	public static Builder base_aggregated(QueryProcessor qp, String plotID, String[] querySchema, Long queryStart, Long queryEnd, DataQuality dataQuality) {
		return new Builder(()->qp.query_base_aggregated(plotID, querySchema, queryStart, queryEnd, dataQuality), queryStart, queryEnd);
	}
	
	public static Builder continuous_base_aggregated(QueryProcessor qp, String plotID, String[] querySchema, Long queryStart, Long queryEnd, DataQuality dataQuality) {
		return new Builder(()->qp.query_continuous_base_aggregated(plotID, querySchema, queryStart, queryEnd, dataQuality), queryStart, queryEnd);
	}
	
	public static TimeSeriesIterator project(TimeSeriesIterator it, String ... schema) {
		return new ProjectionIterator(it,schema);
	}
	
	public static TimeSeriesIterator project(TimeSeriesIterator it, TimeSeriesIterator targetSchema) {
		return project(it, targetSchema.getOutputSchema());
	}
	
	public static TimeSeriesIterator continuous(TimeSeriesIterator it) {
		return new NanGapIterator(it,null,null);
	}
	
	public static TimeSeriesIterator continuous(TimeSeriesIterator it, Long start, Long end) {
		return new NanGapIterator(it,start,end);
	}
	
	public static TimeSeriesIterator apply(TimeSeriesIterator it, Function<TimeSeriesEntry,TimeSeriesEntry> mapper) {
		return new ApplyIterator(it,mapper);
	}
	
	public static TimeSeriesIterator linearIterpolate(TimeSeriesIterator it) {
		return new LinearIterpolationIterator(it);
	}
	
	private final Supplier<TimeSeriesIterator> supplier;
	
	/*public Builder(Supplier<TimeSeriesIterator> supplier) {
		this.supplier = supplier;
		this.queryStart = null;
		this.queryEnd = null;
	}*/
	
	public Builder(Supplier<TimeSeriesIterator> supplier, Long queryStart, Long queryEnd) {
		this.supplier = supplier;
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	
	public TimeSeriesIterator create() {
		return supplier.get();
	}
	
	@Override
	public Iterator<TimeSeriesEntry> iterator() {
		return create();
	}
	
	//**************************************************************************
	
	public final Long queryStart;
	public final Long queryEnd;
	
	/**
	 * Project schema to new schema
	 * @param schema
	 * @return
	 */
	public Builder project(String ... schema) {
		return new Builder(()->project(this.create(),schema),this.queryStart,this.queryEnd);
	}
	
	/**
	 * Project schema to schema of target_iterator
	 * @param target_iterator
	 * @return
	 */
	public Builder project(TimeSeriesIterator target_iterator) {
		return new Builder(()->project(this.create(),target_iterator),this.queryStart,this.queryEnd);
	}
	
	/**
	 * Fill gaps in time with nan rows.
	 * @param start
	 * @param end
	 * @return
	 */
	public Builder continuous(Long start, Long end) {
		return new Builder(()->continuous(this.create(),start,end),start,end);
	}
	
	/**
	 * Fill gaps in time with nan rows.
	 * @param start
	 * @param end
	 * @return
	 */
	public Builder continuous() {
		return new Builder(()->continuous(this.create(),this.queryStart,this.queryEnd),this.queryStart,this.queryEnd);
	}
	
	/**
	 * Apply to each TimeSeriesEntry a mapper function.
	 * @param mapper
	 * @return
	 */
	public Builder apply(Function<TimeSeriesEntry,TimeSeriesEntry> mapper) {
		return new Builder(()->apply(this.create(),mapper),this.queryStart,this.queryEnd);
	}
	
	/**
	 * Interpolate one value gaps in time series with Average between previous and next value.
	 * @return
	 */
	public Builder linearInterpolate() {
		return new Builder(()->linearIterpolate(this.create()),this.queryStart,this.queryEnd);
	}
	
	/**
	 * Write output to CSV-File
	 * @param filename
	 */
	public void writeCSV(String filename) {
		this.create().writeCSV(filename);
	}
	
	public TimestampSeries createTimestampSeries() {
		return Util.ifnull(this.create(), it->TimestampSeries.create(it));
	}


	
	

}
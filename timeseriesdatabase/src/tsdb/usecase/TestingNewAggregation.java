package tsdb.usecase;

import tsdb.DataQuality;
import tsdb.TsDB;
import tsdb.TsDBFactory;
import tsdb.aggregated.iterator.DayAggregationIterator;
import tsdb.aggregated.iterator.MonthAggregationIterator;
import tsdb.aggregated.iterator.WeekAggregationIterator;
import tsdb.aggregated.iterator.YearAggregationIterator;
import tsdb.graph.QueryPlan;
import tsdb.util.iterator.TsIterator;

public class TestingNewAggregation {
	
	public static void main(String[] args) {
		System.out.println("begin...");
		
		TsDB tsdb = TsDBFactory.createDefault();
		
		TsIterator it = QueryPlan.getContinuousGen(tsdb, DataQuality.NO).get("HEG01", new String[]{"Ta_200"}).get(null, null);
		it = new DayAggregationIterator(tsdb, it);
		//it = new WeekAggregationIterator(tsdb, it);
		it = new MonthAggregationIterator(tsdb, it);
		it = new YearAggregationIterator(tsdb, it);
		it.writeConsole();
		System.out.println(it.getProcessingChain().getText());
		
		System.out.println("...end");
	}

}
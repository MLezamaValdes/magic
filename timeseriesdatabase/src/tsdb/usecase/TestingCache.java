package tsdb.usecase;

import tsdb.DataQuality;
import tsdb.TsDBFactory;
import tsdb.QueryProcessor;
import tsdb.TsDB;
import tsdb.aggregated.AggregationInterval;
import tsdb.util.iterator.TsIterator;

public class TestingCache {

	public static void main(String[] args) {
		System.out.println("start...");
		TsDB timeSeriesDatabase = TsDBFactory.createDefault();
		QueryProcessor qp = new QueryProcessor(timeSeriesDatabase);

		String plotID = "HEG20";
		String[] querySchema = null;
		Long queryStart = null;
		Long queryEnd = null;
		AggregationInterval aggregationInterval = AggregationInterval.DAY;
		DataQuality dataQuality = DataQuality.NO;
		boolean useInterpolation = true;

		TsIterator it = qp.query_aggregated(plotID, querySchema, queryStart, queryEnd, dataQuality, aggregationInterval, useInterpolation);
		timeSeriesDatabase.cacheStorage.writeNew("myStream", it);
		
		TsIterator iterator = timeSeriesDatabase.cacheStorage.query("myStream", null, null);
		while(iterator.hasNext()) {
			System.out.println(iterator.next());
		}


		timeSeriesDatabase.cacheStorage.printInfo();


		System.out.println("...end");
	}

}

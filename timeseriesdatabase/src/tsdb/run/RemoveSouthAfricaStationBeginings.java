package tsdb.run;

import java.time.LocalDateTime;

import tsdb.StationProperties;
import tsdb.TsDB;
import tsdb.TsDBFactory;
import tsdb.VirtualPlot;
import tsdb.util.TimeConverter;
import tsdb.util.TimestampInterval;

public class RemoveSouthAfricaStationBeginings {

	/**
Station 1: bis einschlie�lich 4.3. l�schen
Station 2: bis einschlie�lich 5.3. l�schen
Station 3: bis einschlie�lich 6.3. l�schen
Station 4: bis einschlie�lich 7.3. l�schen
Station 5: bis einschlie�lich 10.3. l�schen
Station 6: bis einschlie�lich 11.3. l�schen
Station 7: bis einschlie�lich 12.3. l�schen
Station 8: bis einschlie�lich 13.3. l�schen
Station 10: bis einschlie�lich 20.3. l�schen
Station 11: bis einschlie�lich 21.3. l�schen
Station 12: bis einschlie�lich 22.3. l�schen
Station 13: bis einschlie�lich 24.3. l�schen
Station 14: bis einschlie�lich 25.3. l�schen
Station 15: bis einschlie�lich 26.3. l�schen
	 */
	public static void run(TsDB tsdb) {
		final int startTime = 0;

		Object[][] days = new Object[][]{
				{"SA01",4},
				{"SA02",5},
				{"SA03",6},
				{"SA04",7},
				{"SA05",10},
				{"SA06",11},
				{"SA07",12},
				{"SA08",13},
				{"SA10",20},
				{"SA11",21},
				{"SA12",22},
				{"SA13",24},
				{"SA14",25},
				{"SA15",26},
		};

		for(Object[] day:days) {
			String plotID = (String) day[0];
			int endTime = (int) TimeConverter.DateTimeToOleMinutes(LocalDateTime.of(2014, 3, ((Number)day[1]).intValue(), 23, 59));
			System.out.println("remove  "+plotID+"    "+endTime+"  "+TimeConverter.oleMinutesToText(endTime));
			VirtualPlot virtualPlot = tsdb.getVirtualPlot(plotID);
			for(TimestampInterval<StationProperties> entry:virtualPlot.intervalList) {
				tsdb.streamStorage.removeInterval(entry.value.get_serial(), startTime, endTime);
			}
		}
		
		
	}

	public static void main(String[] args) {
		TsDB tsdb = TsDBFactory.createDefault();
		run(tsdb);
		tsdb.close();
	}

}

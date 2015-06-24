package tsdb.util;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;

public class DataEntry {
	public final int timestamp;
	public final float value;

	public DataEntry(int timestamp, float value) {
		this.timestamp = timestamp;
		this.value = value;
	}

	@Override
	public String toString() {
		return timestamp+" "+TimeUtil.oleMinutesToText((long) timestamp)+" "+value;
	}

	private static class TimeSeriesArchivDataEntryArraySerializer implements org.mapdb.Serializer<DataEntry[]> {

		private final static String TOC_START = "DataEntryArray:start";
		private final static String TOC_END = "DataEntryArray:end";

		@Override
		public void serialize(DataOutput out, DataEntry[] dataEntries) throws IOException {
			out.writeUTF(TOC_START);
			DataOutput2.packInt(out, dataEntries.length);
			
			int prevTimestamp = -1;
			for(DataEntry entry:dataEntries) {
				if(entry.timestamp<=prevTimestamp) {
					throw new RuntimeException("write timestampseries format error: timestamps not ascending ordered");
				}
				out.writeInt(entry.timestamp);
				out.writeFloat(entry.value);
				prevTimestamp = entry.timestamp;
			}	

			out.writeUTF(TOC_END);
		}

		@Override
		public DataEntry[] deserialize(DataInput in, int available) throws IOException {
			String toc_start = in.readUTF();
			if(!toc_start.equals(TOC_START)) {
				throw new RuntimeException("file format error found not "+TOC_START+" but "+toc_start);
			}
			final int dataEntrySize = DataInput2.unpackInt(in);
			DataEntry[] dataEntries = new DataEntry[dataEntrySize];
			int prevTimestamp = -1;
			for(int i=0;i<dataEntries.length;i++) {
				int timestamp = in.readInt();
				float value = in.readFloat();
				if(timestamp<=prevTimestamp) {
					throw new RuntimeException("read timestampseries format error: timestamps not ascending ordered");
				}
				dataEntries[i] = new DataEntry(timestamp,value);
				prevTimestamp = timestamp;
			}
			String toc_end = in.readUTF();
			if(!toc_end.equals(TOC_END)) {
				throw new RuntimeException("file format error");
			}
			return dataEntries;
		}

		@Override
		public int fixedSize() {
			return -1;
		}
	}
	
	public static final org.mapdb.Serializer<DataEntry[]> TIMESERIESARCHIV_SERIALIZER = new TimeSeriesArchivDataEntryArraySerializer();
}
package tsdb.remote;

import java.io.Serializable;

import tsdb.Station;
import tsdb.component.LoggerType;
import tsdb.util.Util;

/**
 * Serializable station info
 * @author woellauer
 *
 */
public class StationInfo implements Serializable {

	private static final long serialVersionUID = -5759969271465519328L;
	public final String stationID;
	public final LoggerType loggerType;
	public final double geoPoslongitude;
	public final double geoPosLatitude;
	public final GeneralStationInfo generalStationInfo;
	public final String alternativeID;

	public StationInfo(Station station) {
		stationID = station.stationID;
		loggerType = station.loggerType;
		geoPoslongitude = station.geoPosLongitude;
		geoPosLatitude = station.geoPosLatitude;
		generalStationInfo = Util.ifnull(station.generalStation, gs->new GeneralStationInfo(gs));
		alternativeID = station.alternativeID;
	}		
}
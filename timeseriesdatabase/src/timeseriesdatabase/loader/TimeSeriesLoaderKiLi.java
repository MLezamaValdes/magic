package timeseriesdatabase.loader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import de.umr.jepc.store.Event;
import timeseriesdatabase.Station;
import timeseriesdatabase.StationProperties;
import timeseriesdatabase.TimeConverter;
import timeseriesdatabase.TimeSeriesDatabase;
import timeseriesdatabase.catalog.SourceEntry;
import timeseriesdatabase.raw.ASCTimeSeries;
import timeseriesdatabase.raw.TimestampSeries;
import util.Util;

public class TimeSeriesLoaderKiLi extends TimeSeriesLoader {

	public TimeSeriesLoaderKiLi(TimeSeriesDatabase timeseriesdatabase) {
		super(timeseriesdatabase);
	}
	
	/**
	 * specific to KiLi:
	 * read files contained in subfolders in KiLi folder tree
	 * @param kiliPath
	 */
	public void loadDirectoryOfAllExploratories_structure_kili(Path kiliPath) {
		
		//TreeMap<String,Path> ascCollectorMap = new TreeMap<String,Path>();
		
		
		log.info("loadDirectoryOfAllExploratories_structure_kili:\t"+kiliPath);
		try {
			DirectoryStream<Path> stream = Files.newDirectoryStream(kiliPath);
			for(Path path:stream) {
     			DirectoryStream<Path> subStream = Files.newDirectoryStream(path,"ra*");
				for(Path subPath:subStream) {
					loadOneDirectory_structure_kili(subPath);
				}
			}
		} catch (IOException e) {
			log.error(e);
		}
	}
	
	
	public void loadOneDirectory_structure_kili(Path kiliPath) {
		try {
			if(Files.exists(kiliPath)) {
				DirectoryStream<Path> stream = Files.newDirectoryStream(kiliPath);
				System.out.println("*** load directory: "+kiliPath+" ***");
				TreeMap<String,Path> validFileMap = new TreeMap<String,Path>();
				for(Path path:stream) {
					String filename = path.getName(path.getNameCount()-1).toString();
					int ascIndex = filename.indexOf(".asc");
					if(ascIndex!=-1) {						
						String fileKey = filename.substring(0, ascIndex);						
						validFileMap.putIfAbsent(fileKey, path);
					} else {
						log.warn("no asc file: "+filename);
					}		

					

				}
				
				for(Path ascPath:validFileMap.values()) {
					System.out.println("asc file: "+ascPath);
					
					try{
						ASCTimeSeries csvtimeSeries = new ASCTimeSeries(ascPath);
						TimestampSeries timestampSeries = csvtimeSeries.readEntries();
						long intervalStart = csvtimeSeries.timestampStart;
						long intervalEnd = csvtimeSeries.timestampEnd;
						
						if(timestampSeries!=null) {

							if(!timestampSeries.entryList.isEmpty()) {

								Station station = timeseriesdatabase.getStation(csvtimeSeries.serialnumber);
								if(station!=null) {									
									
									String[] translatedInputSchema = new String[csvtimeSeries.parameterNames.length];
									for(int i=0;i<csvtimeSeries.parameterNames.length;i++) {
										translatedInputSchema[i] = station.translateInputSensorName(csvtimeSeries.parameterNames[i], false);
									}

									//Map<String, Integer> schemaMap = Util.stringArrayToMap(translatedInputSchema,true);

									StationProperties properties = station.getProperties(intervalStart, intervalEnd);

									if(properties!=null) {
										
										insertOneFile(csvtimeSeries,station,properties,translatedInputSchema,timestampSeries);
										
									} else {
										log.warn("no properties found in "+csvtimeSeries.serialnumber+"   "+TimeConverter.oleMinutesToText(intervalStart)+" - "+TimeConverter.oleMinutesToText(intervalEnd));
									}									
								} else {
									log.error("station not found: "+csvtimeSeries.serialnumber+" in "+ascPath);
								}
							} else {
								log.warn("timestampseries is empty");
							}
						} else {
							log.error("no timestampseries: "+csvtimeSeries.filename);
						}

					} catch(Exception e) {
						e.printStackTrace();
						log.error(e+" in "+ascPath);
					}					
					
					
				}
				
				
			} else {
				log.warn("directory not found: "+kiliPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void insertOneFile(ASCTimeSeries csvtimeSeries, Station station, StationProperties properties, String[] translatedInputSchema, TimestampSeries timestampSeries) {
		AbstractLoader loader = LoaderFactory.createLoader(station.loggerType.typeName, translatedInputSchema, properties, csvtimeSeries);
		if(loader!=null) {
			List<Event> eventList = loader.load(station, station.loggerType.sensorNames, timestampSeries);			
			if(eventList!=null) {
				timeseriesdatabase.streamStorage.insertEventList(csvtimeSeries.serialnumber, eventList, csvtimeSeries.timestampStart, csvtimeSeries.timestampEnd);
				String[] resultSchema = loader.getResultSchema();				
				timeseriesdatabase.sourceCatalog.insert(new SourceEntry(csvtimeSeries.filename,csvtimeSeries.serialnumber,csvtimeSeries.timestampStart, csvtimeSeries.timestampEnd,eventList.size(),csvtimeSeries.parameterNames, resultSchema, csvtimeSeries.timeStep));
			} else {
				log.warn("no events inserted: "+csvtimeSeries.filename);
			}			
		} else {
			log.warn("no loader found for logger type: "+station.loggerType.typeName);
		}		
	}
	
	
	
	/**
	 * specific to KiLi:
	 * read files contained in direct subfolders in KiLi folder tree
	 * special sensor name translation with time interval info from inventory
	 * @param kiliPath
	 */
	public void loadOneDirectory_structure_kili_OLD(Path kiliPath) {
		try {
			if(Files.exists(kiliPath)) {
				DirectoryStream<Path> stream = Files.newDirectoryStream(kiliPath);
				System.out.println("*** load directory: "+kiliPath+" ***");
				for(Path path:stream) {
					//System.out.println(path);

					try{
						ASCTimeSeries csvtimeSeries = new ASCTimeSeries(path);
						TimestampSeries timestampSeries = csvtimeSeries.readEntries();
						long intervalStart = csvtimeSeries.timestampStart;
						long intervalEnd = csvtimeSeries.timestampEnd;
						
						if(timestampSeries!=null) {

							if(!timestampSeries.entryList.isEmpty()) {

								Station station = timeseriesdatabase.getStation(csvtimeSeries.serialnumber);
								if(station!=null) {

									StationProperties properties = station.getProperties(intervalStart, intervalEnd);

									
								
								
								

									String[] translatedInputSchema = new String[csvtimeSeries.parameterNames.length];
									for(int i=0;i<csvtimeSeries.parameterNames.length;i++) {
										translatedInputSchema[i] = station.translateInputSensorName(csvtimeSeries.parameterNames[i], false);
									}

									Map<String, Integer> schemaMap = Util.stringArrayToMap(translatedInputSchema,true);
									
									
									
									
									
									
									
									
									//*** begin PLACE_HOLDER_W_R_300_U **** 
									String PLACE_HOLDER_W_R_300_U = "PLACE_HOLDER_W_R_300_U";
									if(schemaMap.containsKey(PLACE_HOLDER_W_R_300_U)) {
										int counter_PLACE_HOLDER_W_R_300_U = 0;
										String[] entries_PLACE_HOLDER_W_R_300_U =  new String[]{"SWDR_300_U", "SWUR_300_U", "LWDR_300_U", "LWUR_300_U"};

										if(station.loggerType.typeName.equals("wxt")) {
											String[] entries_alternative_PLACE_HOLDER_W_R_300_U = new String[]{"LWDR_300_U", "LWUR_300_U", "SWDR_300_U", "SWUR_300_U"};
											long up_to_2011 = TimeConverter.DateTimeToOleMinutes(LocalDateTime.of(2011, 8, 20,23,59));
											//TODO change entries_PLACE_HOLDER_W_R_300_U											
										}


										for(int schmaIndex=0;schmaIndex<translatedInputSchema.length;schmaIndex++) {
											if(translatedInputSchema[schmaIndex]!=null&&translatedInputSchema[schmaIndex].equals(PLACE_HOLDER_W_R_300_U)) {
												if(counter_PLACE_HOLDER_W_R_300_U<entries_PLACE_HOLDER_W_R_300_U.length) {
													translatedInputSchema[schmaIndex] = entries_PLACE_HOLDER_W_R_300_U[counter_PLACE_HOLDER_W_R_300_U];													
												} else {
													log.warn("no real name for column "+PLACE_HOLDER_W_R_300_U+" "+counter_PLACE_HOLDER_W_R_300_U);
													translatedInputSchema[schmaIndex] = null;
												}
												counter_PLACE_HOLDER_W_R_300_U++;
											}
										}
									}
									//*** end PLACE_HOLDER_W_R_300_U **** 


									//*** begin PLACE_HOLDER_RT_NRT_I ***
									String PLACE_HOLDER_RT_NRT_I = "PLACE_HOLDER_RT_NRT_I";
									if(schemaMap.containsKey(PLACE_HOLDER_RT_NRT_I)) {

										String pu2_1_type = properties.getProperty("pu2_1_type");
										String pu2_2_type = properties.getProperty("pu2_2_type");

										String pu2_1_mapping = properties.getProperty("pu2_1_mapping");
										String pu2_2_mapping = properties.getProperty("pu2_2_mapping");



										switch(pu2_1_type) {
										case "rain":
											if(!(pu2_1_mapping.equals("P_RT_NRT_01_I")||pu2_1_mapping.equals("P_RT_NRT_02_I"))) {
												log.warn("mapping for rain unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;
										case "fog":
											if(!(pu2_1_mapping.equals("F_RT_NRT_01_I")||pu2_1_mapping.equals("F_RT_NRT_02_I"))) {
												log.warn("mapping for fog unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;
										case "tf":
											if(!(pu2_1_mapping.equals("T_RT_NRT_01_I")||pu2_1_mapping.equals("T_RT_NRT_02_I"))) {
												log.warn("mapping for tf unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;											
										default:
											log.warn("type unknown: "+pu2_1_type);
										}

										switch(pu2_2_type) {
										case "rain":
											if(!(pu2_2_mapping.equals("P_RT_NRT_01_I")||pu2_2_mapping.equals("P_RT_NRT_02_I"))) {
												log.warn("mapping for rain unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;
										case "fog":
											if(!(pu2_2_mapping.equals("F_RT_NRT_01_I")||pu2_2_mapping.equals("F_RT_NRT_02_I"))) {
												log.warn("mapping for fog unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;
										case "tf":
											if(!(pu2_2_mapping.equals("T_RT_NRT_01_I")||pu2_2_mapping.equals("T_RT_NRT_02_I"))) {
												log.warn("mapping for tf unknown: "+"pu2_1_type: "+pu2_1_type+"   pu2_2_type: "+pu2_2_type+"  "+"pu2_1_mapping: "+pu2_1_mapping+"   pu2_2_mapping: "+pu2_2_mapping);
											}
											break;											
										default:
											log.warn("type unknown: "+pu2_2_type);
										}

										int counter_PLACE_HOLDER_RT_NRT_I = 0;
										for(int schmaIndex=0;schmaIndex<translatedInputSchema.length;schmaIndex++) {
											if(translatedInputSchema[schmaIndex]!=null&&translatedInputSchema[schmaIndex].equals(PLACE_HOLDER_RT_NRT_I)) {
												if(counter_PLACE_HOLDER_RT_NRT_I==0) {
													if(pu2_1_mapping==null) {
														log.warn("no mapping for "+translatedInputSchema[schmaIndex]+" in "+path);
													}
													translatedInputSchema[schmaIndex] = pu2_1_mapping;
												} else if(counter_PLACE_HOLDER_RT_NRT_I==1) {
													if(pu2_2_mapping==null) {
														log.warn("no mapping for "+translatedInputSchema[schmaIndex]+" in "+path);
													}
													translatedInputSchema[schmaIndex] = pu2_2_mapping;
												} else {
													log.warn("no real name for column "+PLACE_HOLDER_RT_NRT_I+" "+counter_PLACE_HOLDER_RT_NRT_I);
													translatedInputSchema[schmaIndex] = null;
												}
												counter_PLACE_HOLDER_RT_NRT_I++;
											}
										}

										/*int counter_PLACE_HOLDER_RT_NRT_I = 0;
										String[] entries_PLACE_HOLDER_RT_NRT_I = new String[]{"P_RT_NRT_01_I","P_RT_NRT_02_I"};
										for(int schmaIndex=0;schmaIndex<translatedInputSchema.length;schmaIndex++) {
											if(translatedInputSchema[schmaIndex]!=null&&translatedInputSchema[schmaIndex].equals(PLACE_HOLDER_RT_NRT_I)) {
												if(counter_PLACE_HOLDER_RT_NRT_I<entries_PLACE_HOLDER_RT_NRT_I.length) {
													translatedInputSchema[schmaIndex] = entries_PLACE_HOLDER_RT_NRT_I[counter_PLACE_HOLDER_RT_NRT_I];	
												} else {
													log.warn("no real name for column "+PLACE_HOLDER_RT_NRT_I+" "+counter_PLACE_HOLDER_RT_NRT_I);
													translatedInputSchema[schmaIndex] = null;
												}
												counter_PLACE_HOLDER_RT_NRT_I++;
											}
										}*/
									}
									//*** end PLACE_HOLDER_RT_NRT_I ***
									
									
									//*** begin PLACE_HOLDER_RAD ***									
									final String PLACE_HOLDER_RAD = "PLACE_HOLDER_RAD";
									if(schemaMap.containsKey("PLACE_HOLDER_RAD")) {
										String serial_PYR01_name = properties.getProperty("SERIAL_PYR01");
										String serial_PYR02_name = properties.getProperty("SERIAL_PYR02");
										String serial_PAR01_name = properties.getProperty("SERIAL_PAR01");
										String serial_PAR02_name = properties.getProperty("SERIAL_PAR02");

										String[] PLACE_HOLDER_RAD_entries = new String[]{"par_01","par_02","par_03","par_04","par_05","par_06","par_07","par_08","par_09",
												"par_10","par_11","par_12","swdr_13","swdr_14","swdr_15","swdr_16","swdr_17","swdr_18",
												"swdr_19","swdr_20","swdr_21","swdr_22","swdr_23","swdr_24"};

										String radxMapping_1 = null;
										if(!serial_PYR01_name.equals("NaN")) {
											try {
												int index = Integer.parseInt(serial_PYR01_name);
												radxMapping_1 = PLACE_HOLDER_RAD_entries[index-1];
											} catch(Exception e) {
												log.warn(e);
											}
										}
										if(!serial_PAR01_name.equals("NaN")) {
											if(radxMapping_1==null) {
												try {
													int index = Integer.parseInt(serial_PAR01_name);
													radxMapping_1 = PLACE_HOLDER_RAD_entries[index-1];
												} catch(Exception e) {
													log.warn(e);
												}											
											} else {
												log.warn("radxMapping_1 already set");
											}
										}
										
										String radxMapping_2 = null;
										if(!serial_PYR02_name.equals("NaN")) {
											try {
												int index = Integer.parseInt(serial_PYR02_name);
												radxMapping_2 = PLACE_HOLDER_RAD_entries[index-1];
											} catch(Exception e) {
												log.warn(e);
											}
										}
										if(!serial_PAR02_name.equals("NaN")) {
											if(radxMapping_2==null) {
												try {
													int index = Integer.parseInt(serial_PAR02_name);
													radxMapping_2 = PLACE_HOLDER_RAD_entries[index-1];
												} catch(Exception e) {
													log.warn(e);
												}											
											} else {
												log.warn("radxMapping_2 already set");
											}
										}


										log.warn("SERIAL_PYR01: "+serial_PYR01_name+"  SERIAL_PYR02: "+serial_PYR02_name+"    SERIAL_PAR01: "+serial_PAR01_name+"  SERIAL_PAR02: "+serial_PAR02_name+" mapping: "+radxMapping_1+"  "+radxMapping_2);
										
										
										int counter_PLACE_HOLDER_RAD = 0;
										for(int schmaIndex=0;schmaIndex<translatedInputSchema.length;schmaIndex++) {
											if(translatedInputSchema[schmaIndex]!=null&&translatedInputSchema[schmaIndex].equals(PLACE_HOLDER_RAD)) {
												if(counter_PLACE_HOLDER_RAD==0) {
													if(radxMapping_1==null) {
														log.warn("no mapping for "+translatedInputSchema[schmaIndex]+" in "+path);
													}
													translatedInputSchema[schmaIndex] = radxMapping_1;
												} else if(counter_PLACE_HOLDER_RAD==1) {
													if(radxMapping_2==null) {
														log.warn("no mapping for "+translatedInputSchema[schmaIndex]+" in "+path);
													}
													translatedInputSchema[schmaIndex] = radxMapping_2;
												} else {
													log.warn("no real name for column "+PLACE_HOLDER_RAD+" "+counter_PLACE_HOLDER_RAD);
													translatedInputSchema[schmaIndex] = null;
												}
												counter_PLACE_HOLDER_RAD++;
											}											
										}
									}
									//*** end PLACE_HOLDER_RAD ***


									TreeSet<String> duplicates = Util.getDuplicateNames(translatedInputSchema,true);
									if(!duplicates.isEmpty()) {
										log.warn("duplicates: "+duplicates+" in "+path);
									}

									String debugInfo = station.loggerType.toString();
									List<Event> eventList = csvtimeSeries.toEvents(timestampSeries, translatedInputSchema, station.loggerType.sensorNames, debugInfo);

									if(eventList!=null) {							
										timeseriesdatabase.streamStorage.insertEventList(csvtimeSeries.serialnumber, eventList, csvtimeSeries.timestampStart, csvtimeSeries.timestampEnd);										
										timeseriesdatabase.sourceCatalog.insert(new SourceEntry(path,csvtimeSeries.serialnumber,csvtimeSeries.timestampStart, csvtimeSeries.timestampEnd,eventList.size(),csvtimeSeries.parameterNames, translatedInputSchema, csvtimeSeries.timeStep));
									} else {
										log.warn("no events inserted: "+path);
									}
								} else {
									log.error("station not found: "+csvtimeSeries.serialnumber+" in "+path);
								}
							} else {
								log.warn("timestampseries is empty");
							}
						} else {
							log.error("no timestampseries");
						}

					} catch(Exception e) {
						e.printStackTrace();
						log.error(e+" in "+path);
					}

				}
			} else {
				log.warn("directory not found: "+kiliPath);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

}

package tsdb.util;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Wini;
import org.ini4j.Profile.Section;

import com.sun.corba.se.spi.orbutil.fsm.Guard.Result;

import tsdb.Sensor;
import tsdb.Station;
import tsdb.StationProperties;
import tsdb.TimeConverter;

/**
 * Some utilities
 * @author woellauer
 *
 */
public class Util {

	/**
	 * Default logger
	 */
	public static final Logger log = LogManager.getLogger("general");

	/**
	 * convert float to String with two fractional digits
	 * @param value
	 * @return
	 */
	public static String floatToString(float value) {
		if(Float.isNaN(value)) {
			return " --- ";
		}
		return String.format("%.2f", value);
	}

	public static String doubleToString(double value) {
		if(Double.isNaN(value)) {
			return " --- ";
		}
		return String.format("%.2f", value);
	}
	
	public static Map<String,Integer> stringArrayToMap(String[] entries) {
		return stringArrayToMap(entries, false);
	}

	/**
	 * create position map of array of Strings:
	 * name -> array position
	 * @param entries
	 * @return
	 */
	public static Map<String,Integer> stringArrayToMap(String[] entries, boolean ignoreNull) {
		Map<String,Integer> map = new HashMap<String,Integer>();
		if(entries==null) {
			throw new RuntimeException("StringArrayToMap: entries==null");
		}
		for(int i=0;i<entries.length;i++) {
			if(entries[i]==null) {
				if(!ignoreNull) {
					log.warn("StringArrayToMap: entries["+i+"]==null ==> will not be included in map");				
				}
			} else {
				map.put(entries[i], i);
			}
		}
		return map;
	}

	/**
	 * create an array of positions of entries in resultNames with positions of sourcePosStringArray
	 * @param resultNames
	 * @param sourcePosStringArray
	 * @param warn warn if entry was not found in sourcePosStringArray
	 * @return
	 */
	public static int[] stringArrayToPositionIndexArray(String resultNames[], String[] sourcePosStringArray, boolean warn, boolean exception) {
		return stringArrayToPositionIndexArray(resultNames,stringArrayToMap(sourcePosStringArray), warn, exception);
	}

	/**
	 * create an array of positions of entries in resultNames with positions of sourcePosStringMap
	 * @param resultNames
	 * @param sourcePosStringMap
	 * @param warn warn if entry was not found in sourcePosStringMap
	 * @return
	 */
	public static int[] stringArrayToPositionIndexArray(String resultNames[], Map<String,Integer> sourcePosStringMap, boolean warn, boolean exception) {
		int[] sourcePos = new int[resultNames.length];
		for(int i=0;i<resultNames.length;i++) {
			Integer pos = sourcePosStringMap.get(resultNames[i]);
			if(pos==null) {
				if(warn) {
					log.warn("stringArrayToPositionIndexArray: "+resultNames[i]+" not in "+sourcePosStringMap);
				}
				if(exception) {
					throw new RuntimeException("stringArrayToPositionIndexArray: "+resultNames[i]+" not in "+sourcePosStringMap);
				}
				sourcePos[i] = -1;
			} else {
				sourcePos[i] = pos;
			}		
		}
		return sourcePos;
	}

	/**
	 * print array of values in one line
	 * @param a
	 */
	public static void printArray(double[] a) {
		for(int i=0;i<a.length;i++) {
			System.out.format("%.2f  ",a[i]);
		}
		System.out.println();
	}

	public static void printArray(float[] a) {
		for(int i=0;i<a.length;i++) {
			System.out.format("%.2f  ",a[i]);
		}
		System.out.println();
	}

	public static String arrayToString(float[] a) {
		String result="";
		for(int i=0;i<a.length;i++) {
			result+=floatToString(a[i])+" ";
		}
		return result;
	}

	public static void printArray(String[] a) {
		printArray(a," ");
	}

	/**
	 * print array of values in one line
	 * @param a
	 */
	public static void printArray(String[] a,String sep) {
		for(int i=0;i<a.length;i++) {
			System.out.print(a[i]+sep);
		}
		System.out.println();
	}

	/**
	 * named range of float values
	 * @author woellauer
	 *
	 */
	public static class FloatRange {
		public final String name;
		public final float min;
		public final float max;
		public FloatRange(String name, float min, float max) {
			this.name = name;
			this.min = min;
			this.max = max;
		}
	}

	/**
	 * Reads a list of range float values from ini-file section
	 * @param fileName
	 * @param sectionName
	 * @return
	 */
	public static List<FloatRange> readIniSectionFloatRange(String fileName, String sectionName) {
		try {
			Wini ini = new Wini(new File(fileName));
			Section section = ini.get(sectionName);
			if(section!=null) {
				ArrayList<FloatRange> resultList = new ArrayList<FloatRange>();
				for(Entry<String, String> entry:section.entrySet()) {
					String name = entry.getKey();
					String range = entry.getValue();
					try {
						String minString = range.substring(range.indexOf('[')+1, range.indexOf(','));
						String maxString = range.substring(range.indexOf(',')+2, range.indexOf(']'));
						float min=Float.parseFloat(minString);
						float max=Float.parseFloat(maxString);
						resultList.add(new FloatRange(name, min, max));
					} catch (Exception e) {
						log.warn("error in read: "+name+"\t"+range+"\t"+e);
					}
				}
				return resultList;
			} else {
				log.error("ini section not found: "+sectionName);
				return null;
			}
		} catch (Exception e) {
			log.error("ini file not read "+fileName+"\t"+sectionName+"\t"+e);
			return null;
		}
	}

	public static <T> ArrayList<T> iteratorToList(Iterator<T> it) {
		ArrayList<T> resultList = new ArrayList<T>();
		while(it.hasNext()) {
			resultList.add(it.next());
		}
		return resultList;
	}

	public static <T> List<T> createList(List<T> list, T e) {
		ArrayList<T> result = new ArrayList<T>(list.size()+1);
		result.addAll(list);
		result.add(e);
		return result;
	}

	public static <T> T  ifnull(T a, T isNull){
		if(a==null) {
			return isNull;
		} else {
			return a;
		}
	}

	/**
	 * return result of func if a is not null else null
	 * @param a
	 * @param func
	 * @return
	 */
	public static <A, B> B  ifnull(A a, Function<A,B> func){
		if(a==null) {
			return null;
		} else {
			return func.apply(a);
		}
	}

	/**
	 * return result of funcArg if a is not null else result of funcNull
	 * @param a
	 * @param funcArg
	 * @param funcNull
	 * @return
	 */
	public static <A, B> B  ifnull(A a, Function<A,B> funcArg, Supplier<B> funcNull){
		if(a==null) {
			return funcNull.get();
		} else {
			return funcArg.apply(a);
		}
	}
	
	public static <A, B> B  ifnull(A a, Function<A,B> funcArg, B nullValue){
		if(a==null) {
			return nullValue;
		} else {
			return funcArg.apply(a);
		}
	}

	public static String bigNumberToString(long n) {
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
		formatSymbols.setGroupingSeparator('\'');
		DecimalFormat df = (DecimalFormat) DecimalFormat.getIntegerInstance();
		df.setGroupingSize(3);
		df.setDecimalFormatSymbols(formatSymbols);
		return df.format(n);
	}

	public static int getIndexInArray(String text, String array[]) {
		for(int i=0;i<array.length;i++) {
			if(array[i].equals(text)) {
				return i;
			}
		}
		return -1;
	}

	public static <S, T> int fillArray(Collection<S> collection, T[] array, Function<S,T> transform) {
		return fillArray(collection.iterator(),array,transform);
	}

	public static <S, T> int fillArray(Iterator<S> input_iterator, T[] array, Function<S,T> transform) {
		Iterator<T> it = new Iterator<T>(){
			@Override
			public boolean hasNext() {
				return input_iterator.hasNext();
			}
			@Override
			public T next() {
				return transform.apply(input_iterator.next());
			}
		};
		return fillArray(it,array);
	}

	public static <T> int fillArray(Collection<T> collection, T[] array) {
		return fillArray(collection.iterator(),array);
	}

	public static <T> int fillArray(Iterator<T> input_iterator, T[] array) {
		int counter=0;
		while(input_iterator.hasNext()) {
			if(counter>=array.length) {
				return counter;
			}
			array[counter]=input_iterator.next();
			counter++;
		}
		return counter;
	}

	public static Float[] array_float_to_array_Float(float[] a) {
		Float[] result = new Float[a.length];
		for(int i=0;i<a.length;i++) {
			result[i] = a[i];
		}
		return result;
	}

	/**
	 * creates a map of all entries in one section of an "ini"-file
	 * @param section
	 * @return
	 */
	public static Map<String, String> readIniSectionMap(Section section) {
		Map<String,String> sectionMap = new HashMap<String, String>();
		for(String key:section.keySet()) {
			if(!key.equals("NaN")) {
				sectionMap.put(key, section.get(key));
			}
		}
		return sectionMap;
	}

	/**
	 * 
	 * @param array nullable
	 * @return
	 */
	public static String arrayToString(String[] array) {
		if(array==null) {
			return "[null]";
		}
		String result = "";
		for(String s:array) {
			result+=s+" ";
		}
		return result;
	}
	
	public static String arrayToString(int[] array) {
		String result = "";
		for(int s:array) {
			result+=s+" ";
		}
		return result;
	}

	public static TreeSet<String> getDuplicateNames(String[] schema, boolean ignorNull) {
		TreeSet<String> resultSet = new TreeSet<String>();		
		Set<String> names = new HashSet<String>();		
		for(String name:schema) {
			if(!ignorNull||name!=null) {
				if(!names.contains(name)) {
					names.add(name);
				} else {
					resultSet.add(name);
				}
			}
		}	
		return resultSet;
	}
	
	public String ifNaN(float value, String text) {
		if(Float.isNaN(value)) {
			return text;
		} else {
			return ""+value;
		}
	}
	
	public static String ifNaN(double value, String text) {
		if(Double.isNaN(value)) {
			return text;
		} else {
			return ""+value;
		}
	}
	
	public static boolean containsString(String[] array, String text) {
		for(String s:array) {
			if(s.equals(text)) {
				return true;
			}
		}
		return false;
	}
	
	public static String[] getValidEntries(String[] names, String[] source) {
		Map<String, Integer> sourceMap = Util.stringArrayToMap(source);
		return Arrays.asList(names).stream().filter(name->sourceMap.containsKey(name)).toArray(String[]::new);
	}
	
	public static boolean isContained(String[] names, String[] source) {
		Map<String, Integer> sourceMap = Util.stringArrayToMap(source);
		for(String name:names) {
			if(!sourceMap.containsKey(name)) {
				return false;
			}
		}
		return true;
	}
	
	public static void throwNull(Object ... o) {
		for(Object x:o) {
			if(x==null) {
				throw new RuntimeException("null");
			}
		}
	}
	
	public static <T> ArrayList<T> streamToList(Stream<T> stream) {
		return (ArrayList<T>) stream.collect(Collectors.toList());
	}
}
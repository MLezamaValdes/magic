package tsdb.explorer;

import static tsdb.util.AssumptionCheck.throwNull;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tsdb.DataQuality;
import tsdb.Region;
import tsdb.Sensor;
import tsdb.SensorCategory;
import tsdb.TimeConverter;
import tsdb.aggregated.AggregationInterval;
import tsdb.raw.TimestampSeries;
import tsdb.remote.GeneralStationInfo;
import tsdb.remote.PlotInfo;
import tsdb.remote.RemoteTsDB;
import tsdb.util.gui.TimeSeriesDiagram;
import tsdb.util.gui.TimeSeriesHeatMap;
import tsdb.util.gui.TimeSeriesPainterGraphics2D;

public class TimeSeriesMultiViewScene extends TsdbScene {
	private static final Logger log = LogManager.getLogger();	
	private final RemoteTsDB tsdb;

	private static final Region regionAll = new Region("[all]","[all]");
	private static final GeneralStationInfo GeneralStationAll = new GeneralStationInfo("[all]", "[?]");
	private static final PlotInfo plotAll = new PlotInfo("[all]", "[?]", "[?]");
	private static final Sensor sensorAll = new Sensor("[all]");
	private static final String timeAll = "[all]";

	private ComboBox<Region> comboRegion;
	private ComboBox<GeneralStationInfo> comboGeneralStation;
	private ComboBox<PlotInfo> comboPlot;
	private ComboBox<Sensor> comboSensor;
	private SimpleStringProperty selectedCountProperty;

	private ArrayList<QueryEntry> queryList = new ArrayList<QueryEntry>();
	private ArrayList<QueryEntry> selectionQueryList = new ArrayList<QueryEntry>();
	private ArrayList<ScreenImageEntry> screenImageList = new ArrayList<ScreenImageEntry>();

	private ScrollBar scrollBar;
	private VBox vboxQueryImages;
	private BorderPane borderPaneDiagrams;


	private ExecutorService executorQueryTimeSeries;
	private ExecutorService executorDrawImages;
	private ComboBox<String> comboTime;
	private HashMap<String, Sensor> sensorMap;

	protected TimeSeriesMultiViewScene(RemoteTsDB tsdb) {
		super("time series multi view");
		throwNull(tsdb);
		this.tsdb = tsdb;



	}

	@Override
	protected Parent createContent() {
		BorderPane borderPaneMain = new BorderPane();


		Label labelRegion = new Label("Region");
		labelRegion.setAlignment(Pos.CENTER);
		labelRegion.setMaxHeight(100d);
		comboRegion = new ComboBox<Region>();
		StringConverter<Region> regionConverter = new StringConverter<Region>() {			
			@Override
			public String toString(Region region) {
				return region.longName;
			}			
			@Override
			public Region fromString(String string) {
				return null;
			}
		};
		comboRegion.setConverter(regionConverter);
		comboRegion.valueProperty().addListener(this::onRegionChanged);

		Label labelGeneralStation = new Label("General");
		labelGeneralStation.setAlignment(Pos.CENTER);
		labelGeneralStation.setMaxHeight(100d);
		comboGeneralStation = new ComboBox<GeneralStationInfo>();
		StringConverter<GeneralStationInfo> generalStationConverter = new StringConverter<GeneralStationInfo>() {			
			@Override
			public String toString(GeneralStationInfo general) {
				return general.longName;
			}			
			@Override
			public GeneralStationInfo fromString(String string) {
				return null;
			}
		};
		comboGeneralStation.setConverter(generalStationConverter);
		comboGeneralStation.valueProperty().addListener(this::onGeneralStationChanged);

		Label labelPlot = new Label("Plot");
		labelPlot.setAlignment(Pos.CENTER);
		labelPlot.setMaxHeight(100d);
		comboPlot = new ComboBox<PlotInfo>();
		StringConverter<PlotInfo> plotConverter = new StringConverter<PlotInfo>() {			
			@Override
			public String toString(PlotInfo plot) {
				return plot.name;
			}			
			@Override
			public PlotInfo fromString(String string) {
				return null;
			}
		};
		comboPlot.setConverter(plotConverter);
		comboPlot.valueProperty().addListener(this::onPlotChanged);

		Label labelSensor = new Label("Sensor");
		labelSensor.setAlignment(Pos.CENTER);
		labelSensor.setMaxHeight(100d);
		comboSensor = new ComboBox<Sensor>();
		StringConverter<Sensor> sensorConverter = new StringConverter<Sensor>() {			
			@Override
			public String toString(Sensor sensor) {
				return sensor.name;
			}			
			@Override
			public Sensor fromString(String string) {
				return null;
			}
		};
		comboSensor.setConverter(sensorConverter);
		comboSensor.valueProperty().addListener(this::onSensorChanged);
		
		
		Label labelTime = new Label("Time");
		labelTime.setAlignment(Pos.CENTER);
		labelTime.setMaxHeight(100d);
		comboTime = new ComboBox<String>();
		comboTime.valueProperty().addListener(this::onTimeChanged);
		
		
		
		
		
		

		FlowPane controlPane = new FlowPane(10d,10d);

		controlPane.getChildren().add(new HBox(10d,labelRegion,comboRegion,new Separator(Orientation.VERTICAL)));
		controlPane.getChildren().add(new HBox(10d,labelGeneralStation,comboGeneralStation,new Separator(Orientation.VERTICAL)));
		controlPane.getChildren().add(new HBox(10d,labelPlot,comboPlot,new Separator(Orientation.VERTICAL)));
		controlPane.getChildren().add(new HBox(10d,labelSensor,comboSensor,new Separator(Orientation.VERTICAL)));
		controlPane.getChildren().add(new HBox(10d,labelTime,comboTime,new Separator(Orientation.VERTICAL)));

		Label labelSelectedCount = new Label();
		selectedCountProperty = new SimpleStringProperty();
		selectedCountProperty.set("?");
		labelSelectedCount.textProperty().bind(selectedCountProperty);
		Button buttonSetCurrent = new Button("set to current");

		buttonSetCurrent.setOnAction(this::onSetCurrent);

		Button buttonAddCurrent = new Button("add to current");
		buttonAddCurrent.setDisable(true);
		controlPane.getChildren().addAll(labelSelectedCount, new Label("selected"),new Separator(Orientation.VERTICAL),new Label(" --> selection"),buttonSetCurrent,buttonAddCurrent);
		borderPaneMain.setTop(controlPane);
		/*vboxQueryImages = new VBox();
		vboxQueryImages.setAlignment(Pos.TOP_CENTER);
		scrollPane = new ScrollPane();
		scrollPane.setContent(vboxQueryImages);
		borderPaneMain.setCenter(scrollPane);*/

		borderPaneDiagrams = new BorderPane();
		scrollBar = new ScrollBar();
		scrollBar.setOrientation(Orientation.VERTICAL);
		scrollBar.valueProperty().addListener(this::updateScreen);
		borderPaneDiagrams.setRight(scrollBar);
		vboxQueryImages = new VBox();

		vboxQueryImages.widthProperty().addListener(this::onResize);
		vboxQueryImages.heightProperty().addListener(this::onResize);


		borderPaneDiagrams.setCenter(vboxQueryImages);
		borderPaneMain.setCenter(borderPaneDiagrams);
		
		
		

		return borderPaneMain;
	}

	private static final double imageHeight = 100;
	private static final int imageviewCount = 11;

	private void onResize(Observable observable) {
		double width = vboxQueryImages.getWidth();
		double height = vboxQueryImages.getHeight();
		System.out.println("resize vboxQueryImages "+width+"  "+height);

		vboxQueryImages.getChildren().clear();
		for(ScreenImageEntry s:screenImageList) {
			s.setQueryEntry(null);
		}
		screenImageList.clear();

		int imageCount = imageviewCount;//(int) Math.ceil(height/imageHeight);
		for(int i=0;i<imageCount;i++) {

			ScreenImageEntry sie = new ScreenImageEntry(new ImageView());
			screenImageList.add(sie);
			vboxQueryImages.getChildren().add(sie.imageView);			
		}

	}

	private void updateScreen(Observable observable) {
		int screenPos = (int) scrollBar.getValue();
		for(int i=0;i<screenImageList.size();i++) {
			int entryPos = screenPos+i;
			if(entryPos<queryList.size()) {				
				QueryEntry queryEntry = queryList.get(entryPos);
				screenImageList.get(i).setQueryEntry(queryEntry);
			} else {
				screenImageList.get(i).setQueryEntry(null);
			}
		}
	}


	static class PriorityThreadFactory implements ThreadFactory { // source from Executors.defaultThreadFactory();
		private static final AtomicInteger poolNumber = new AtomicInteger(1);
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		private final int priority;

		PriorityThreadFactory(int priority) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() :
				Thread.currentThread().getThreadGroup();
			namePrefix = "pool-" +
					poolNumber.getAndIncrement() +
					"-thread-";
			this.priority = priority;
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r,
					namePrefix + threadNumber.getAndIncrement(),
					0);
			if (t.isDaemon())
				t.setDaemon(false);
			/*if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);*/
			t.setPriority(priority);
			return t;
		}
	}

	static ThreadPoolExecutor createPriorityExecutor(int nThreads, int priority) {
		int corePoolSize = nThreads;
		int maximumPoolSize = nThreads;
		long keepAliveTime = 0L;
		TimeUnit unit = TimeUnit.MILLISECONDS;
		BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
		//ThreadFactory threadFactory = Executors.defaultThreadFactory();
		ThreadFactory threadFactory = new PriorityThreadFactory(priority);
		/*ThreadFactory threadFactory = new ThreadFactory() {			
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r);
				thread.setPriority(Thread.MIN_PRIORITY);
				return null;
			}
		};*/
		RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
		return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler);

	}
	
	


	private void onSetCurrent(ActionEvent event) {

		queryList = selectionQueryList;
		scrollBar.setValue(0);
		scrollBar.setMin(0);
		int scrollMax = queryList.size()-imageviewCount;
		if(scrollMax<0) {
			scrollMax = 0;
		}
		scrollBar.setMax(scrollMax);
		scrollBar.setVisibleAmount(imageviewCount);

		updateScreen(null);

		if(executorQueryTimeSeries!=null) {			
			executorQueryTimeSeries.shutdownNow();			
		}
		//executorQueryTimeSeries = Executors.newWorkStealingPool();
		//executorQueryTimeSeries = createPriorityExecutor(Thread.MIN_PRIORITY);
		executorQueryTimeSeries = createPriorityExecutor(2,Thread.MIN_PRIORITY);




		if(executorDrawImages!=null) {		
			executorDrawImages.shutdownNow();		
		}
		//executorDrawImages = Executors.newWorkStealingPool();
		executorDrawImages = createPriorityExecutor(2,Thread.MIN_PRIORITY+1);


		for(QueryEntry queryEntry:queryList) {

			queryEntry.timestampSeriesProperty.addListener((s,o,ts)->{
				if(ts!=null) {
					System.out.println("************************** add task create image "+queryEntry.plotID+"  "+queryEntry.sensor.name);
					executorDrawImages.submit(()->{
						System.out.println("************************** create image "+queryEntry.plotID+"  "+queryEntry.sensor.name);


						BufferedImage bufferedImage = new BufferedImage((int)borderPaneDiagrams.getWidth()-30,(int)imageHeight,java.awt.image.BufferedImage.TYPE_INT_RGB);
						Graphics2D gc = bufferedImage.createGraphics();
						gc.setBackground(new java.awt.Color(255, 255, 255));
						gc.setColor(new java.awt.Color(0, 0, 0));
						gc.clearRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
						gc.dispose();
						TimeSeriesPainterGraphics2D tsp = new TimeSeriesPainterGraphics2D(bufferedImage);
						TimeSeriesDiagram tsd = new TimeSeriesDiagram(ts,AggregationInterval.HOUR,queryEntry.sensor.category);						
						tsd.draw(tsp);
						
						gc = bufferedImage.createGraphics();					
						gc.setColor(java.awt.Color.LIGHT_GRAY);
						gc.drawString(queryEntry.plotID+" : "+queryEntry.sensor.name, 42, 20);
						gc.dispose();
						
						
						
						/*TimeSeriesHeatMap tshm = new TimeSeriesHeatMap(ts);
						TimeSeriesPainterGraphics2D tsp = new TimeSeriesPainterGraphics2D(bufferedImage);
						tshm.draw(tsp, queryEntry.sensorName);*/
						WritableImage image = SwingFXUtils.toFXImage(bufferedImage, null);

						Platform.runLater(()->queryEntry.imageProperty.set(image));						


					});
				}
			});



			System.out.println("************************** add task query TimestampSeries"+queryEntry.plotID+"  "+queryEntry.sensor.name);
			executorQueryTimeSeries.submit(()->{
				System.out.println("************************** query TimestampSeries"+queryEntry.plotID+"  "+queryEntry.sensor.name);
				try {
					TimestampSeries ts = tsdb.plot(null, queryEntry.plotID, new String[]{queryEntry.sensor.name}, AggregationInterval.HOUR, DataQuality.STEP, false, queryEntry.startTimestamp, queryEntry.endTimestamp);
					if(ts!=null) {
						Platform.runLater(()->queryEntry.timestampSeriesProperty.set(ts));
					}
				} catch (Exception e) {
					log.error(e);
				}
			});
		}



		/*vboxQueryImages.getChildren().clear();

		System.out.println("vboxQueryImages "+vboxQueryImages.getWidth()+"  "+vboxQueryImages.getHeight());

		ExecutorService executor = Executors.newWorkStealingPool();

		for(QueryEntry queryEntry:queryList) {


			ImageView imageView = new ImageView();
			vboxQueryImages.getChildren().add(imageView);

			executor.submit(()->{

				System.out.println("get "+queryEntry.plotID+"  "+queryEntry.sensorName);
				//vboxQueryImages.getChildren().add(new Button(queryEntry.plotID+"  "+queryEntry.sensorName));
				try {			
					TimestampSeries ts = tsdb.plot(null, queryEntry.plotID, new String[]{queryEntry.sensorName}, AggregationInterval.HOUR, DataQuality.STEP, false, null, null);
					if(ts!=null) {
						TimeSeriesDiagram tsd = new TimeSeriesDiagram(ts,AggregationInterval.HOUR,SensorCategory.TEMPERATURE);

						try {
							BufferedImage bufferedImage = new BufferedImage((int)borderPaneDiagrams.getWidth()-30,(int)200,java.awt.image.BufferedImage.TYPE_INT_RGB);
							Graphics2D gc = bufferedImage.createGraphics();
							gc.setBackground(new java.awt.Color(255, 255, 255));
							gc.setColor(new java.awt.Color(0, 0, 0));
							gc.clearRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
							gc.dispose();
							TimeSeriesPainterGraphics2D tsp = new TimeSeriesPainterGraphics2D(bufferedImage);
							tsd.draw(tsp);
							WritableImage image = SwingFXUtils.toFXImage(bufferedImage, null);

							Platform.runLater(()->imageView.setImage(image));


						} catch (Exception e) {
							e.printStackTrace();

						}


					}
				} catch (Exception e) {
					log.error(e);
				}
			});			
		}*/


	}



	@Override
	protected void onShown() {
		ObservableList<String> timeList = FXCollections.observableArrayList();		
		timeList.add(timeAll);
		timeList.add("2008");
		timeList.add("2009");
		timeList.add("2010");
		timeList.add("2011");
		timeList.add("2012");
		timeList.add("2013");
		timeList.add("2014");		
		comboTime.setItems(timeList);
		comboTime.setValue(timeAll);
		
		try {
			Sensor[] sensors = tsdb.getSensors();
			sensorMap = new HashMap<String,Sensor>();
			for(Sensor sensor:sensors) {
				sensorMap.put(sensor.name,sensor);
			}			
		} catch (RemoteException e) {
			log.error(e);
		}
		
		setRegions();
	}

	private void setRegions() {
		ObservableList<Region> regionList = FXCollections.observableArrayList();
		try {
			Region[] regions = tsdb.getRegions();
			regionList.add(regionAll);
			regionList.addAll(regions);
		} catch (RemoteException e) {
			log.error(e);
		}

		comboRegion.setItems(regionList);
		comboRegion.setValue(regionAll);		
	}

	private void onRegionChanged(ObservableValue<? extends Region> observable, Region oldValue, Region region) {
		ObservableList<GeneralStationInfo> generalStationList = FXCollections.observableArrayList();

		try {
			GeneralStationInfo[] g = tsdb.getGeneralStations();
			if(region==null||region.name.equals("[all]")) {
				generalStationList.add(GeneralStationAll);
				generalStationList.addAll(g);
			} else {
				generalStationList.add(GeneralStationAll);
				for(GeneralStationInfo s:g) {
					if(s.region.name.equals(region.name)) {
						generalStationList.add(s);
					}
				}
			}
		} catch (Exception e) {
			log.error(e);
		}



		comboGeneralStation.setItems(generalStationList);
		comboGeneralStation.setValue(GeneralStationAll);
	}

	private void onGeneralStationChanged(ObservableValue<? extends GeneralStationInfo> observable, GeneralStationInfo oldValue, GeneralStationInfo general) {
		ObservableList<PlotInfo> plotList = FXCollections.observableArrayList();
		try {
			PlotInfo[] plotInfos = tsdb.getPlots();
			if(general==null||general.name.equals("[all]")) {
				Region region = comboRegion.getValue();
				if(region==null||region.name.equals("[all]")) {
					plotList.add(plotAll);
					plotList.addAll(plotInfos);
				} else {
					plotList.add(plotAll);
					for(PlotInfo plot:plotInfos) {
						if(plot.generalStationInfo.region.name.equals(region.name)) {
							plotList.add(plot);
						}
					}
				}
			} else {
				plotList.add(plotAll);
				for(PlotInfo plot:plotInfos) {
					if(plot.generalStationInfo.name.equals(general.name)) {
						plotList.add(plot);
					}
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		comboPlot.setItems(plotList);
		/*if(plotList.isEmpty()) {
			comboPlot.setValue(null);
		} else {
			comboPlot.setValue(plotList.get(0));
		}*/
		comboPlot.setValue(plotAll);
	}



	private void onPlotChanged(ObservableValue<? extends PlotInfo> observable, PlotInfo oldValue, PlotInfo plot) {
		ObservableList<Sensor> sensorList = FXCollections.observableArrayList();
		System.out.println("plot");
		try {
			Sensor[] sensors = tsdb.getSensors();
			Map<String,Sensor> sensorMap = new HashMap<String,Sensor>();
			for(Sensor sensor:sensors) {
				sensorMap.put(sensor.name, sensor);
			}

			if(plot==null||plot.name.equals(plotAll.name)) {
				if(comboPlot.getItems()!=null) {
					TreeSet<String> sensorSet = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
					for(PlotInfo item:comboPlot.getItems()) {
						if(!item.name.equals(plotAll.name)) {
							String[] plotSensorNames = tsdb.getSensorNamesOfPlot(item.name);
							if(plotSensorNames!=null) {
								sensorSet.addAll(Arrays.asList(plotSensorNames));
							}
						}
					}
					sensorList.add(sensorAll);
					for(String sensorName:sensorSet) {
						Sensor sensor = sensorMap.get(sensorName);
						if(sensor.isAggregable()) {
							sensorList.add(sensor);
						}
					}	
				}
			} else {
				String[] sensorNames = tsdb.getSensorNamesOfPlot(plot.name);
				sensorList.add(sensorAll);
				for(String sensorName:sensorNames) {
					Sensor sensor = sensorMap.get(sensorName);
					if(sensor.isAggregable()) {
						sensorList.add(sensor);
					}
				}	
			}
		} catch (Exception e) {
			log.error(e);
		}

		/*comboSensor.setItems(sensorList);
		if(sensorList.isEmpty()) {
			comboSensor.setValue(null);
		} else {
			comboSensor.setValue(sensorList.get(0));
		}*/
		comboSensor.setItems(sensorList);
		comboSensor.setValue(sensorAll);		
	}

	private static class QueryEntry {
		public final String plotID;
		public final Sensor sensor;
		public final Long startTimestamp;
		public final Long endTimestamp;
		public final ObjectProperty<TimestampSeries> timestampSeriesProperty;		
		public final ObjectProperty<Image>  imageProperty;
		public QueryEntry(String plotID,Sensor sensor,Long startTimestamp,Long endTimestamp) {
			this.plotID = plotID;
			this.sensor = sensor;
			this.timestampSeriesProperty = new SimpleObjectProperty<TimestampSeries>();
			this.imageProperty = new SimpleObjectProperty<Image>();
			this.startTimestamp = startTimestamp;
			this.endTimestamp = endTimestamp;
		}
	}

	private static class ScreenImageEntry {

		public final ImageView imageView;
		private QueryEntry queryEntry; 
		private ChangeListener<Image> imageChangeListener;

		public ScreenImageEntry(ImageView imageView) {
			this.imageView = imageView;
			this.queryEntry = null;
			this.imageChangeListener = null;
		}

		public void setQueryEntry(QueryEntry queryEntryNew) {
			if(queryEntry!=null) {
				if(imageChangeListener!=null) {
					queryEntry.imageProperty.removeListener(imageChangeListener);
				}
				imageChangeListener = null;
			}
			this.queryEntry = queryEntryNew;
			if(queryEntryNew!=null) {
				Image check = queryEntryNew.imageProperty.get();
				if(check!=null) {
					imageView.setImage(check);
				} else {
					imageChangeListener = (s,o,image)->{
						System.out.println("************************** set image "+queryEntry.plotID+"  "+queryEntry.sensor.name);
						imageView.setImage(image);
					};			
					queryEntryNew.imageProperty.addListener(imageChangeListener);
					imageView.setImage(queryEntryNew.imageProperty.get());
				}
			} else {
				imageView.setImage(null);
			}

		}
	}

	private void onSensorChanged(ObservableValue<? extends Sensor> observable, Sensor oldValue, Sensor sensor) {
		updateSelectionQueryList();
	}
	
	private void onTimeChanged(ObservableValue<? extends String> observable, String oldValue, String sensor) {
		updateSelectionQueryList();
	}

	@Override
	protected void onClose() {
		if(executorQueryTimeSeries!=null) {			
			executorQueryTimeSeries.shutdownNow();			
		}
		if(executorDrawImages!=null) {		
			executorDrawImages.shutdownNow();		
		}
	}
	
	void updateSelectionQueryList() {
		selectionQueryList.clear();
		
		PlotInfo selectionPlot = comboPlot.getValue();
		Sensor sensor = comboSensor.getValue();
		String timeText = comboTime.getValue();
		if(selectionPlot==null||sensor==null||timeText==null) {
			selectedCountProperty.set("?");	
			return;
		}
		
		
		try {
			ArrayList<PlotInfo> selectedPlotList = new ArrayList<PlotInfo>();

			if(selectionPlot.name.equals(plotAll.name)) {
				for(PlotInfo item:comboPlot.getItems()) {
					if(!item.name.equals(plotAll.name)) {
						selectedPlotList.add(item);
					}
				}			
			} else {
				selectedPlotList.add(selectionPlot);
			}

			HashMap<String,Sensor> selectedSensorMap = new HashMap<String,Sensor>();
			
			if(sensor.name.equals(sensorAll.name)) {
				for(Sensor item:comboSensor.getItems()) {
					if(!item.name.equals(sensorAll.name)) {
						selectedSensorMap.put(item.name, item);
					}
				}
			} else {
				selectedSensorMap.put(sensor.name, sensor);
			}
			
			Long startTimestamp = null;
			Long endTimestamp = null;
			
			
			if(!timeText.equals(timeAll)) {
				int year = Integer.parseInt(timeText);
				startTimestamp = TimeConverter.getYearStartTimestamp(year);
				endTimestamp = TimeConverter.getYearEndTimestamp(year);
			}


			for(PlotInfo plot:selectedPlotList) {
				String[] plotSensorNames;

				plotSensorNames = tsdb.getSensorNamesOfPlot(plot.name);

				if(plotSensorNames!=null) {
					for(String sensorName:plotSensorNames) {
						if(selectedSensorMap.containsKey(sensorName)) {
							sensor = sensorMap.get(sensorName);
							selectionQueryList.add(new QueryEntry(plot.name, sensor,startTimestamp,endTimestamp));
						}
					}
				}		
			}

		} catch (RemoteException e) {
			log.error(e);
		}

		selectedCountProperty.set(""+selectionQueryList.size());		
	}
	
	

}

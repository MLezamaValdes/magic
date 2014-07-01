package gui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import timeseriesdatabase.GeneralStation;
import timeseriesdatabase.LoggerType;
import timeseriesdatabase.Sensor;
import timeseriesdatabase.Station;
import timeseriesdatabase.TimeConverter;
import timeseriesdatabase.TimeSeriesDatabase;
import timeseriesdatabase.aggregated.AggregationType;
import timeseriesdatabase.aggregated.NanGapIterator;
import timeseriesdatabase.aggregated.TimeSeries;
import timeseriesdatabase.raw.TimestampSeriesEntry;
import util.SchemaIterator;
import util.Util;

public class AggregatedQueryDialog extends Dialog {

	private static Logger log = Util.log;

	TimeSeriesDatabase timeSeriesDatabase;

	Canvas canvas;

	TimeSeries queryResult;

	// *** begin of GUI elemmts ***
	Combo comboGeneralStation;
	Combo comboPlotID;
	Combo comboSensorName;
	Label labelInfo;
	// *** end of GUI elements ***
	
	float minValue;
	float maxValue;

	public AggregatedQueryDialog(Shell parent, TimeSeriesDatabase timeSeriesDatabase) {
		this(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.MAX, timeSeriesDatabase);

	}

	public AggregatedQueryDialog(Shell parent, int style,TimeSeriesDatabase timeSeriesDatabase) {
		super(parent, style);
		this.timeSeriesDatabase = timeSeriesDatabase;
		setText("Query");
	}

	public String open() {
		// Create the dialog window
		Shell shell = new Shell(getParent(), getStyle());
		shell.setText(getText());
		createContents(shell);
		//shell.pack();
		shell.open();
		Display display = getParent().getDisplay();
		updateGUI();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		// Return the entered value, or null
		return null;
	}

	private void createContents(final Shell shell) {
		shell.setLayout(new GridLayout(2, false));

		new Label(shell, SWT.NONE).setText("general station");
		/*Text textGeneralStation = new Text(shell, SWT.BORDER);
		GridData gridDataGeneralStation = new GridData();
		gridDataGeneralStation.horizontalAlignment = SWT.FILL;
		gridDataGeneralStation.grabExcessHorizontalSpace = true;
		textGeneralStation.setLayoutData(gridDataGeneralStation);
		textGeneralStation.setText("HEG");*/


		comboGeneralStation = new Combo(shell, SWT.BORDER);
		GridData gridDataGeneralStation = new GridData();
		gridDataGeneralStation.horizontalAlignment = SWT.FILL;
		gridDataGeneralStation.grabExcessHorizontalSpace = true;
		comboGeneralStation.setLayoutData(gridDataGeneralStation);
		comboGeneralStation.setItems(new String [] {"HEG"});
		comboGeneralStation.setText("HEG");
		comboGeneralStation.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				System.out.println("widgetSelected");
				updateGUIplotID();				
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				System.out.println("widgetDefaultSelected");
				updateGUIplotID();				
			}
		});


		new Label(shell, SWT.NONE).setText("plot ID");		
		comboPlotID = new Combo(shell, SWT.BORDER);
		GridData gridDataPlotID = new GridData();
		gridDataPlotID.horizontalAlignment = SWT.FILL;
		gridDataPlotID.grabExcessHorizontalSpace = true;
		comboPlotID.setLayoutData(gridDataPlotID);
		comboPlotID.setItems(new String [] {"HEG01"});
		comboPlotID.setText("HEG01");
		comboPlotID.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateGUISensorName();

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});


		new Label(shell, SWT.NONE).setText("sensor name");		
		comboSensorName = new Combo(shell, SWT.BORDER);
		GridData gridDataSensorName = new GridData();
		gridDataSensorName.horizontalAlignment = SWT.FILL;
		gridDataSensorName.grabExcessHorizontalSpace = true;
		comboSensorName.setLayoutData(gridDataSensorName);
		comboSensorName.setItems(new String [] {"Ta_200"});
		comboSensorName.setText("Ta_200");

		new Label(shell, SWT.NONE).setText("");
		Button buttonQuery = new Button(shell, SWT.NONE);
		GridData gridDataQuery = new GridData();
		gridDataQuery.horizontalAlignment = SWT.FILL;
		gridDataQuery.grabExcessHorizontalSpace = true;
		buttonQuery.setLayoutData(gridDataQuery);
		buttonQuery.setText("query");
		buttonQuery.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				buttonQuery.setEnabled(false);	        	  
				runQuery();
				canvas.redraw();	        	  
				buttonQuery.setEnabled(true);
				updateGUIInfo();
			}
		});

		new Label(shell, SWT.NULL).setText("result:");
		canvas = new Canvas(shell, SWT.BORDER);
		GridData gridDataCanvas = new GridData();
		gridDataCanvas.horizontalAlignment = SWT.FILL;
		gridDataCanvas.verticalAlignment = SWT.FILL;
		gridDataCanvas.grabExcessHorizontalSpace = true;
		gridDataCanvas.grabExcessVerticalSpace = true;
		canvas.setLayoutData(gridDataCanvas);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent event) {
				paintCanvas(event.gc);
			}
		});

		new Label(shell, SWT.NULL).setText("info:");
		labelInfo = new Label(shell, SWT.NULL);
		GridData gridDataInfo = new GridData();
		gridDataInfo.horizontalAlignment = SWT.FILL;
		gridDataInfo.grabExcessHorizontalSpace = true;
		labelInfo.setLayoutData(gridDataInfo);
		labelInfo.setText("no result");

	}

	/*
	private void createContentsOLD(final Shell shell) {




		GridLayout gridLayout = new GridLayout();

		gridLayout.numColumns = 3;

		shell.setLayout(gridLayout);



		new Label(shell, SWT.NULL).setText("plot ID:");

		Text textPlotID = new Text(shell, SWT.SINGLE | SWT.BORDER);
		textPlotID.setText("HEG01");

		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);

		gridData.horizontalSpan = 2;

		textPlotID.setLayoutData(gridData);



		new Label(shell, SWT.NULL).setText("sensor name:");
		Combo comboSensorName = new Combo(shell, SWT.NULL);
		comboSensorName.setItems(new String [] {"Ta_200", "...", "...", "..."});
		comboSensorName.setText("Ta_200");
		comboSensorName.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));



		Label label = new Label(shell, SWT.NULL);

		label.setText("...");

		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));



		new Label(shell, SWT.NULL).setText("result:");

		canvas = new Canvas(shell, SWT.BORDER);

		gridData = new GridData(GridData.FILL_BOTH);

		gridData.widthHint = 80;

		gridData.heightHint = 80;

		gridData.verticalSpan = 3;

		canvas.setLayoutData(gridData);

		canvas.addPaintListener(new PaintListener() {

			public void paintControl(final PaintEvent event) {
				paintCanvas(event.gc);

			}

		});



		Button run = new Button(shell, SWT.PUSH);

		run.setText("query");

		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING);

		gridData.horizontalIndent = 5;

		run.setLayoutData(gridData);

		run.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				run.setEnabled(false);	        	  
				runQuery();
				canvas.redraw();	        	  
				run.setEnabled(true);
			}

		});





	}*/


	private void runQuery() {
		queryResult = null;
		String plotID = comboPlotID.getText();
		String sensorName = comboSensorName.getText();
		try {			
			labelInfo.setText("query...");
			//queryResult = timeSeriesDatabase.queryBaseAggregatedDataGapFilled(plotID, new String[]{sensorName}, null, null);
			queryResult = timeSeriesDatabase.queryBaseAggregatedTimeSeries(plotID, new String[]{sensorName}, null, null, true, true, true);
			//queryResult = timeSeriesDatabase.queryGapFilledTimeSeries(plotID, new String[]{sensorName}, null, null, true, true, true);
			updateViewData();
			updateGUIInfo();
		} catch(Exception e) {
			log.error(e);
		}

	}

	private void paintCanvas(GC gc) {
		if(queryResult!=null) {
			float[] data = queryResult.data[0];
			float width = canvas.getSize().x;
			float height = canvas.getSize().y;
			float timeFactor = width/data.length;

			float valueFactor = height/(maxValue-minValue);

			float valueOffset = -minValue;


			System.out.println("valueFactor: "+valueFactor);
			System.out.println("valueOffset: "+valueOffset);

			Float prevValue = null;

			for(int offset=0;offset<data.length;offset++) {
				float value = data[offset];
				if(!Float.isNaN(value)) {
					int x = (int) (offset*timeFactor);
					int y = (int)(height-((value+valueOffset)*valueFactor));
					if(prevValue!=null) {
						int xprev = (int) ((offset-1)*timeFactor);
						int yprev = (int)(height-((prevValue+valueOffset)*valueFactor));
						gc.drawLine(xprev, yprev, x, y);
					} else {
						gc.drawLine(x, y, x, y);
					}
					prevValue = value;
				} else {
					prevValue = null;
				}
			}
			System.out.println("data length: "+data.length);
		}

	}

	void updateGUI() {
		updateGUIgeneralstations();
		updateGUIplotID();
		updateGUISensorName();
		updateGUIInfo();
	}

	void updateGUIgeneralstations() {
		//String[] generalStations = timeSeriesDatabase.generalStationMap.keySet().toArray(new String[0]);
		//TODO: just for testing!
		String[] generalStations = new String[]{"HEG","HEW"};
		comboGeneralStation.setItems(generalStations);
		comboGeneralStation.setText(generalStations[0]);
	}

	void updateGUIplotID() {
		String generalStationName = comboGeneralStation.getText();
		GeneralStation generalStation = timeSeriesDatabase.generalStationMap.get(generalStationName);
		if(generalStation!=null) {
			java.util.List<Station> list = generalStation.stationList;
			String[] plotIDs = new String[list.size()];
			for(int i=0;i<list.size();i++) {
				plotIDs[i] = list.get(i).plotID;
			}
			comboPlotID.setItems(plotIDs);
			comboPlotID.setText(plotIDs[0]);
		} else {
			comboPlotID.setItems(new String[]{});
		}

	}

	void updateGUISensorName() {
		System.out.println("updateGUISensorName");
		String stationName = comboPlotID.getText();
		Station station = timeSeriesDatabase.stationMap.get(stationName);
		if(station!=null) {
			LoggerType loggerType = station.getLoggerType();
			ArrayList<String> sensorNames = new ArrayList<String>();
			for(String name:loggerType.sensorNames) {
				if(timeSeriesDatabase.baseAggregatonSensorNameSet.contains(name)) {
					sensorNames.add(name);
				}
			}
			comboSensorName.setItems(sensorNames.toArray(new String[0]));
			comboSensorName.setText(sensorNames.get(0));
		} else {
			comboSensorName.setItems(new String[]{});
		}

	}
	
	void updateViewData() {
		if(queryResult!=null) {
			float[] data = queryResult.data[0];
		
			minValue = Float.MAX_VALUE;
			maxValue = -Float.MAX_VALUE;

			for(int offset=0;offset<data.length;offset++) {
				float value = data[offset];
				if(!Float.isNaN(value)) {
					if(value<minValue) {
						minValue = value;						
					}
					if(value>maxValue) {
						maxValue = value;						
					}
				}
			}
		} else {
			minValue = Float.NaN;
			maxValue = -Float.NaN;
		}
	}

	void updateGUIInfo() {
		if(queryResult!=null) {
			long starttimestamp = queryResult.getFirstTimestamp();
			long endtimestamp = queryResult.getLastTimestamp();
			LocalDateTime start = TimeConverter.oleMinutesToLocalDateTime(starttimestamp);
			LocalDateTime end = TimeConverter.oleMinutesToLocalDateTime(endtimestamp);
			String s = queryResult.data[0].length+" entries \t\t time: "+start+" - "+end+"\t\t value range: "+minValue+" - "+maxValue;
			labelInfo.setText(s);
		} else {
			labelInfo.setText("no result");
		}
	}



}


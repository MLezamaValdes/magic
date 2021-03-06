package tsdb.gui.info;

import java.rmi.RemoteException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import tsdb.component.SourceEntry;
import tsdb.gui.info.SourceViewComparator.SortType;
import tsdb.remote.RemoteTsDB;
import tsdb.util.TimeUtil;
import tsdb.util.Util;

@Deprecated
public class SourceCatalogInfoDialog extends Dialog {
	
	private static final Logger log = LogManager.getLogger();

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Database Source Catalog");

	}	

	private RemoteTsDB tsdb;
	private Table table;
	private TableViewer viewer;

	private SourceViewComparator sourceViewComparator = new SourceViewComparator();

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public SourceCatalogInfoDialog(Shell parentShell, RemoteTsDB timeSeriesDatabase) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE);

		this.tsdb = timeSeriesDatabase;
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));

		// define the TableViewer
		viewer = new TableViewer(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		// create the columns 
		// not yet implemented
		createColumns(viewer);

		table = viewer.getTable();
		table.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setBounds(74, 10, 300, 171);
		//formToolkit.paintBordersFor(table);

		// set the content provider
		viewer.setContentProvider(ArrayContentProvider.getInstance());

		try {

			SourceEntry[] sourceCatalogEntries = tsdb.getSourceCatalogEntries();

			viewer.setInput(sourceCatalogEntries);

			System.out.println("catalog size: "+sourceCatalogEntries.length);


			viewer.setComparator(sourceViewComparator);

		} catch(RemoteException e) {
			e.printStackTrace();
			log.error(e);
		}

		return container;
	}

	private void createColumns(TableViewer viewer) {
		TableViewerColumn colStationName = new TableViewerColumn(viewer, SWT.NONE);
		colStationName.getColumn().setWidth(100);
		colStationName.getColumn().setText("Station Name");
		colStationName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+e.stationName;
			}
		});
		colStationName.getColumn().addSelectionListener(getSelectionSortListener(SortType.STATION_NAME));		

		TableViewerColumn colFirstTimestamp = new TableViewerColumn(viewer, SWT.NONE);
		colFirstTimestamp.getColumn().setWidth(120);
		colFirstTimestamp.getColumn().setText("First Timestamp");
		colFirstTimestamp.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+TimeUtil.oleMinutesToText(e.firstTimestamp);
			}
		});
		colFirstTimestamp.getColumn().addSelectionListener(getSelectionSortListener(SortType.FIRST_TIMESTAMP));	

		TableViewerColumn colLastTimestamp = new TableViewerColumn(viewer, SWT.NONE);
		colLastTimestamp.getColumn().setWidth(120);
		colLastTimestamp.getColumn().setText("Last Timestamp");
		colLastTimestamp.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+TimeUtil.oleMinutesToText(e.lastTimestamp);
			}
		});
		colLastTimestamp.getColumn().addSelectionListener(getSelectionSortListener(SortType.LAST_TIMESTAMP));

		TableViewerColumn colRowCount = new TableViewerColumn(viewer, SWT.NONE);
		colRowCount.getColumn().setWidth(50);
		colRowCount.getColumn().setText("Row Count");
		colRowCount.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+e.rows;
			}
		});
		colRowCount.getColumn().addSelectionListener(getSelectionSortListener(SortType.ROW_COUNT));		

		TableViewerColumn colFileName = new TableViewerColumn(viewer, SWT.NONE);
		colFileName.getColumn().setWidth(300);
		colFileName.getColumn().setText("File Name");
		colFileName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+e.filename;
			}
		});
		colFileName.getColumn().addSelectionListener(getSelectionSortListener(SortType.FILE_NAME));

		TableViewerColumn colHeaderNames = new TableViewerColumn(viewer, SWT.NONE);
		colHeaderNames.getColumn().setWidth(300);
		colHeaderNames.getColumn().setText("Header Names");
		colHeaderNames.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+Util.arrayToString(e.headerNames);
			}
		});
		colHeaderNames.getColumn().addSelectionListener(getSelectionSortListener(SortType.HEADER_NAMES));

		TableViewerColumn colSensorNames = new TableViewerColumn(viewer, SWT.NONE);
		colSensorNames.getColumn().setWidth(300);
		colSensorNames.getColumn().setText("Sensor Names");
		colSensorNames.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+Util.arrayToString(e.sensorNames);
			}
		});
		colSensorNames.getColumn().addSelectionListener(getSelectionSortListener(SortType.SENSOR_NAMES));

		TableViewerColumn colTimeStep = new TableViewerColumn(viewer, SWT.NONE);
		colTimeStep.getColumn().setWidth(50);
		colTimeStep.getColumn().setText("Time Step");
		colTimeStep.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				SourceEntry e = (SourceEntry) element;
				return ""+e.timeStep;
			}
		});
		colTimeStep.getColumn().addSelectionListener(getSelectionSortListener(SortType.TIME_STEP));
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	private SelectionAdapter getSelectionSortListener(SortType sorttype) {
		return new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(sorttype==sourceViewComparator.sorttype) {
					sourceViewComparator.sortAsc = !sourceViewComparator.sortAsc;
				} else {
					sourceViewComparator.sortAsc = true;
				}
				sourceViewComparator.sorttype = sorttype;
				viewer.refresh();
			}			
		};
	}
}

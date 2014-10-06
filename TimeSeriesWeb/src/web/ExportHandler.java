package web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import tsdb.DataQuality;
import tsdb.aggregated.AggregationInterval;
import tsdb.remote.RemoteTsDB;
import tsdb.util.ZipExport;

public class ExportHandler extends AbstractHandler {

	private static class ExportModel{

		public String[] plots;
		public String[] sensors;

		public ExportModel() {
			this.plots = new String[]{"plot1","plot2","plot3"};
			this.sensors = new String[]{"sensor1","sensor2","sensor3","sensor4"};
		}
	}

	private final RemoteTsDB tsdb;

	public ExportHandler(RemoteTsDB tsdb) {
		this.tsdb = tsdb;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		System.out.println("ExportHandler: "+target);
		baseRequest.setHandled(true);
		response.setContentType("text/plain;charset=utf-8");

		HttpSession session = request.getSession();
		if(session.isNew()) {
			ExportModel model = new ExportModel();
			session.setAttribute("ExportModel", model);
			model.plots = new String[]{"HEG01","HEG02"};
			model.sensors = new String[]{"Ta_200",};
		}
		ExportModel model = (ExportModel) session.getAttribute("ExportModel");

		boolean ret = false;

		switch(target) {
		case "/plots": {
			ret = handle_plots(response.getWriter(),model);
			break;
		}
		case "/sensors": {
			ret = handle_sensors(response.getWriter(),model);
			break;
		}
		case "/apply_plots": {
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String line = reader.readLine();
			while(line!=null) {
				lines.add(line);
				line = reader.readLine();
			}			
			ret = apply_plots(response.getWriter(),model,lines);
			break;
		}
		case "/apply_sensors": {
			ArrayList<String> lines = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
			String line = reader.readLine();
			while(line!=null) {
				lines.add(line);
				line = reader.readLine();
			}			
			ret = apply_sensors(response.getWriter(),model,lines);
			break;
		}
		case "/result.zip": {
			ret = handle_download(response,model);
			break;
		}
		default: {
			ret = handle_error(response.getWriter(), target);
		}
		}

		if(ret) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private boolean handle_error(PrintWriter writer, String target) {
		writer.println("error: unknown query: "+target);
		return false;
	}

	private boolean handle_plots(PrintWriter writer, ExportModel model) {		
		writeStringArray(writer,model.plots);
		return true;
	}

	private boolean handle_sensors(PrintWriter writer, ExportModel model) {		
		writeStringArray(writer,model.sensors);		
		return true;
	}

	private static void writeStringArray(PrintWriter writer, String[] array) {
		if(array==null) {
			return;
		}
		boolean notFirst = false;
		for(String s:array) {
			if(notFirst) {
				writer.print('\n');
			}
			writer.print(s);
			notFirst = true;
		}
	}

	private boolean apply_plots(PrintWriter writer, ExportModel model, ArrayList<String> lines) {		
		model.plots = lines.toArray(new String[0]);		
		System.out.println(lines);
		return true;
	}

	private boolean apply_sensors(PrintWriter writer, ExportModel model, ArrayList<String> lines) {
		model.sensors = lines.toArray(new String[0]);
		System.out.println(lines);
		return true;
	}

	private boolean handle_download(HttpServletResponse response, ExportModel model) {
		response.setContentType("application/zip");
		try {
			OutputStream outputstream = response.getOutputStream();
			String[] sensorNames = model.sensors;
			String[] plotIDs = model.plots;
			AggregationInterval aggregationInterval = AggregationInterval.HOUR;
			DataQuality dataQuality = DataQuality.NO;
			boolean interpolated = false;
			ZipExport zipexport = new ZipExport(tsdb, sensorNames, plotIDs, aggregationInterval, dataQuality, interpolated);
			zipexport.writeToStream(outputstream);
			//writer.print("download");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}	
	}	

}

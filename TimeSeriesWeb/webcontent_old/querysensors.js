var region_select;
var generalstation_select;
var sensor_select;
var time_select;
var time_month_select;
var aggregation_select;
var quality_select;
var qualities = ["no", "physical", "step", "empirical"];
var qualitiesText = ["0: no","1: physical","2: physical + step","3: physical + step + empirical"];
var timeText = ["[all]","2008","2009","2010","2011","2012","2013","2014","2015"];
var monthText = ["[whole year]","jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"];
var aggregationText = ["raw","hour","day","week","month","year"];


var sensors;

var tasks = 0;

$(document).ready(function(){
	incTask();
	region_select = $("#region_select");
	generalstation_select = $("#generalstation_select");
	sensor_select = $("#sensor_select");
	time_select = $("#time_select");
	time_month_select = $("#time_month_select");
	aggregation_select = $("#aggregation_select");
	quality_select = $("#quality_select");
	$.each(qualitiesText, function(i,text) {quality_select.append(new Option(text,i));});
	quality_select.val(2);
	$.each(timeText, function(i,text) {time_select.append(new Option(text,i));});	
	time_month_select.hide();
	$.each(monthText, function(i,text) {time_month_select.append(new Option(text,i));});
	$.each(aggregationText, function(i,text) {aggregation_select.append(new Option(text));});
	aggregation_select.val("hour");
	
	getID("region_select").onchange = updateGeneralStations;
	getID("generalstation_select").onchange = updateSensors;
	getID("time_select").onchange = onUpdateTime;
getID("aggregation_select").onchange = onUpdateAggregation;	
	getID("sensor_select").onchange = updateSensor;
	getID("query_sensor").onclick = runQuerySensor;
	
	updataRegions();
	decTask();
});

function onUpdateTime() {
	if(time_select.val()==0) {
		time_month_select.hide();
	} else {
		time_month_select.show();
	}
}

function onUpdateAggregation() {
	if(aggregation_select.val()=="raw") {
		$("#div_quality_select").hide();
		$("#div_interpolated").hide();
	} else {
		$("#div_quality_select").show();
		$("#div_interpolated").show();
	}
}

var incTask = function() {
	runDisabled(true);
	tasks++;	
	getID("status").innerHTML = "busy ("+tasks+")...";
}

var decTask = function() {
	tasks--;
	if(tasks===0) {
		getID("status").innerHTML = "ready";
		runDisabled(false);
	} else if(tasks<0){
		getID("status").innerHTML = "error";
	} else {
		getID("status").innerHTML = "busy ("+tasks+")...";
	}
}

function runDisabled(disabled) {
	$(".blockable").prop( "disabled",disabled);
}

var updataRegions = function() {
	incTask();
	region_select.empty();
	$.get("../tsdb/region_list").done(function(data) {
		var rows = splitData(data);
		$.each(rows, function(i,row) {region_select.append(new Option(row[1],row[0]));});
		updateGeneralStations();
	    decTask();
	}).fail(function() {region_select.append(new Option("[error]","[error]"));decTask();});
}

var updateGeneralStations = function() {
	incTask();
	var regionName = region_select.val();
	generalstation_select.empty();	
	$.get("../tsdb/generalstation_list?region="+regionName).done(function(data) {
		var rows = splitData(data);
		generalstation_select.append(new Option("[all]","[all]"));
		$.each(rows, function(i,row) {generalstation_select.append(new Option(row[1],row[0]));})
		updateSensors();
		decTask();		
	}).fail(function() {generalstation_select.append(new Option("[error]","[error]"));decTask();});
}

var updateSensors = function() {
	incTask();
	
	var prev = "unknown";
	if(sensor_select.val()!=null) {
		prev = sensors[sensor_select.val()][0];
	}
	
	var queryText = "";
	var generalStationName = generalstation_select.val();	
	if(generalStationName==="[all]") {
		queryText = "region="+region_select.val();
	} else {
		queryText = "general_station="+generalStationName;
	}
	sensor_select.empty();	
	$.get("../tsdb/sensor_list?"+queryText).done(function(data) {
		var pre = -1;
		var rows = splitData(data);
		sensors = rows
		//$.each(sensors, function(i,row) {document.getElementById("sensor_select").add(new Option(row[0],i));})
		$.each(rows, function(i,row) {
			sensor_select.append(new Option(row[0],i));
			if(row[0]===prev) {
				pre = i;
			}
			if(pre<0&&row[0]==="Ta_200") {
				pre = i;
			}
		})
		
		if(pre>0) {
			sensor_select.val(pre);
		}
		
		updateSensor();
		decTask();
	}).fail(function() {plot_select.append(new Option("[error]","[error]"));decTask();});
}

var updateSensor = function() {
	incTask();
	var row = sensors[sensor_select.val()];
	if(row === undefined) {
		getID("sensor_description").innerHTML = "---";
		getID("sensor_unit").innerHTML = "---";
	} else {
		getID("sensor_description").innerHTML = row[1];
		getID("sensor_unit").innerHTML = row[2];
	}
	decTask();
}

var runQuerySensor = function() {
	incTask();
	getID("result").innerHTML = "query...";
	if(sensors[sensor_select.val()] === undefined) {
		getID("result").innerHTML = "no sensor";
		decTask();
		return;
	}
	var sensorName = sensors[sensor_select.val()][0];
	generalStationName = generalstation_select.val();	
	var queryText = "";
	if(generalStationName==="[all]") {
		queryText = "region="+region_select.val();
	} else {
		queryText = "generalstation="+generalStationName;
	}
	$.get("../tsdb/plot_list?"+queryText).done(function(data) {
		getID("result").innerHTML = "";	
		var rows = splitData(data);
		$.each(rows, function(i,row) {addDiagram(row[0],sensorName);})
		decTask();		
	}).fail(function() {getID("result").innerHTML = "error";decTask();})
}

var addDiagram = function(plotName, sensorName) {
	incTask();
	var plotResult = getID("result").appendChild(document.createElement("div"));
	var plotResultTitle = plotResult.appendChild(document.createElement("div"));
	plotResultTitle.innerHTML += "query "+plotName+"...";
	var aggregationName = aggregation_select.val();
	var qualityName = qualities[quality_select.val()];
	var interpolatedName = ""+getID("interpolated").checked;
	var timeParameter = "";
	var timeName = timeText[time_select.val()];
	if(timeName!="[all]") {
		timeParameter = "&year="+timeName;
		var month = time_month_select.val();
		if(month!=0) {
			timeParameter += "&month="+month;
		}		
	}	
	var image = new Image();
	plotResult.appendChild(image);
	image.onload = function() {
		plotResultTitle.innerHTML = plotName;
		decTask();
	}
	image.onerror = function() {	
		plotResultTitle.innerHTML = plotName+": no data";
		plotResultTitle.style.color = "grey";
		plotResult.removeChild(image);
		decTask();
	}
	image.src = "../tsdb/query_image?plot="+plotName+"&sensor="+sensorName+"&aggregation="+aggregationName+"&quality="+qualityName+"&interpolated="+interpolatedName+timeParameter;	
}

/**
 * Calendar
 * @param   beginYear           1990
 * @param   endYear             2010
 * @param   language            0(zh_cn)|1(en_us)|2(en_en)|3(zh_tw)
 * @param   patternDelimiter    "-"
 * @param   date2StringPattern  "yyyy-MM-dd"
 * @param   string2DatePattern  "ymd"
 * @version 1.0 build 2006-04-01
 * @version 1.1 build 2006-12-17
 * @author  KimSoft (jinqinghua [at] gmail.com)
 * NOTE!    you can use it free, but keep the copyright please
 * IMPORTANT:you must include this script file inner html body elment 
 */
function Calendar(beginYear, endYear, language, patternDelimiter, date2StringPattern, string2DatePattern) {
	this.beginYear = beginYear || 1990;
	this.endYear   = endYear   || 2020;
	this.language  = language  || 0;
	this.patternDelimiter = patternDelimiter     || "-";
	this.date2StringPattern = date2StringPattern || Calendar.language["date2StringPattern"][this.language].replace(/\-/g, this.patternDelimiter);
	this.string2DatePattern = string2DatePattern || Calendar.language["string2DatePattern"][this.language];
	
	this.dateControl = null;
	this.panel  = this.getElementById("__calendarPanel");
	this.iframe = window.frames["__calendarIframe"];
	this.form   = null;
	
	this.date = new Date();
	this.year = this.date.getFullYear();
	this.month = this.date.getMonth();
	/////////////////////////////////////////////
	this.counter = 0;
	
	this.hour = this.date.getHours();
	this.minute = this.date.getMinutes();
	this.second = this.date.getSeconds();
	
	/////////////////////////////////////////////////
	
	this.colors = {"bg_cur_day":"#00CC33","bg_over":"#EFEFEF","bg_out":"#FFCC00"}
};

Calendar.language = {
	"year"   : ["\u5e74", "", "", "\u5e74"],
	"months" : [
				["\u4e00\u6708","\u4e8c\u6708","\u4e09\u6708","\u56db\u6708","\u4e94\u6708","\u516d\u6708","\u4e03\u6708","\u516b\u6708","\u4e5d\u6708","\u5341\u6708","\u5341\u4e00\u6708","\u5341\u4e8c\u6708"],
				["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"],
				["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"],
				["\u4e00\u6708","\u4e8c\u6708","\u4e09\u6708","\u56db\u6708","\u4e94\u6708","\u516d\u6708","\u4e03\u6708","\u516b\u6708","\u4e5d\u6708","\u5341\u6708","\u5341\u4e00\u6708","\u5341\u4e8c\u6708"]
				],
	"weeks"  : [["\u65e5","\u4e00","\u4e8c","\u4e09","\u56db","\u4e94","\u516d"],
				["Sun","Mon","Tur","Wed","Thu","Fri","Sat"],
				["Sun","Mon","Tur","Wed","Thu","Fri","Sat"],
				["\u65e5","\u4e00","\u4e8c","\u4e09","\u56db","\u4e94","\u516d"]
		],
	"clear"  : ["\u6e05\u7a7a", "Clear", "Clear", "\u6e05\u7a7a"],
	"today"  : ["\u4eca\u5929", "Today", "Today", "\u4eca\u5929"],
	"close"  : ["\u5173\u95ed", "Close", "Close", "\u95dc\u9589"],
	
	"hour"	 : ["\u5173\u95ed", "Hour", "Hour", "\u95dc\u9589"],
	"minute" : ["\u5173\u95ed", "Minute", "Minute", "\u95dc\u9589"],	
	"second" : ["\u5173\u95ed", "Second", "Second", "\u95dc\u9589"],	
	
	"date2StringPattern" : ["yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd hh:mm:ss"],
	"string2DatePattern" : ["ymd","ymd", "ymd", "ymd"]
};

Calendar.prototype.draw = function() {
	calendar = this;
	
	var _cs = [];
	_cs[_cs.length] = '<form id="__calendarForm" name="__calendarForm" method="post">';
	_cs[_cs.length] = '<table id="__calendarTable" width="100%" border="0" cellpadding="3" cellspacing="1" align="center">';
	_cs[_cs.length] = ' <tr>';
	_cs[_cs.length] = '  <th><input class="l" name="goPrevMonthButton" type="button" id="goPrevMonthButton" value="&lt;" \/><\/th>';
	_cs[_cs.length] = '  <th colspan="5"><select class="year" name="yearSelect" id="yearSelect"><\/select><select class="month" name="monthSelect" id="monthSelect"><\/select><\/th>';
	_cs[_cs.length] = '  <th><input class="r" name="goNextMonthButton" type="button" id="goNextMonthButton" value="&gt;" \/><\/th>';
	_cs[_cs.length] = ' <\/tr>';
	_cs[_cs.length] = ' <tr>';
	for(var i = 0; i < 7; i++) {
		_cs[_cs.length] = '<th class="theader">';
		_cs[_cs.length] = Calendar.language["weeks"][this.language][i];
		_cs[_cs.length] = '<\/th>';	
	}
	_cs[_cs.length] = '<\/tr>';
	for(var i = 0; i < 6; i++){
		_cs[_cs.length] = '<tr align="center">';
		for(var j = 0; j < 7; j++) {
			switch (j) {
				case 0: _cs[_cs.length] = '<td class="sun">&nbsp;<\/td>'; break;
				case 6: _cs[_cs.length] = '<td class="sat">&nbsp;<\/td>'; break;
				default:_cs[_cs.length] = '<td class="normal">&nbsp;<\/td>'; break;
			}
		}
		_cs[_cs.length] = '<\/tr>';
	}
	_cs[_cs.length] = ' <tr>';
	
	
	_cs[_cs.length] = '  <th colspan="2"><select  class="b" name="hourSelect" id="hourSelect"><  \/select><\/th>';
	//_cs[_cs.length] = '  <th colspan="0">时<\/th>';
	_cs[_cs.length] = '  <th colspan="2"><select  class="b" name="minuteSelect" id="minuteSelect"><  \/select><\/th>';
	//_cs[_cs.length] = '  <th colspan="1">分<\/th>';
	_cs[_cs.length] = '  <th colspan="2"><select  class="b" name="secondSelect" id="secondSelect"><  \/select><\/th>';
	//_cs[_cs.length] = '  <th colspan="1">秒<\/th>';
	
	_cs[_cs.length] = ' <\/tr>';
	
	_cs[_cs.length] = ' <tr>';
	
	_cs[_cs.length] = '  <th colspan="2"><input type="button" class="b" name="clearButton" id="clearButton" \/><\/th>';
	_cs[_cs.length] = '  <th colspan="3"><input type="button" class="b" name="selectTodayButton" id="selectTodayButton" \/><\/th>';
	_cs[_cs.length] = '  <th colspan="2"><input type="button" class="b" name="closeButton" id="closeButton" \/><\/th>';
	_cs[_cs.length] = ' <\/tr>';
	_cs[_cs.length] = '<\/table>';
	_cs[_cs.length] = '<\/form>';
	
	this.iframe.document.body.innerHTML = _cs.join("");
	this.form = this.iframe.document.forms["__calendarForm"];

	this.form.clearButton.value = Calendar.language["clear"][this.language];
	this.form.selectTodayButton.value = '确定';//Calendar.language["today"][this.language];
	this.form.closeButton.value = Calendar.language["close"][this.language];
	
	//this.dateControl.value=calendar.format(calendar.date2StringPattern);\
	//alert(calendar.dateControl.value);
	var today;
	if(calendar.dateControl.value == ''){
		today = new Date();
	}else{
		today = calendar.dateControl.value.toDate();
	
	}
	//alert(today);
	calendar.dateControl.value = today.format(calendar.date2StringPattern);
	
	this.form.goPrevMonthButton.onclick = function () {calendar.goPrevMonth(this);}
	this.form.goNextMonthButton.onclick = function () {calendar.goNextMonth(this);}
	this.form.yearSelect.onchange = function () {calendar.update(this);}
	this.form.monthSelect.onchange = function () {calendar.update(this);}
	this.form.hourSelect.onchange = function () {calendar.update2(this); }	
	this.form.minuteSelect.onchange = function () {calendar.update2(this);}
	this.form.secondSelect.onchange = function () {calendar.update2(this); }
	
	this.form.clearButton.onclick = function () {calendar.dateControl.value = "";calendar.hide();}
	this.form.closeButton.onclick = function () {calendar.hide();}
	
	this.form.selectTodayButton.onclick = function () {      //add some hour and minute inputs, and change to
		
		var today = new Date();
		//calendar.date = today;
		/*calendar.year = today.getFullYear();
		calendar.month = today.getMonth();
		calendar.hour = today.getHours();
		calendar.minute = today.getMinutes();
		calendar.second = today.getSeconds();
		*/
	//	calendar.dateControl.value = today.format(calendar.date2StringPattern);
		calendar.dateControl.value = new Date(          calendar.date.getFullYear(),
														calendar.date.getMonth(),
														calendar.date.getDate(),
														calendar.date.getHours(),
														calendar.date.getMinutes(),
														calendar.date.getSeconds()
														).format(calendar.date2StringPattern);
		
		calendar.hide();
	}
};

Calendar.prototype.bindYear = function() {
	var ys = this.form.yearSelect;
	ys.length = 0;
	for (var i = this.beginYear; i <= this.endYear; i++){
		ys.options[ys.length] = new Option(i + Calendar.language["year"][this.language], i);
	}
};

Calendar.prototype.bindMonth = function() {
	var ms = this.form.monthSelect;
	ms.length = 0;
	for (var i = 0; i < 12; i++){
		ms.options[ms.length] = new Option(Calendar.language["months"][this.language][i], i);
	}
};

Calendar.prototype.bindHour = function() {
	var ms = this.form.hourSelect;
	ms.length = 0;
	for (var i = 0; i < 24; i++){
		ms.options[ms.length] = new Option(i);
	}
	//ms.set
//	ms.selectedIndex(6);
	
};

Calendar.prototype.bindMinute = function() {
	var ms = this.form.minuteSelect;
	ms.length = 0;
	for (var i = 0; i < 60; i++){
		ms.options[ms.length] = new Option(i);
	}
};

Calendar.prototype.bindSecond = function() {
	var ms = this.form.secondSelect;
	ms.length = 0;
	for (var i = 0; i < 60; i++){
		ms.options[ms.length] = new Option(i);
	}
};

Calendar.prototype.goPrevMonth = function(e){
	if (this.year == this.beginYear && this.month == 0){return;}
	this.month--;
	if (this.month == -1) {
		this.year--;
		this.month = 11;
	}
	this.date = new Date(this.year, this.month, 1);
	this.changeSelect();
	this.bindData();
};

Calendar.prototype.goNextMonth = function(e){
	if (this.year == this.endYear && this.month == 11){return;}
	this.month++;
	if (this.month == 12) {
		this.year++;
		this.month = 0;
	}
	this.date = new Date(this.year, this.month, 1);
	this.changeSelect();
	this.bindData();
};

Calendar.prototype.changeSelect = function() {
	var ys = this.form.yearSelect;
	var ms = this.form.monthSelect;
	var hs = this.form.hourSelect;
	var ms2 = this.form.minuteSelect;
	var ss = this.form.secondSelect;
	
	for (var i= 0; i < ys.length; i++){
		if (ys.options[i].value == this.date.getFullYear()){
			ys[i].selected = true;
			break;
		}
	}
	
	
	for (var i= 0; i < ms.length; i++){
		if (ms.options[i].value == this.date.getMonth()){
			ms[i].selected = true;
			break;
		}
	}	
	
	for (var i= 0; i < hs.length; i++){
		if (hs.options[i].value == this.date.getHours()){
			hs[i].selected = true;
			break;
		}
	}	
	for (var i= 0; i < ms2.length; i++){
		if (ms2.options[i].value == this.date.getMinutes()){
			ms2[i].selected = true;
			break;
		}
	}	
	for (var i= 0; i < ss.length; i++){
		if (ss.options[i].value == this.date.getSeconds()){
			ss[i].selected = true;
			break;
		}
	}
	
	
	
};


//var dhours;

Calendar.prototype.update = function (e){
	this.year  = e.form.yearSelect.options[e.form.yearSelect.selectedIndex].value;
	this.month = e.form.monthSelect.options[e.form.monthSelect.selectedIndex].value;
	this.date = new Date(this.year, this.month, 1);
	
	
	//dhours = e.form.hourSelect.options[e.form.hourSelect.selectedIndex].value;
	//this.date.setHours(e.form.hourSelect.options[e.form.hourSelect.selectedIndex].value);
	//this.date.setMinutes(e.form.minuteSelect.options[e.form.minuteSelect.selectedIndex].value);
	//this.date.setSeconds(e.form.secondSelect.options[e.form.secondSelect.selectedIndex].value);
	

	
	this.changeSelect();
	this.bindData();
};
Calendar.prototype.update2 = function (e){
	
	this.date.setHours(e.form.hourSelect.options[e.form.hourSelect.selectedIndex].value);
	this.date.setMinutes(e.form.minuteSelect.options[e.form.minuteSelect.selectedIndex].value);
	this.date.setSeconds(e.form.secondSelect.options[e.form.secondSelect.selectedIndex].value);
	

	
	this.changeSelect();
	this.bindData();
};

Calendar.prototype.bindData = function () {
	var calendar = this;
	var dateArray = this.getMonthViewDateArray(this.date.getFullYear(), this.date.getMonth());
	var tds = this.getElementsByTagName("td", this.getElementById("__calendarTable", this.iframe.document));
	for(var i = 0; i < tds.length; i++) {
  		tds[i].style.backgroundColor = calendar.colors["bg_over"];
		tds[i].onclick = null;
		tds[i].onmouseover = null;
		tds[i].onmouseout = null;
		tds[i].innerHTML = dateArray[i] || "&nbsp;";
		if (i > dateArray.length - 1) continue;
		if (dateArray[i]){
			var today = new Date();
			tds[i].onclick = function () {
				 if (calendar.dateControl){
					calendar.date.setDate(this.innerHTML);
					calendar.dateControl.value = new Date(calendar.date.getFullYear(),
														calendar.date.getMonth(),
														calendar.date.getDate(),
														calendar.date.getHours(),
														calendar.date.getMinutes(),
														calendar.date.getSeconds()
														).format(calendar.date2StringPattern);
					
					for(var j = 0 ; j < tds.length; j++){
						var a = today.getFullYear() == calendar.date.getFullYear();
						var b = today.getMonth() == calendar.date.getMonth() ;
						var c = today.getDate() == dateArray[j];
						if (a&&b&&c) {
							tds[j].style.backgroundColor = calendar.colors["bg_cur_day"];
						}
						else
							tds[j].style.backgroundColor = calendar.colors["bg_over"];
					}
					
					this.style.backgroundColor = calendar.colors["bg_out"];
				}
				//calendar.hide(); 
				//this
				
				
			}
		//	tds[i].onmouseover = function () {this.style.backgroundColor = calendar.colors["bg_out"];}
		//	tds[i].onmouseout  = function () {this.style.backgroundColor = calendar.colors["bg_over"];}
			
			
			if (today.getFullYear() == calendar.date.getFullYear()) {
				if (today.getMonth() == calendar.date.getMonth()) {
					if (today.getDate() == dateArray[i]) {
						tds[i].style.backgroundColor = calendar.colors["bg_cur_day"];
						tds[i].onmouseover = function () {this.style.backgroundColor = calendar.colors["bg_out"];}
						tds[i].onmouseout  = function () {this.style.backgroundColor = calendar.colors["bg_cur_day"];}
					}
				}
			}
		}//end if
	}//end for
//	if(this.innerHTML == "&nbsp;") this.innerHTML = '1'; 
	calendar.dateControl.value = new Date(              calendar.date.getFullYear(),
														calendar.date.getMonth(),
														calendar.date.getDate(),
														calendar.date.getHours(),
														calendar.date.getMinutes(),
														calendar.date.getSeconds()
														).format(calendar.date2StringPattern);
};

Calendar.prototype.getMonthViewDateArray = function (y, m) {
	var dateArray = new Array(42);
	var dayOfFirstDate = new Date(y, m, 1).getDay();
	var dateCountOfMonth = new Date(y, m + 1, 0).getDate();
	for (var i = 0; i < dateCountOfMonth; i++) {
		dateArray[i + dayOfFirstDate] = i + 1;
	}
	return dateArray;
};

Calendar.prototype.show = function (dateControl, popuControl) {
	if (this.panel.style.visibility == "visible") {
		this.panel.style.visibility = "hidden";
	}
	if (!dateControl){
		throw new Error("arguments[0] is necessary!")
	}
	this.dateControl = dateControl;
	popuControl = popuControl || dateControl;

	this.draw();
	this.bindYear();
	this.bindMonth();
	this.bindHour();
	this.bindMinute();
	this.bindSecond();
	
	
	if (dateControl.value.length > 0){
		//alert(dateControl.value);
		this.date  = new Date(dateControl.value.toDate());
		this.year  = this.date.getFullYear();
		this.month = this.date.getMonth();
		this.hour  = this.date.getHours();
		this.minute = this.date.getMinutes();
		this.second = this.date.getSeconds();
	}
	this.changeSelect();
	this.bindData();

	var xy = this.getAbsPoint(popuControl);
	this.panel.style.left = xy.x + "px";
	this.panel.style.top = (xy.y + dateControl.offsetHeight) + "px";
	this.panel.style.visibility = "visible";
};

Calendar.prototype.hide = function() {
	this.panel.style.visibility = "hidden";
};

Calendar.prototype.getElementById = function(id, object){
	object = object || document;
	return document.getElementById ? object.getElementById(id) : document.all(id);
};

Calendar.prototype.getElementsByTagName = function(tagName, object){
	object = object || document;
	return document.getElementsByTagName ? object.getElementsByTagName(tagName) : document.all.tags(tagName);
};

Calendar.prototype.getAbsPoint = function (e){
	var x = e.offsetLeft;
	var y = e.offsetTop;
	while(e = e.offsetParent){
		x += e.offsetLeft;
		y += e.offsetTop;
	}
	return {"x": x, "y": y};
};

/**
 * @param   d the delimiter
 * @param   p the pattern of your date
 * @author  meizz
 * @author  kimsoft add w+ pattern
 */
Date.prototype.format = function(style) {
	var o = {
		"M+" : this.getMonth() + 1, //month
		"d+" : this.getDate(),      //day
		"h+" : this.getHours(),     //hour
		"m+" : this.getMinutes(),   //minute
		"s+" : this.getSeconds(),   //second
		"w+" : "\u65e5\u4e00\u4e8c\u4e09\u56db\u4e94\u516d".charAt(this.getDay()),   //week
		"q+" : Math.floor((this.getMonth() + 3) / 3),  //quarter
		"S"  : this.getMilliseconds() //millisecond
	}
	if (/(y+)/.test(style)) {
		style = style.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	}
	for(var k in o){
		if (new RegExp("("+ k +")").test(style)){
			style = style.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
		}
	}
	return style;
};

/**
 * @param d the delimiter
 * @param p the pattern of your date
 * @rebuilder kimsoft
 * @version build 2006.12.15
 */
String.prototype.toDate = function() {
	
//	var left = this.split(' ');
	var a = this.split('-');
//	alert(a);
	var y = parseInt(a[0], 10);
	//remember to change this next century ;)
	if(y.toString().length <= 2) y += 2000;
	if(isNaN(y)) y = new Date().getFullYear();
	var m = parseInt(a[1], 10) - 1;
	var dayTime = a[2].split(' ');
	//alert(dayTime[0]);
	//alert(dayTime[1]);
	
	var d = dayTime[0];
	if(isNaN(d)) d = 1;
	var hms = dayTime[1].split(':');
	var ho = hms[0];
	var mi = hms[1];
	var se = hms[2];
	//alert(ho + '==' + mi);
	//return new Date(y, m, d, ho, mi ,se);
	var dat = new Date(y,m,d,ho,mi,se);
	//alert(dat);
	return dat;
};

document.writeln('<div id="__calendarPanel" onblur = "calendar.hide();" style="position:absolute;visibility:hidden;z-index:9999;background-color:#FFFFFF;border:1px solid #666666;width:200px;height:240px;">');
document.writeln('<iframe name="__calendarIframe" id="__calendarIframe" width="100%" height="100%" scrolling="no" frameborder="0" style="margin:0px;"><\/iframe>');
var __ci = window.frames['__calendarIframe'];
__ci.document.writeln('<!DOCTYPE html PUBLIC "-\/\/W3C\/\/DTD XHTML 1.0 Transitional\/\/EN" "http:\/\/www.w3.org\/TR\/xhtml1\/DTD\/xhtml1-transitional.dtd">');
__ci.document.writeln('<html xmlns="http:\/\/www.w3.org\/1999\/xhtml">');
__ci.document.writeln('<head>');
__ci.document.writeln('<meta http-equiv="Content-Type" content="text\/html; charset=utf-8" \/>');
__ci.document.writeln('<title>Web Calendar(UTF-8) Written By KimSoft<\/title>');
__ci.document.writeln('<style type="text\/css">');
__ci.document.writeln('<!--');
__ci.document.writeln('body {font-size:12px;margin:0px;text-align:center;}');
__ci.document.writeln('form {margin:0px;}');
__ci.document.writeln('select {font-size:12px;background-color:#EFEFEF;}');
__ci.document.writeln('table {border:0px solid #CCCCCC;background-color:#FFFFFF}');
__ci.document.writeln('th {font-size:12px;font-weight:normal;background-color:#FFFFFF;}');
__ci.document.writeln('th.theader {font-weight:normal;background-color:#666666;color:#FFFFFF;width:24px;}');
__ci.document.writeln('select.year {width:64px;}');
__ci.document.writeln('select.month {width:60px;}');
__ci.document.writeln('td {font-size:12px;text-align:center;}');
__ci.document.writeln('td.sat {color:#0000FF;background-color:#EFEFEF;}');
__ci.document.writeln('td.sun {color:#FF0000;background-color:#EFEFEF;}');
__ci.document.writeln('td.normal {background-color:#EFEFEF;}');
__ci.document.writeln('input.l {border: 1px solid #CCCCCC;background-color:#EFEFEF;width:20px;height:20px;}');
__ci.document.writeln('input.r {border: 1px solid #CCCCCC;background-color:#EFEFEF;width:20px;height:20px;}');
__ci.document.writeln('input.b {border: 1px solid #CCCCCC;background-color:#EFEFEF;width:100%;height:20px;}');
//__ci.document.writeln('select.year {width:64px;}');
//__ci.document.writeln('select.month {width:60px;}');
__ci.document.writeln('-->');
__ci.document.writeln('<\/style>');
__ci.document.writeln('<\/head>');
__ci.document.writeln('<body>');
__ci.document.writeln('<\/body>');
__ci.document.writeln('<\/html>');
__ci.document.close();
document.writeln('<\/div>');
var calendar = new Calendar();
//-->
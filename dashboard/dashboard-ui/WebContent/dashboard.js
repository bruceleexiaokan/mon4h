/**
 * cQuery JavaScript Library
 * http://www.ctrip.com/
 *
 * Copyright(C) 2008 - 2011, Ctrip All rights reserved.
 * Version: 110421
 *
 * Dashboard widget 1.0
 * Last modified by cdchu
 * Date: 2012-12-10
 */
var real_rate = 0;
var real_rate_show = 1 ;
	
;(function($){
	// module information
	var cls={
		name:'dashboard',
		version:'1.0',
		init:function(){},
		uninit:function(){},
		module:dashboard
	};

	var isHighChartReady=0;
	var highChartLoadingCallback=null;

	// * @cfg {object} ADDITION_EVENT 自定义事件列表
	var ADDITION_EVENT={
		'load':1,
		'click':1
	};
	// * @cfg {string} EVENT_TAIL 自定义事件后缀
	var EVENT_TAIL='_'+cls.name+'_'+cls.version;

	$.loader.js('../Highcharts-2.3.3/js/highcharts.src.js',{
		onload:function(){
			isHighChartReady=1;
			highChartLoadingCallback&&highChartLoadingCallback();

			var queueList=[
				'../Highcharts-2.3.3/js/highcharts-more.js',
				'../Highcharts-2.3.3/js/modules/exporting.src.js'
			];
			var queue=queueList.length;
			var queueCallback=function(){
				queue--;
				if (!queue){
					queueFinish();
				}
			};
			var queueFinish=function(){
				Highcharts.setOptions({
					global: {
						useUTC:false
					},
					lang:{
						//months:['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月'],
						//shortMonths:['一月','二月','三月','四月','五月','六月','七月','八月','九月','十月','十一月','十二月'],
						months:['01','02','03','04','05','06','07','08','09','10','11','12'],
						shortMonths:['01','02','03','04','05','06','07','08','09','10','11','12'],
						weekdays:['周日','周一', '周二','周三','周四','周五','周六'],
						loading:'正在加载数据，请稍候……',
						downloadJPEG:'导出JPEG图片',
						downloadPDF:'导出PDF图片',
						downloadPNG:'导出PNG图片',
						downloadSVG:'导出SVG图片',
						exportButtonTitle:'导出',
						printButtonTitle:'打印',
						hideButtonTitle:'全隐藏'
					},
					xAxis:{
						dateTimeLabelFormats:{
							second:'%H:%M:%S',
							minute:'%H:%M',
							hour:'%H:%M',
							day:'%b/%e',
							week:'%b/%e',
							month:'%y年%b月',
							year:'%Y'
						}
					},
				     plotOptions :  {   
				         series: {
				               connectNulls: true
				           }
				       }
					
				});
			};
			for (var i=0;i<queue;i++){
				$.loader.js(queueList[i],{onload:queueCallback});
			}
		},
		onerror:function(){
			$.error('dashboard.init','Error loading HighChart widget.');
		}
	});

	// dashboard class
	function dashboard(obj,opt){
		this._init(obj,opt);
	}

	// dashboard prototype extend
	$.extend(dashboard.prototype,{
		// public properties
		uid:null,
		target:null,
		filterContainer:null,
		chartContainer:null,
		title:null,
		bases:{
			'%chart%':'line',
			'%startTime%':null,
			'%endTime%':null,
			'%interval%':null,
			'%metric%':null
		},
		filters:{},
		groups:null,
		statistics:{
			aggregator:'sum',
			downsampler:'sum'
		},
		theme:'skies',
		template:{
			containerStyle:'\
					ul {\
						margin:0;\
						padding:0;\
						list-style:none;\
					}\
					li {\
						margin:0;\
						padding:0;\
					}\
					.title {\
						text-align:center;\
						font-family:"Microsoft Yahei";\
						font-size:16px;\
						font-weight:bold;\
						padding:15px 10px;\
					}\
				',
			container:'\
					<ul>\
						<li class="title">${title}<\/li>\
						<li>${filter}<\/li>\
						<li>${chart}<\/li>\
					</ul>\
				',
			filterStyle:'\
					dl {\
						margin:0 0 15px 0;\
						padding:10px;\
						overflow:hidden;\
						border-top:1px solid gray;\
						border-bottom:1px solid gray;\
						background:#EEE;\
						zoom:1;\
					}\
					dl * {\
						font-family:"Microsoft Yahei";\
						font-size:12px;\
						line-height:20px;\
					}\
					dt,dd {\
						padding:0;\
						margin:0;\
						float:left;\
					}\
					dt {\
						margin-left:20px;\
						margin-right:10px;\
					}\
				',
			filter:'\
					{{if (filters.length)}}\
						<dl>\
							{{each filters}}\
								<dt>${name}:<\/dt>\
								<dd>${html}<\/dd>\
							{{\/each}}\
						<\/dl>\
					{{/if}}\
				'
			},
		chart:null,
		visible:10,   //这里是说显示多少个？？？还是显示前多少？？？？
		// public methods
		reload:function(){
			var _this=this;
			clearInterval(this._realtimeClock);
			if(typeof(orPanel) != 'undefined')
			{
				realtimePanel.css('display','');
				orPanel.css('display','none');
				realtimePanel.html("<center><img  src='../info_loading.gif'></center>");
			}
			
			
				_this._loadData(function(){
				_this._loadHighcharts();
			});
			
			//orPanel.css('display','');
		},
		refresh:function(){
			var _this=this;
			clearInterval(this._realtimeClock);
			_this._loadTags(function(){
				if (_this._validateTags()){
					_this.reload();
				}
			});
		},
		bind:function(types,handler,opt){
			this._event('bind',types,handler,opt);
			return this;
		},
		unbind:function(types,handler){
			this._event('unbind',types,handler);
			return this;
		},
		trigger:function(types,opt){
			this._event('trigger',types,opt);
			return this;
		},
		uninit:function(){
			this.chart=null;
			clearInterval(this._realtimeClock);
			clearInterval(this._chartSizeClock);
			this.target.html();
		},
		// private properties
//		_requestURL:{
//			getTags:'GetMetricsTags.php?reqdata=${reqdata}&callback=${callback}',
//			getData:'GetDataPoints.php?reqdata=${reqdata}&callback=${callback}',
//			getGroupData:'GetGroupedDataPoints.php?reqdata=${reqdata}&callback=${callback}'
//		},
		_requestURL:{
			getTags:'[[query_engine_server/]/]/jsonp/getmetricstags?reqdata=${reqdata}&callback=${callback}',
			getData:'[[query_engine_server/]/]/jsonp/getdatapoints?reqdata=${reqdata}&callback=${callback}',
			getGroupData:'[[query_engine_server/]/]/jsonp/getgroupeddatapoints?reqdata=${reqdata}&callback=${callback}'
		},
		_usedRe:{
			interval:/^(\d+)([smhdMy])$/,
			dateTimeString1:/\b(\d)\b/g,
			dateTimeString2:/-/g,
			dateTimeString3:/^(\d+[^\d]+(?:\d+[^\d]+\d+[^\d]+)?)([^\|]*)\|\1/,
			dateTimeString4:/:00$/,
			createStyle:/(\s*)([^\{\}]+)\{/g,
			filterType:/^([@%#])(.+)\1$/,
			validateTags:/^([@%#])(.+)\1$/
		},
		_chartAvaliable:{
			'line':'line',
			'spline':'spline',
			'area':'area',
			'areaspline':'areaspline',
			'column':'column',
			'bar':'bar',
			'pie':'pie',
			'scatter':'scatter',
			'realtime':'line'
		},
		_statisticsAvaliable:{
			'sum':1,
			'max':1,
			'min':1,
			'avg':1,
			'dev':1,
			'mid':1
		},
		_themeAvaliable:{
			'dark-blue':1,
			'dark-green':1,
			'gray':1,
			'grid':1,
			'skies':1
		},
		_systemTagsAvaliable:{
			'startTime':1,
			'endTime':1,
			'interval':1,
			'statistics':1
		},
		_requestCount:0,
		_tagsAvaliable:[],
		_filters:[],
		_tags:{
			bases:{},
			filters:{}
		},
		_highChartOpt:null,
		_placeHolder:$.browser.isIE?'<pre style="display:none;">placeholder</pre>':'',
		_lastDateTime:null,
		_filterIdHash:{},
		_filterDataHash:{},
		_lastXAxisDateTime:null,
		_realtimeClock:null,
		_lastReqData:null,
		_seriesNameHash:{},
		_seriesIndexHash:{},
		_eventHash:[],
		_dataStatistics:{},
		_chartSizeClock:null,
		_lastChartSize:{
			width:null,
			height:null,
			oHeight:null
		},
		// private methods
		_init:function(obj,opt){
			// add uid
			this.uid=$.uid();

			// copy opt
			opt=$.copy(opt);

			// get target
			this.target=$(obj);

			// validate target
			if (!this.target){
				$.error('dashboard._init','Invalid target');
				return;
			}

			// get title
			this.title=opt.title||this.title;

			// get bases
			this.bases=$.extend(true,{},this.bases,opt.bases);

			// validate chart
			if (!this._validateChart(this.bases['%chart%'])){
				return false;
			}

			// validate startTime
			if (!this._validateStartTime(this.bases['%startTime%'])){
				return;
			}

			// validate endTime
			if (!this._validateEndTime(this.bases['%endTime%'])){
				return;
			}

			// validate interval
			if (!this._validateInterval(this.bases['%interval%'])){
				return;
			}

			// validate metric
			if (!this._validateMetric(this.bases['%metric%'])){
				return;
			}

			// get filters
			if (opt.filters){
				this.filters=$.extend(true,{},this.filters,opt.filters);
			}

			// get groups
			if (opt.groups){
				switch ($.type(opt.groups)){
					case 'string':
						this.groups=[opt.groups];
						break;
					case 'array':
						if (opt.groups.length){
							this.groups=$.copy(opt.groups);
						}
						break;
					default:
						break;
				}
			}

			// get statistics
			if (!this._validateStatistics(opt.statistics||this.statistics)){
				return false;
			}

			// get theme
			if (opt.theme&&this._themeAvaliable.hasOwnProperty(opt.theme)){
				this.theme=opt.theme;
			}

			// get template
			if (opt.template){
				this.template=$.extend(true,{},this.template,opt.template);
			}

			// bind methods
			$.bindMethod(this);

			// init container
			this._initContainer();

			// load data
			this.refresh();
		},
		_validateChart:function(chart){
			if (!chart){
				$.error('dashboard._validateChart','Invalid chart');
				return false;
			}else{
				if (this._chartAvaliable.hasOwnProperty(chart)){
					this.bases['%chart%']=chart;
				}else{
					$.error('dashboard._validateChart','Invalid chart: '+chart);
					return false;
				}
			}
			return true;
		},
		_validateStartTime:function(startTime){
			if (!startTime){
				$.error('dashboard._validateStartTime','Invalid startTime');
				return false;
			}else{
				switch ($.type(startTime)){
					case 'string':
						startTime=startTime.toDateTime();
						break;
					case 'number':
						startTime=new Date(startTime);
						break;
					case 'date':
						break;
					default:
						$.error('dashboard._validateStartTime','Invalid startTime type');
						return false;
				}
				if (!startTime){
					$.error('dashboard._validateStartTime','Invalid startTime: '+startTime);
					return false;
				}else{
					//startTime.setSeconds(0);
					this.bases['%startTime%']=this._toDateTimeString(startTime);
				}
			}
			return true;
		},
		_validateEndTime:function(endTime){
			if (!endTime){
				$.error('dashboard._validateEndTime','Invalid endTime');
				return false;
			}else{
				var startTime=this.bases['%startTime%'].toDateTime();
				switch ($.type(endTime)){
					case 'string':
						endTime=endTime.toDateTime();
						break;
					case 'number':
						endTime=new Date(endTime);
						break;
					case 'date':
						break;
					default:
						$.error('dashboard._validateEndTime','Invalid endTime type');
						return false;
				}
				if (!endTime){
					$.error('dashboard._validateEndTime','Invalid endTime: '+endTime);
					return false;
				}else{
				//	endTime.setSeconds(0);
					if (+endTime<+startTime){
						$.error('dashboard._validateEndTime','Invalid endTime, endTime must be later than startTime');
					
						//if(typeof(orPanel) != 'undefined')
					//	{
							orPanel('display','none');
						//}
						return false;
					}
					this.bases['%endTime%']=this._toDateTimeString(endTime);
				}
			}
			return true;
		},
		_validateInterval:function(interval){
			if (!interval){
				$.error('dashboard._validateInterval','Invalid interval');
				return false;
			}else{
				var arr=interval.toString().match(this._usedRe.interval);
				if (!arr||arr[1].toInt()==0){
					$.error('dashboard._validateInterval','Invalid interval: '+interval);
					return false;
				}
				this.bases['%interval%']=interval;
			}
			return true;
		},
		_validateMetric:function(metric){
			if (!metric){
				$.error('dashboard._init','Invalid metric');
				return false;
			}
			this.bases['%metric%']=metric;
			return true;
		},
		_validateStatistics:function(statistics){
			switch ($.type(statistics)){
				case 'string':
					if (this._statisticsAvaliable.hasOwnProperty(statistics)){
						this.statistics={
							aggregator:statistics,
							downsampler:statistics
						};
						return true;
					}
					break;
				case 'object':
					if (this._statisticsAvaliable.hasOwnProperty(statistics.aggregator)&&this._statisticsAvaliable.hasOwnProperty(statistics.downsampler)){
						this.statistics={
							aggregator: statistics.aggregator,
							downsampler:statistics.downsampler
						};
						return true;
					}
					break;
				default:
					break;
			}
			$.error('dashboard._validateStatistics','Invalid statistics');
			return false;
		},
		_initContainer:function(){
			var filterContainerUid=$.uid();
			var chartContainerUid=$.uid();
			var containerStyle=this._createStyle(this.target,this.template.containerStyle);
			this.target.html(containerStyle+$.tmpl.render(this.template.container,{
				title:this.title||'Dashboard - '+this.bases['%metric%'],
				filter:'<div id="'+filterContainerUid+'"></div>',
				chart:'<div id="'+chartContainerUid+'"></div>'
			}));
			this.filterContainer=$('#'+filterContainerUid).parentNode();
			this.chartContainer=$('#'+chartContainerUid).parentNode();
			this.chartContainer.html();
			this._initFilters();
			this._initChartSize();
			clearInterval(this._chartSizeClock);
			this._chartSizeClock=this._initChartSize.repeat(200);
		},
		_createStyle:function(el,style){
			el=$(el);
			var id=el[0].id=el[0].id||el.uid();
			return this._placeHolder+'<style>'+style.replace(this._usedRe.createStyle,'$1#'+id+' $2{')+'</style>';
		},
		_initFilters:function(){
			this._filters=[];
			for (var filterKey in this.filters){
				if (this.filters.hasOwnProperty(filterKey)){
					var filter=this._getFilter(filterKey,this.filters[filterKey]);
					if (filter){
						this._filters.push(filter);
					}
				}
			}
			var filterStyle=this._createStyle(this.filterContainer,this.template.filterStyle);
			this.filterContainer.html(filterStyle+$.tmpl.render(this.template.filter,{
				filters:this._filters
			}));
			this._initFilterEvent();
		},
		_initFilterEvent:function(){
			this.filterContainer.find('input,select').bind('change',this._filterChange);
		},
		_getChartSize:function(){
			var t1=this.target.offset();
			var t2=this.filterContainer.offset();
			return {
				width:t2.width,
				height:t1.top+t1.height-t2.top-t2.height
			}
		},
		_initChartSize:function(){
			var t=this._getChartSize();
			var w=t.width;
			var h=t.height;
			if (h>0){
				if (h!=this._lastChartSize.oHeight){
					this.chartContainer.css('height',h+'px');
					this._lastChartSize.oHeight=h;
				}
				if (this.chart){
					if (w!=this._lastChartSize.width||h!=this._lastChartSize.height){
						this.chart.setSize(w,h);
						this._lastChartSize.width=w;
						this._lastChartSize.height=h;
					}
				}
			}
		},
		_filterChange:function(e){
			var el=$(e.target);
			var elId=el[0].id;
			var elType=el[0].tagName.toLowerCase();
			var elVal=el.value();
			var elAllVal=[];
			var err=null;
			if (elType=='select'){
				for (var i=0,n=el[0].options.length;i<n;i++){
					var t=el[0].options[i].value;
					if (t){
						elAllVal.push(t);
					}
				}
			}
			var filter=this._filterIdHash[elId];
			if (!filter){
				return;
			}
			switch (filter.type){
				case '@':
					this._tags.filters[filter.name]=elVal?[elVal]:elAllVal;
					break;
				case '#':
					var customFilter=this._filterDataHash[elVal];
					for (var key in customFilter){
						if (customFilter.hasOwnProperty(key)){
							var arr=key.match(this._usedRe.validateTags);
							if (arr){
								var type=arr[1];
								var name=arr[2];
								var filterValue=customFilter[key];
								var filterValueType=$.type(filterValue);
								switch (type){
									case '@':
										switch (filterValueType){
											case 'string':
												this._tags.filters[name]=filterValue?[filterValue]:[];
												break;
											case 'array':
												this._tags.filters[name]=filterValue;
												break;
											default:
												break;
										}
									case '%':
										switch (name){
											case 'startTime':
												if (!this._validateStartTime(filterValue)){
													err='Invalid startTime';
												}
												break;
											case 'endTime':
												if (!this._validateEndTime(filterValue)){
													err='Invalid endTime';
												}
												break;
											case 'interval':
												if (!this._validateInterval(filterValue)){
													err='Invalid interval';
												}
												break;
											case 'statistics':
												if (!this._validateStatistics(filterValue)){
													err='Invalid statistics';
												}
												break;
											default:
												return;
										}
										break;
									default:
										break;
								}
							}
						}
					}
					break;
				case '%':
					switch (filter.name){
						case 'startTime':
							if (!this._validateStartTime(elVal)){
								err='Invalid startTime';
							}
							break;
						case 'endTime':
							if (!this._validateEndTime(elVal)){
								err='Invalid endTime';
							}
							break;
						case 'interval':
							if (!this._validateInterval(elVal)){
								err='Invalid interval';
							}
							break;
						case 'statistics':
							if (!this._validateStatistics(elVal)){
								err='Invalid statistics';
							}
							break;
						default:
							return;
					}
					break;
				default:
					break;
			}
			if (err){
				alert(err);
				el.value(el[0].lastValue||el[0].defaultValue);
			}else{
				el[0].lastValue=elVal;
				this.reload();
			}
		},
		_getFilter:function(filterKey,filterValue){
			var t,c;
			t=filterKey.match(this._usedRe.filterType);
			if (t){
				var filter={
					type:t[1],
					name:t[2],
					html:''
				};
				var filterValueType=$.type(filterValue);
				switch (filter.type){
					case '@':
						switch (filterValueType){
							case 'string':
								filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="'+filterValue+'" />';
								break;
							case 'array':
								if (filterValue.length){
									filter.html='<select id="'+this._getFilterId(filter.type,filter.name)+'"><option value="">All</option>';
									for (var i=0,n=filterValue.length;i<n;i++){
										filter.html+='<option value="'+filterValue[i]+'">'+filterValue[i]+'</option>';
									}
									filter.html+='</select>';
								}else{
									filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="" />';
								}
								break;
							case 'object':
								if (filterValue.avaliable&&$.type(filterValue.avaliable)=='array'&&filterValue.avaliable.length){
									filter.html='<select id="'+this._getFilterId(filter.type,filter.name)+'"><option value="">All</option>';
									for (var i=0,n=filterValue.avaliable.length;i<n;i++){
										filter.html+='<option value="'+filterValue.avaliable[i]+'"'+(filterValue.avaliable[i]===filterValue.selected?' selected':'')+'>'+filterValue.avaliable[i]+'</option>';
									}
									filter.html+='</select>';
								}else{
									filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="" />';
								}
								break;
							default:
								break;
						}
						break;
					case '#':
						switch (filterValueType){
							case 'object':
								if (filterValue.avaliable&&$.type(filterValue.avaliable)=='object'){
									t='<select id="'+this._getFilterId(filter.type,filter.name)+'">';
									c=0;
									for (var key in filterValue.avaliable){
										if (filterValue.avaliable.hasOwnProperty(key)){
											t+='<option value="'+this._getFilterDataId(filterValue.avaliable[key])+'"'+(key===filterValue.selected?' selected':'')+'>'+key+'</option>';
											c++;
										}
									}
									t+='</select>';
									if (c){
										filter.html=t;
									}
								}
								break;
							default:
								break;
						}
						break;
					case '%':
						switch (filterValueType){
							case 'string':
								filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="'+(filterValue||this.bases['%'+filter.name+'%'])+'" />';
								break;
							case 'array':
								if (filterValue.length){
									filter.html='<select id="'+this._getFilterId(filter.type,filter.name)+'">';
									for (var i=0,n=filterValue.length;i<n;i++){
										filter.html+='<option value="'+filterValue[i]+'">'+filterValue[i]+'</option>';
									}
									filter.html+='</select>';
								}else{
									filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="" />';
								}
								break;
							case 'object':
								if (filterValue.avaliable&&$.type(filterValue.avaliable)=='array'&&filterValue.avaliable.length){
									filter.html='<select id="'+this._getFilterId(filter.type,filter.name)+'">';
									for (var i=0,n=filterValue.avaliable.length;i<n;i++){
										filter.html+='<option value="'+filterValue.avaliable[i]+'"'+(filterValue.avaliable[i]===filterValue.selected?' selected':'')+'>'+filterValue.avaliable[i]+'</option>';
									}
									filter.html+='</select>';
								}else{
									filter.html='<input id="'+this._getFilterId(filter.type,filter.name)+'" type="text" value="" />';
								}
								break;
							default:
								break;
						}
						break;
					default:
						break;
				}
				if (filter.html){
					return filter;
				}else{
					return null;
				}
			}else{
				return null;
			}
		},
		_getFilterId:function(type,name){
			var id=$.uid();
			this._filterIdHash[id]={
				type:type,
				name:name
			};
			return id;
		},
		_getFilterDataId:function(data){
			var id=$.uid();
			this._filterDataHash[id]=data;
			return id;
		},
		_toDateTimeString:function(dateTime,isMinify){
			var t=dateTime.toStdDateTimeString();
			t=t.replace(this._usedRe.dateTimeString1,'0$1');
			if (isMinify){
				var l=this._lastDateTime;
				this._lastDateTime=t;
				if (l){
					var arr=(t+'|'+l).match(this._usedRe.dateTimeString3);
					if (arr){
						t=arr[2];
					}
				}
				t=t.replace(this._usedRe.dateTimeString2,'/');
				t=t.replace(this._usedRe.dateTimeString4,'');
			}
			return t;
		},
		_getRequestCount:function(){
			return ++this._requestCount;
		},
		_isCurrentRequest:function(t){
			return this._requestCount==t;
		},
		_loadTags:function(callback){
			var _this=this;
			var uid='dashboard_loadTags_'+$.uid();
			var done=0;
			var reqUid=this._getRequestCount();
			$.tmp[uid]=function(t){
				done=1;
				if (!_this._isCurrentRequest(reqUid)){
					return;
				}
				if ($.type(t)!=='object'||t['result-code']!==0){
					$.error('dashboard._loadTags','Found error: '+$.stringifyJSON(t));
					return;
				}
				t=t['time-series-list'][0];
				if (!t){
					$.error('dashboard._loadTags','Unknown metric: '+_this.bases['%metric%']);
					return;
				}
				_this._tagsAvaliable=t.tags||[];
				callback&&callback();
			};
			
			var namespaceValueTemp="";
			if( typeof(namespace) != 'undefined' ) {
				namespaceValueTemp=namespace.value();
			}
			if( namespaceValueTemp=="" ) {
				namespaceValueTemp = null;
			}
			
			var reqData={
				"version":1,
				"time-series-pattern":{
					"namespace":namespaceValueTemp,
					"metrics-name":{
						"equals":this.bases['%metric%']
					}
				}
			};
			var url=$.tmpl.render(this._requestURL.getTags,{
				reqdata:encodeURIComponent($.stringifyJSON(reqData)),
				callback:encodeURIComponent('cQuery.tmp["'+uid+'"]')
			});
			var evt=function(){
				if (!done){
					if(onlyOnce == 0){
						alert('Loading tags error, error server response. js 886');
						onlyOnce = 1;
					}
				
				}else{
					onlyOnce = 0;
					delete cQuery.tmp[uid];
				}
			};
			$.loader.js(url,{
				async:true,
				charset:'utf-8',
				onload:evt,
				onerror:evt
			});
		},
		_validateTags:function(){
			this._tags={
				bases:{},
				filters:{}
			};
			var tags={
				bases:{},
				filters:{}
			};
			for (var key in this.bases){
				if (this.bases.hasOwnProperty(key)){
					var arr=key.match(this._usedRe.validateTags);
					if (arr){
						var type1=arr[1];
						var name1=arr[2];
						if (type1=='@'){
							if (this._tagsAvaliable.indexOf(name1)==-1){
								$.error('dashboard._validateTags','Unknown tag: '+name1);
								return false;
							}
							var filterValue=this.bases[key];
							var filterValueType=$.type(filterValue);
							switch (filterValueType){
								case 'string':
									if (filterValue){
										tags.bases[name1]=[filterValue];
									}
									break;
								case 'array':
									if (filterValue.length){
										tags.bases[name1]=filterValue;
									}
									break;
								default:
									break;
							}
						}
					}
				}
			}
			for (var key1 in this.filters){
				if (this.filters.hasOwnProperty(key1)){
					var arr=key1.match(this._usedRe.validateTags);
					if (arr){
						var type1=arr[1];
						var name1=arr[2];
						var filterValue=this.filters[key1];
						var filterValueType=$.type(filterValue);
						switch (type1){
							case '@':
								if (this._tagsAvaliable.indexOf(name1)==-1){
									$.error('dashboard._validateTags','Unknown tag: '+name1);
									return false;
								}
								switch (filterValueType){
									case 'string':
										if (filterValue){
											tags.filters[name1]=[filterValue];
										}
										break;
									case 'array':
										if (filterValue.length){
											tags.filters[name1]=filterValue;
										}
										break;
									case 'object':
										if (filterValue.avaliable&&$.type(filterValue.avaliable)=='array'&&filterValue.avaliable.length){
											for (var i=0,n=filterValue.avaliable.length;i<n;i++){
												if (filterValue.avaliable[i]===filterValue.selected){
													tags.filters[name1]=[filterValue.avaliable[i]];
												}
											}
										}
										break;
									default:
										break;
								}
								break;
							case '#':
								switch (filterValueType){
									case 'object':
										if (filterValue.avaliable&&$.type(filterValue.avaliable)=='object'){
											if (filterValue.avaliable.hasOwnProperty(filterValue.selected)){
												var customFilter=filterValue.avaliable[filterValue.selected];
												if (customFilter&&$.type(customFilter)=='object'){
													for (var key2 in customFilter){
														if (customFilter.hasOwnProperty(key2)){
															var arr=key2.match(this._usedRe.validateTags);
															if (arr){
																var type2=arr[1];
																var name2=arr[2];
																var filterValue=customFilter[key2];
																var filterValueType=$.type(filterValue);
																switch (type2){
																	case '@':
																		if (this._tagsAvaliable.indexOf(name2)==-1){
																			$.error('dashboard._validateTags','Unknown tag: '+name2);
																			return false;
																		}
																		switch (filterValueType){
																			case 'string':
																				if (filterValue){
																					tags.filters[name2]=[filterValue];
																				}
																				break;
																			case 'array':
																				if (filterValue.length){
																					tags.filters[name2]=filterValue;
																				}
																				break;
																			default:
																				break;
																		}
																		break;
																	case '%':
																		if (!this._systemTagsAvaliable.hasOwnProperty(name2)){
																			$.error('dashboard._validateTags','Unknown tag: '+name2);
																			return false;
																		}
																		var validator='_validate'+name2.slice(0,1).toUpperCase()+name2.slice(1);
																		switch (filterValueType){
																			case 'string':
																				if (filterValue){
																					this[validator](filterValue);
																				}
																				break;
																			case 'array':
																				if (filterValue.length){
																					this[validator](filterValue[0]);
																				}
																				break;
																			case 'object':
																				if (filterValue.avaliable&&$.type(filterValue.avaliable)=='object'){
																					if (filterValue.avaliable.hasOwnProperty(filterValue.selected)){
																						this[validator](filterValue.avaliable[filterValue.selected]);
																					}else{
																						$.error('dashboard._validateTags','Invalid selected: '+filterValue.selected);
																						return false;
																					}
																				}
																				break;
																			default:
																				break;
																		}
																		break;
																	default:
																		break;
																}
															}
														}
													}
												}
											}else{
												$.error('dashboard._validateTags','Invalid selected: '+filterValue.selected);
												return false;
											}
										}
										break;
									default:
										break;
								}
								break;
							case '%':
								if (!this._systemTagsAvaliable.hasOwnProperty(name1)){
									$.error('dashboard._validateTags','Unknown tag: '+name1);
									return false;
								}
								var validator='_validate'+name1.slice(0,1).toUpperCase()+name1.slice(1);
								switch (filterValueType){
									case 'string':
										if (filterValue){
											this[validator](filterValue);
										}
										break;
									case 'array':
										if (filterValue.length){
											this[validator](filterValue[0]);
										}
										break;
									case 'object':
										if (filterValue.avaliable&&$.type(filterValue.avaliable)=='object'){
											if (filterValue.avaliable.hasOwnProperty(filterValue.selected)){
												this[validator](filterValue.avaliable[filterValue.selected]);
											}else{
												$.error('dashboard._validateTags','Invalid selected: '+filterValue.selected);
												return false;
											}
										}
										break;
									default:
										break;
								}
							default:
								break;
						}
					}
				}
			}
			this._tags=tags;
			return true;
		},
		_loadData:function(callback){
			var _this=this;
			
			var namespaceValueTemp="";
			if( typeof(namespace) != 'undefined' ) {
				namespaceValueTemp=namespace.value();
			}
			if( namespaceValueTemp=="" ) {
				namespaceValueTemp = null;
			}
			
			var reqData={
				"version": 1, 
				"time-series-pattern":{
					"namespace":namespaceValueTemp, 
					"metrics-name":this.bases['%metric%'], 
					"tag-search-part":$.extend({},this._tags.bases,this._tags.filters)
				},
				"aggregator":{
					"accept-linear-interpolation":true,
					"function":this.bases['statistics']||'sum'
				},
				"downsampler":{
					"interval":this.bases['%interval%'], 
					"function":this.bases['downfunc']||'sum'
				},
				"max-datapoint-count":this.bases['%chart%']=='pie'?1:100,
				"start-time":this.bases['%startTime%'], 
				"end-time":this.bases['%endTime%'],
				"rate":this.bases['%rate%']||'false'
			};
			

			
			if (this.groups){
				reqData["group-by"]=this.groups;
				for (var i=0,n=this.groups.length;i<n;i++){
					if (!reqData["time-series-pattern"]["tag-search-part"].hasOwnProperty(this.groups[i])){
						reqData["time-series-pattern"]["tag-search-part"][this.groups[i]]=[];
					}
				}
			}
			
			this._sendDataRequest(reqData,function(t){
				if ($.type(t)!=='object'||t['result-code']!==0){
					if(t['result-info'] == 'Successed, but no data for this query.') {
					//	if(typeof(orPanel) != 'undefined')
					//	{
							orPanel.css('display','none');
							realtimePanel.css('display','');
							realtimePanel.html("<center><img  src='../error.gif'></center>");
					//	}
						return false;
					}
					 
					if(t['result-info'] !== 'Successed'){
						
						orPanel.css('display','none');
						realtimePanel.css('display','');
						realtimePanel.html("<center><img  src='../error.gif'></center>");;
						return false;
					} 
					$.error('dashboard._loadData','Found error: '+$.stringifyJSON(t));
					return false;
				}
				
				realtimePanel.css('display','none');
				
//				if(t['time-series-group-list'][0]['data-points']['result-last-value'] !='undefined'){
//					
//					if(typeof(real_rate_show)!='undefined'){
//							if(real_rate_show == 0){
//								for(var i = 0; i < t['time-series-group-list'].length; i++){
//									lastV.push(t['time-series-group-list'][i]['data-points']['result-last-value']);
//							
//						}
//						
//								real_rate_show = 1;
//							}
//					}
//				}
//				

				if(typeof(callbackFunction) != 'undefined')
					t = callbackFunction(t);
				


				if (_this._convertOpt(t)){
					callback&&callback();
				}
			});
		},
		_sendDataRequest:function(reqData,callback){
			var _this=this;
			var uid='dashboard_loadData_'+$.uid();
			var done=0;
			var reqUid=this._getRequestCount();
			$.tmp[uid]=function(t){
				done=1;
				if (!_this._isCurrentRequest(reqUid)){
					return;
				}
				callback&&callback(t);
			};
			
			
//			
//			if(_this.bases['%rate%'] == 'true' & _this.bases['%chart%'] == 'realtime' ){
//				if( real_rate ==0){
//					reqData["rate"] = 'true';
//					real_rate = 1;
//					real_rate_show = 0;
//				}
//				else{
//					reqData["rate"] = 'false';
//							//real_rate ++;
//					}
//			}	
//			else{
//				real_rate =0;
//				lastV = [];
//			}
//			
			
			this._lastReqData=reqData;
			var url=$.tmpl.render(this._requestURL.getGroupData,{
				reqdata:encodeURIComponent($.stringifyJSON(reqData)),
				callback:encodeURIComponent('cQuery.tmp["'+uid+'"]')
			});
			var evt=function(){
				if (!done){
					if(onlyOnce == 0){
						onlyOnce = 1;
					alert('Loading data error, error server response. js 1224');
					}	
					return false;
				}else{
					onlyOnce = 0;
					delete cQuery.tmp[uid];
				}
			};
			$.loader.js(url,{
				async:true,
				charset:'utf-8',
				onload:evt,
				onerror:evt
			});
		},
		_getEventId:function(type,handler){
			var id='dashboard_event_'+$.uid();
			this._eventHash.push({
				id:id,
				type:type,
				handler:handler
			});
			return id;
		},
		_sortNumber:function(a,b){
			return a==b?1:(a>b?1:-1);
		},
		_calcLimit:function(callback){
			var n=this._dataStatistics.data.length;
			if (n<this.visible){
				return;
			}
			this._dataStatistics.count=[];
			this._dataStatistics.avg=[];
			this._dataStatistics.max=[];
			this._dataStatistics.avgLimit=Infinity;
			this._dataStatistics.maxLimit=Infinity;
			for (var i=0;i<n;i++){
				this._dataStatistics.count[i]=0;
				this._dataStatistics.avg[i]=0;
				this._dataStatistics.max[i]=-Infinity;
				var data=this._dataStatistics.data[i];
				for (var j=0,m=data.length;j<m;j++){
					if (data[j]!==null){
						this._dataStatistics.count[i]++;
						this._dataStatistics.avg[i]+=data[j];
						if (data[j]>this._dataStatistics.max[i]){
							this._dataStatistics.max[i]=data[j];
						}
					}
				}
				this._dataStatistics.avg[i]/=this._dataStatistics.count[i];
			}
			this._dataStatistics._avg=$.copy(this._dataStatistics.avg);
			this._dataStatistics._avg.sort(this._sortNumber);
			this._dataStatistics._max=$.copy(this._dataStatistics.max);
			this._dataStatistics._max.sort(this._sortNumber);
			this._dataStatistics.avgLimit=this._dataStatistics._avg[n-this.visible];
			this._dataStatistics.maxLimit=this._dataStatistics._max[n-this.visible];
			for (var i=0;i<n;i++){
				callback(i,
					this._dataStatistics.avg[i]>=this._dataStatistics.avgLimit ||
					this._dataStatistics.max[i]>=this._dataStatistics.maxLimit
				);
			}
		},
		_convertOpt:function(data){
			var _this=this;
			var series=data['time-series-group-list'];
			if (!series.length){
				$.error('dashboard._convertOpt','None data.');
				return false;
			}
			if (!this.chartContainer[0]){
				$.error('dashboard._convertOpt','None chartContainer.');
				return false;
			}
			// todo
//			switch (this.bases['%chart%']){
//				case '':
//					break;
//				case '':
//					break;
//				default:
//					break;
//			}
			var t=this._getChartSize();
			this._lastChartSize.width=t.width;
			this._lastChartSize.height=t.height;
			this._highChartOpt={
				chart:{
					renderTo:this.chartContainer[0],
					type:this._chartAvaliable[this.bases['%chart%']],
					events:{
						load:function(){
							_this._chartLoad(this);
						}
					},
					width:t.width,
					height:t.height,
					reflow:false
				},
				title:null,
				xAxis:{
					type:'datetime'
				},
				yAxis:{},
				plotOptions:{
					series:{
						cursor:'pointer',
						connectNulls:true,
						events:{
							click:function(e){
								return _this._serieClick(e,this);
							},
							legendItemClick:function(e){
								return _this._legendClick(e,this);
							}
						}
					},
					pie:{
						dataLabels:{
							formatter:function(){
								var point=this.point;
								var id=_this._getEventId('click',function(e){
									_this._dataLabelClick(e,point);
								});
								return '<span id="'+id+'" style="font-size:14px;font-weight:bold;color:'+point.color+';cursor:pointer;">'+point.name+'</span>';
							},
							connectorWidth:2,
							useHTML:true
						}
					}
				},
				legend:{
					align: 'right',
					verticalAlign: 'top',
					layout:'vertical',
					y:30
				},
				series:[],
				exporting:{
					chartOptions:{
						title:{
							text:this.title,
							style:{
								fontFamily:'微软雅黑'
							}
						}
					},
					url:'[[export_svg_server/]/]/dashboard-ui/export/',
					width:this.chartContainer.offset().width
				},
				credits:{
					enabled:false
				}
			};

			this._lastDateTime=null;
			var baseTime=series[0]['data-points']['base-time'].toDateTime();
			var interval=this.bases['%interval%']=series[0]['data-points']['interval'];
			var categories=[];
			for (var i=0,n=series[0]['data-points']['data-points'].length;i<n;i++){
				var t=this._addTime(baseTime,interval,i+1);
				categories.push(t.getTime());
			}
			this._lastXAxisDateTime=t;

			switch (this._highChartOpt.chart.type){
				case 'area':
				case 'bar':
				case 'column':
				case 'line':
				case 'spline':
					//this._highChartOpt.xAxis.categories=categories;
					$.extend(true,this._highChartOpt,{
						tooltip:{
							crosshairs:true,
							formatter:function(){
								return '<strong>'+_this._toDateTimeString(new Date(this.x))+'</strong><br />'+this.point.series.name+' <span style="color:'+this.point.series.color+';font-weight:bold;">'+this.y+'</span>';
							}
						}
					});
					// clear
					this._seriesIndexHash={};
					this._dataStatistics={
						data:[]
					};
					for (var i=0,n=series.length;i<n;i++){
						var o={
							name:this._createSeriesName(series[i]['time-series-group'],this.bases['%metric%']),
							data:[]
						};
						this._seriesIndexHash[o.name]=i;
						this._dataStatistics.data[i]=[];
						var d=series[i]['data-points']['data-points'];
						for (var j=0,m=d.length;j<m;j++){
							var t=d[j];
							this._dataStatistics.data[i].push(t);
							//alert(t);
							if(t != null){
								if (parseInt(t,10)!=t){
									t=Math.round(t*1000)/1000;
								}
							}
							o.data.push([categories[j],t]);
						//	alert(t);
						};
						this._highChartOpt.series.push(o);
					}
					
					this._calcLimit(function(i,t){
						_this._highChartOpt.series[i].visible=t;
					});
					break;
				case 'pie':
//					var t=series[0]['data-points']['data-points'].length;
//					var p=this.chartContainer.offset();
//					var xCount=Math.floor(p.width/Math.sqrt(p.width*p.height/t));
//					var size=Math.floor(p.width/xCount);
					$.extend(true,this._highChartOpt,{
						tooltip:{
							formatter:function(){
								return '<strong>'+this.point.series.name+'</strong><br />'+this.point.name+' <span style="color:'+this.point.color+';font-weight:bold;">'+this.y+' ( '+this.percentage.toFixed(2)+'% )'+'</span>';
							}
						}
					});
					var o=this._highChartOpt.series;
					for (var i=0,n=series.length;i<n;i++){
						var d=series[i]['data-points']['data-points'];
						var t=this._createSeriesName(series[i]['time-series-group'],this.bases['%metric%']);
						for (var j=0,m=d.length;j<m;j++){
							if (!o[j]){
								o[j]={
									name:this._toDateTimeString(new Date(categories[j])),
//									center:[
//										Math.floor((j%xCount+0.5)*size),
//										(Math.floor(j/xCount)+0.5)*size
//									],
//									size:size/3,
									data:[]
								}
							}
							o[j].data.push([t,d[j]]);
						}
					}
//					this._highChartOpt.series=[this._highChartOpt.series[0]];
					break;
				default:
					break;
			}
			return true;
		},
		_addTime:function(baseTime,interval,times){
			var arr=interval.match(this._usedRe.interval);
			if (!arr){
				return null;
			}
			var val=arr[1].toInt();
			var unit=arr[2]||'s';
			var ret;
			switch (unit){
				case 's':
					ret=baseTime.addSeconds(val*times);
					break;
				case 'm':
					ret=baseTime.addMinutes(val*times);
					break;
				case 'h':
					ret=baseTime.addHours(val*times);
					break;
				case 'd':
					ret=baseTime.addDays(val*times);
					break;
				case 'M':
					ret=baseTime.addMonths(val*times);
					break;
				case 'y':
					ret=baseTime.addYears(val*times);
					break;
				default:
					return null;
			}
			return ret;
		},
		_createSeriesName:function(hash,defVal){
			var arr=[];
			for (var name in hash){
				if (hash.hasOwnProperty(name)){
					arr.push(name+':'+hash[name]);
				}
			}
			var t=(arr.length?arr.join(';'):defVal)||null;
			if (t){
				this._seriesNameHash[t]=hash;
			}
			return t;
		},
		_loadHighcharts:function(callback){
			if (!isHighChartReady){
				highChartLoadingCallback=function(){
					this._loadHighcharts();
				};
				return;
			}
		//	alert(1530);
			if(typeof(orPanel) != 'undefined')
			{
			 realtimePanel.css('display','none');
			 orPanel.css('display','');
			}

			this.chartContainer.html("");
			this._eventHash=[];
			this.chart=new Highcharts.Chart(this._highChartOpt);
		},
		_chartLoad:function(chart){
			//alert(1537);
			
			var _this=this;
			if (_this.bases['%chart%']=='realtime'){
				clearInterval(this._realtimeClock);
				_this._realtimeClock=setInterval(function(){
					var startTime=_this._lastXAxisDateTime=_this._addTime(_this._lastXAxisDateTime,_this.bases['%interval%'],1);
					var endTime=_this._addTime(startTime,_this.bases['%interval%'],1);
					var reqData=$.extend({},_this._lastReqData,{
						"max-datapoint-count":1, 
						"start-time":_this._toDateTimeString(startTime), 
						"end-time":_this._toDateTimeString(endTime)
					});
					_this._sendDataRequest(reqData,function(t){
						if ($.type(t)!=='object'||t['result-code']!==0){
							$.error('dashboard._chartLoad','Found error: '+$.stringifyJSON(t));
							return;
						}
						t=t['time-series-group-list'];
						if (t.length){
							var x=_this._addTime(t[0]['data-points']['base-time'].toDateTime(),_this.bases['%interval%'],1).getTime();
							var done={};
							for (var i=0,n=t.length;i<n;i++){
								var name=_this._createSeriesName(t[i]['time-series-group'],_this.bases['%metric%']);
								var j;
								if (_this._seriesIndexHash.hasOwnProperty(name)){
									j=_this._seriesIndexHash[name];
								}else{
									chart.addSeries({
										name:name,
										data:[]
									},false,true);
									j=_this._seriesIndexHash[name]=chart.series.length-1;
									_this._dataStatistics.data[j]=[];
									for (var k=0,l=_this._dataStatistics.data[0].length;k<l;k++){
										_this._dataStatistics.data[j].push(null);
									}
								}
								var y=t[i]['data-points']['data-points'][0];
								//if(lastV[i]  )
							//	alert("heher             " + lastV[i]);
//								if(document.getElementById("is_rate")!=null ) {
//									if(document.getElementById("is_rate").checked) {
//									
//										var re = (y - lastV[i])|| 0;
//										lastV[i] = y;
//										y = re;
//									}
//								}
								
								chart.series[j].addPoint([x,y],false,true);
								_this._dataStatistics.data[j].shift();
								_this._dataStatistics.data[j].push(y);
								done[name]=1;
							}
							for (var i=0,n=chart.series.length;i<n;i++){
								if (!done.hasOwnProperty(chart.series[i].name)){
									chart.series[i].addPoint([x,0],false,true);
								}
							}
							_this._calcLimit(function(i,t){
								chart.series[i].visible=t;
							});
							chart.redraw();
						}
					});
				},this._addTime(new Date(0),_this.bases['%interval%'],1).getTime());  // 以后可能会在这里更新一下！！！！动态确定长度
			}
			for (var i=0,n=this._eventHash.length;i<n;i++){
				var t=this._eventHash[i];
				$('#'+t.id).bind(t.type,t.handler);
			}
			
			this.trigger('load',{
				arguments:chart
			});
			chart.hideLoading();
		},
		_serieClick:function(e,serie){
			if (this.bases['%chart%']=='pie'){
				var serieName=e.point.config[0];
			}else{
				var serieName=e.point.series.name;
			}

			var data={
				name:serieName,
				type:'serieClick',
				x:e.point.config[0],
				y:e.point.config[1],
				interval:this.bases['%interval%'],
				extBases:{}
			};

			if (this.bases['%chart%']=='pie'){
				var group=this._seriesNameHash[serieName];
			}else{
				var t=new Date(e.point.config[0]);
				data.extBases['%startTime%']=this._toDateTimeString(this._addTime(t,this.bases['%interval%'],-1));
				data.extBases['%endTime%']=this._toDateTimeString(t);
				var group=this._seriesNameHash[serieName];
			}

			if (group){
				data.group=$.copy(group);
				for (var key in data.group){
					if (data.group.hasOwnProperty(key)){
						data.extBases['@'+key+'@']=data.group[key];
					}
				}
			}

			this.trigger('click',{
				arguments:data
			});
		},
		_legendClick:function(e,series){
			var be=cQuery.event.fixProperty(e.browserEvent);
			var data={
				name:e.name,
				type:'legendClick'
			};
			this.trigger('click',{
				arguments:data
			});
		},
		_dataLabelClick:function(e,point){
			var data={
				name:point.name,
				type:'serieClick',
				x:point.config[0],
				y:point.config[1],
				interval:this.bases['%interval%'],
				extBases:{}
			};
			var group=this._seriesNameHash[point.name];
	
			if (group){
				data.group=$.copy(group);
				for (var key in data.group){
					if (data.group.hasOwnProperty(key)){
						data.extBases['@'+key+'@']=data.group[key];
					}
				}
			}

			this.trigger('click',{
				arguments:data
			});
		},
		_event:function(operate,types){
			switch ($.type(types)){
				case 'string':
					types=[types];
					break;
				case 'array':
					break;
				default:
					$.error('dashboard.'+operate,'Invalid types '+types);
					return;
			}
			var type;
			for (var i=0,n=types.length;i<n;i++){
				type=types[i].trim();
				if (!type||$.type(type)!='string'){
					$.error('dashboard.'+operate,'Invalid type '+type);
					continue;
				}
				if (type in ADDITION_EVENT){
					this.target[operate].apply(this.target,[type+'_'+this.uid+EVENT_TAIL].concat(Array.prototype.slice.call(arguments,2)));
				}else{
					$.error('dashboard.'+operate,'Unsupport type '+type);
				}
			}
		}
	});

	// reg module
	$.mod.reg(cls);
})(cQuery);

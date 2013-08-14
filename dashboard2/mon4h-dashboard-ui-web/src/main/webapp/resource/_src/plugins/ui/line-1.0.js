define( ['jquery','chart'], function( $,ct ) {
	//var pie = new chart('pie')
	function chart(){
		this.init();
	}
	chart.prototype = {
		init:function(){
			var self = this;
			this.defaults = {
				chart: {
					type: 'spline'
				},
				title: {
					text: null
				},
				subtitle: {
					text: '',
					x: -20
				},
				xAxis: {
					categories: [],
					tickInterval: 1,
					labels: {
						rotation: 0,
						align: 'center',
						style: {
							font: 'normal 12px Microsoft Yahei'
						}
					}
				},
				yAxis: {
					title: {
						text: null
					},
					 // min: -0.1,
					labels: {
						style: {
							font: 'normal 12px Microsoft Yahei'
						}
					}
				},
				tooltip: {
					crosshairs: true,
					shared: true,
					formatter:function(){
						var no = 0;
						var arr = [];
						var points = this.points;
						var len = points.length;
						
						for(var i=0;i<len;i++){
							var name = points[i].series.name;
							var x = points[i].x;
							var y = points[i].y;
							var d = new Date(x);
							var t = "";
							if(name.indexOf('(')!=-1){
								
								var n1 = name.split(")");
								var n2 = n1[0].split("(");
								var n = n2[n2.length-1];
							
								n = $.trim(n);
								if(n=="year"){
									d = d.addYears(-1)
								}else if(n=="month"){
									d = d.addMonths(-1)
								}else if(n=="day"){
										
									d = d.addDays(-1)
								}else if(n=="week"){
										
									d = d.addDays(-7)
								}else if(n=="hour"){
									d = d.addHours(-1)
								}else if(n=="minute"){
									d = d.addMinutes(-1)
								}else if(n.indexOf('ms')!=-1){
									
									var temp_n =parseInt(n.replace('ms',''),10);
									var temp_d = d.getTime();
									d = new Date(temp_d-temp_n);
								}
								var n3 = name.split(")")
								t = "<strong>"+n3[0]+"<strong/><br>";
								name = n3[n3.length-1]
							}
							name = '<span style="color:'+points[i].series.color+';font-weight:;">'+name+'</span>';
							if(d.toStdDateTimeString().indexOf("NaN")==-1){
								var x =d.toStdDateTimeString();
							}else{
								var x =x;
							}
							
							
							if(arr.length==0){
								x ="<strong>"+x+"</strong><br>";
							}else {
								if(!no&&t){
									x = "<strong>"+x+"</strong><br>";
								}else{
									x = "";
								}
							}
							if(!no&&t){
								no=1
								t = "<br>"
							}else if(no){
								t=""
							}	
							
							arr.push(t+x+" "+name+" "+y)
						}
						
						return arr.join("<br>");
					}
				},
				legend: {
					layout: 'horizontal',
					align: 'center',
					verticalAlign: 'bottom',
					y: 12,
					floating: false,
					borderWidth: 0
				},
				colors: [
				   '#4572A7', 
				   '#AA4643', 
				   '#89A54E', 
				   '#80699B', 
				   '#3D96AE', 
				   '#DB843D', 
				   '#92A8CD', 
				   '#A47D7C', 
				   '#B5CA92'
				],
				plotOptions: {
					series:{
						cursor:'pointer',
						connectNulls:true,
						events:{
							click:function(e){
								
								var point = e.point;
								var name = point.series.name;
								var x = point.x;
								var y = point.y;
								var d = new Date(x);
								if(name.indexOf('(')!=-1){
									
									var n1 = name.split(")");
									var n2 = n1[0].split("(");
									var n = n2[n2.length-1];
									n = $.trim(n);
									if(n=="year"){
										d = d.addYears(-1)
									}else if(n=="month"){
										d = d.addMonths(-1)
									}else if(n=="day"){
											
										d = d.addDays(-1)
									}else if(n=="week"){
											
										d = d.addDays(-7)
									}else if(n=="hour"){
										d = d.addHours(-1)
									}else if(n=="minute"){
										d = d.addMinutes(-1)
									}else if(n.indexOf('ms')!=-1){
										
										var temp_n =parseInt(n.replace('ms',''),10);
										var temp_d = d.getTime();
										d = new Date(temp_d-temp_n);
									}
								}
								//var x =d.toStdDateTimeString();
								var d0 = self.gchart.options.series[0].data[0][0];
								var d1 = self.gchart.options.series[0].data[1][0];
								var z = x-(d1-d0)
								var data = {
									name:name,
									x:d.toStdDateTimeString(),
									y:y,
									startTime:(new Date(z)).toStdDateTimeString(),
									endTime:d.toStdDateTimeString(),
								};
								self.gchart.options.cclick(data)
							}
						}
					}
					
				}, 
				series: []
			}
			this.opts = {};
		},
		setSource:function(data){
			
			CData.log("line setSource")
			this.setSourceData = data;
			var data = ct.formatData(data);
			$.extend(true,this.defaults.xAxis.categories,data.categories)
			$.extend(true,this.defaults.series,data.series)
		},
		render:function(id,opt){
			CData.log("line render")
			
			if(this._destroy)return;
			var self = this;
			
			this.opt = opt;
			this.opt["id"] = id;
			
			
			if(!$('#'+id).length)return;
			
			if(self.gchart){
				self.gchart.destroy()
			}
			$('#'+id).html("");
			
			var defaults = ct.mix(this.defaults);
			var opts = $.extend(true,defaults,opt||{})
			
			var type = opts.xAxis.type;
			if(type=="datetime"){
				var categories = opts.xAxis.categories.clone();
				delete opts.xAxis.categories;
				delete opts.xAxis.tickInterval;
				opts.series = this._series(opts,categories);
			}
			if(opts.isLengend)ct.legend(opts);
			
			this._setWH(opts,id);
			self.gchart = $("#"+id).highcharts(opts).highcharts();
			self.gchart.setSourceData = self.setSourceData;
			if(this.opt.cRenderEnd)this.opt.cRenderEnd(self.gchart);
		},
		refresh:function(data){
			CData.log('line refresh')
			var self = this;
			self.setSource(data);
			self.render(self.opt.id,self.opt);
		},
		addPoint:function(data){
			CData.log("line addPoint")
			var self = this;
			CData.log('line addPoint')
			var type = self.opt.xAxis.type;
			if(type=="datetime"){
				var data = ct.formatData(data);
				var dseries = data.series;
				var dcategories = data.categories;
				
				var dlen = dseries.length;
			
				var series = self.gchart.series;
				var len = series.length;
				for(var i=0;i<len;i++){
					var serie = series[i];
					var name = serie.name;
					for(var j=0;j<dlen;j++){
						var dserie = dseries[j];
						if(dserie.name==name){
							serie.addPoint([dcategories[0].toDateTime().getTime(),dserie.data[0]],false,true);
						}
					}
				};
				
				self.gchart.redraw();
				if(self.opt.cRenderEnd)this.opt.cRenderEnd(self.gchart);
			}else{
			
			}
			
			
		},
		_setWH:function(opts,id){
			var w = opts.width;
			var h = opts.height;
			if(w)$("#"+id).css({'width':w})
			if(h)$("#"+id).css({'height':h})
		},
		_series:function(opts,categories){
			var series = opts.series;
			var _series = [];
			for(var j=0;j<series.length;j++){
				var item = series[j];
				var data = item.data;
				var name = item.name;
				var _data = [];
				
				for(var i=0;i<categories.length;i++){
					var categorie = categories[i];
					
					categorie = categorie.toDateTime().getTime();
					var list = data[i]
					_data.push([categorie,list])
				}
				_series.push({
					name:name,
					data:_data
				})
				
			}
			//对值进行过滤
			_series =  this._calcLimit(_series);
		
			return _series;
		},
		_calcLimit:function(series){
			
			var seriesClone = (series||[]).clone();
			
			var n=seriesClone.length;
			
			var visibleNo = this.opt.visibleNo||10;
			
			if (n<visibleNo){
				return seriesClone;
			}
			var _dataStatistics = {
				count:[],
				avg:[],
				max:[],
				avgLimit:Infinity,
				maxLimit:Infinity
			}
			
			for (var i=0;i<n;i++){
				_dataStatistics.count[i]=0;
				_dataStatistics.avg[i]=0;
				_dataStatistics.max[i]=-Infinity;
				var data=seriesClone[i].data;
				for (var j=0,m=data.length;j<m;j++){
					
					if (data[j][1]!==null){
						_dataStatistics.count[i]++;
						_dataStatistics.avg[i]+=data[j][1];
						if (data[j][1]>_dataStatistics.max[i]){
							_dataStatistics.max[i]=data[j][1];
						}
					}
				}
				_dataStatistics.avg[i]/=_dataStatistics.count[i];
			}
			_dataStatistics._avg=_dataStatistics.avg.clone();
			_dataStatistics._avg.sort(this._sortNumber);
			_dataStatistics._max=_dataStatistics.max.clone();
			_dataStatistics._max.sort(this._sortNumber);
			_dataStatistics.avgLimit=_dataStatistics._avg[n-visibleNo];
			_dataStatistics.maxLimit=_dataStatistics._max[n-visibleNo];
			var ret = [];
			for (var i=0;i<n;i++){
				if(_dataStatistics.avg[i]>=_dataStatistics.avgLimit ||_dataStatistics.max[i]>=_dataStatistics.maxLimit){
					seriesClone[i].visible = true;
					ret.unshift(seriesClone[i]);
				}else{
					seriesClone[i].visible = false;
					ret.push(seriesClone[i]);
				}
			}
			return ret;
		},
		_sortNumber:function(a,b){
			return a==b?1:(a>b?1:-1);
		},
		destroy:function(){
			CData.log("line.destroy")
			this._destroy = true;
			
		}
	}

	return chart;
});

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
					plotBackgroundColor: null,
					plotBorderWidth: null,
					plotShadow: false
				},
				title: {
					text: 'Browser market shares at a specific website, 2010'
				},
				tooltip: {
					pointFormat: '{series.name}: <b>{point.percentage}%</b>',
					percentageDecimals: 1
				},
				plotOptions: {
					pie: {
						allowPointSelect: true,
						cursor: 'pointer',
						dataLabels: {
							enabled: true,
							color: '#000000',
							connectorColor: '#000000',
							formatter: function() {
								return '<b>'+ this.point.name +'</b>: '+ this.percentage.toFixed(2) +' %';
							}
						}
					}
				},
				series: []
			}
			
		},
		setSource:function(data){
			var data = ct.formatData(data);
			var categories = data.categories;
			var series = data.series;
			
			for(var i=0;i<series.length;i++){
				
				var serie = series[i];
				
				var serie_data = serie.data;
				var serie_name = serie.name;
				var obj = {
					type: 'pie',
					name: serie_name,
					data: []
				}
				for(var j=0;j<serie_data.length;j++){
					var categorie = categories[j];
					var item = serie_data[j];
					obj.data.push([categorie,item])
				}
				this.defaults.series.push(obj);
			}
			
		},
		render:function(id,opt){
			var series = [];
			for(var j=0;j<this.defaults.series.length;j++){
				var item = this.defaults.series[j];
				series.push(item)
			}
			this.defaults.series = []
			var len = series.length;
			var $elm = $('#'+id);
			var width = $elm.css('width')
			var height = $elm.css('height')
			this._setWH(opt,id);
			for(var j=0;j<len;j++){
				var $container = $('<div style="position:relative;"></div>')
				$container.css({
					"width":width,
					"height":height
				})
				$container.appendTo($elm);
				if(j==0)$container.show();
				else $container.hide();
				
				this.defaults.series = [series[j]];
				var opts = $.extend(true,this.defaults,opt||{})
				
				$container.highcharts(opts);
				
				
			}
		},
		_setWH:function(opts,id){
			var w = opts.width;
			var h = opts.height;
			if(w)$("#"+id).css({'width':w})
			if(h)$("#"+id).css({'height':h})
		}
	}
	return chart;
});
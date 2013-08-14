define( ['jquery','highcharts','highchartsmore','highchartsexporting'], function() {
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
			//downloadEXCLE:'导出EXCLE文件',
			downloadCSV:'导出CSV文件',
			downloadJPEG:'导出JPEG图片',
			downloadPDF:'导出PDF图片',
			downloadPNG:'导出PNG图片',
			downloadSVG:'导出SVG图片',
			exportButtonTitle:'导出',
			printButtonTitle:'打印'
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
	function chart(){};
	chart.prototype = {
		formatData:CData.formatData,
		mix:function(obj){
			var defaults ={
				credits:{
					enabled:false
				},
				tooltip:{
					crosshairs:true
				}
			}
			var ret = $.extend(true,{},defaults,obj||{});
			return ret;
		},
		legend:function(opts){
			if(opts.series.length>10){
				$.extend(opts.legend,{
					align: 'right',
					verticalAlign: 'top',
					layout:'vertical',
					y:30
				})
			}
		}
	}
	return (new chart());
});

define( ['jquery','highcharts','highchartsmore','highchartsexporting'], function() {
	Highcharts.setOptions({
		global: {
			useUTC:false
		},
		lang:{
			//months:['һ��','����','����','����','����','����','����','����','����','ʮ��','ʮһ��','ʮ����'],
			//shortMonths:['һ��','����','����','����','����','����','����','����','����','ʮ��','ʮһ��','ʮ����'],
			months:['01','02','03','04','05','06','07','08','09','10','11','12'],
			shortMonths:['01','02','03','04','05','06','07','08','09','10','11','12'],
			weekdays:['����','��һ', '�ܶ�','����','����','����','����'],
			loading:'���ڼ������ݣ����Ժ򡭡�',
			//downloadEXCLE:'����EXCLE�ļ�',
			downloadCSV:'����CSV�ļ�',
			downloadJPEG:'����JPEGͼƬ',
			downloadPDF:'����PDFͼƬ',
			downloadPNG:'����PNGͼƬ',
			downloadSVG:'����SVGͼƬ',
			exportButtonTitle:'����',
			printButtonTitle:'��ӡ'
		},
		xAxis:{
			dateTimeLabelFormats:{
				second:'%H:%M:%S',
				minute:'%H:%M',
				hour:'%H:%M',
				day:'%b/%e',
				week:'%b/%e',
				month:'%y��%b��',
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

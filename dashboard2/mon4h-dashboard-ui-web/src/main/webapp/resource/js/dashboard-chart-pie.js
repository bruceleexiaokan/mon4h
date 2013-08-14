/**
 * User: huang_jie
 * Date: 7/26/13
 * Time: 12:19 PM
 */
!function ($) {
    $.fn.DashboardPieChart = function (options) {
        new DashboardPieChart(this, options);
    };
    var DashboardPieChart = function (element, options) {
        this.el = $(element);
        this.options = options;
        $.extend(this, this.options);
        this._init();
    }
    DashboardPieChart.prototype = {
        id: "dashboard-data",
        data: null,
        metricName: "",
        namespace: "",
        _init: function () {
            var that = this;
            var datapoints = that.data['results'][0]['data-lists'];
            var categories = [];
            var reportData = new Array();
            for (var i = 0; i < datapoints.length; i++) {
                categories.push(datapoints[i][0]);
                reportData.push(datapoints[i][1][0]);
            }
            var chart_data = {
                "format": ["categories", categories],
                "results": [
                    {
                        "data-lists": [
                            ['data', reportData]
                        ]
                    }
                ]
            };
            var gData1 = CData().setData(chart_data).pie({
                id: that.id,
                params: {
                    title: {
                        text: that.namespace + " - " + that.metricName
                    },
                    tooltip: {
                        formatter: function () {
                            return '<strong>'+this.point.name+'</strong><br/><span style="color:'+this.point.color+';font-weight:bold;">'+this.y+' ('+this.percentage.toFixed(2)+'%)'+'</span>';
                        }
                    }
                }
            });
        }
    }
}(window.jQuery);
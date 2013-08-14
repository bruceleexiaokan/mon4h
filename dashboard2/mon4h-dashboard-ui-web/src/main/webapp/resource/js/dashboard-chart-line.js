/**
 * User: huang_jie
 * Date: 7/26/13
 * Time: 12:19 PM
 */
!function ($) {
    $.fn.DashboardLineChart = function (options) {
        new DashboardLineChart(this, options);
    };
    var DashboardLineChart = function (element, options) {
        this.el = $(element);
        this.options = options;
        $.extend(this, this.options);
        this._init();
    }
    DashboardLineChart.prototype = {
        id: "dashboard-data",
        data: null,
        metricName: "",
        namespace: "",
        _init: function () {
            $("#dashboard-data").empty();
            $("#dashboard-data").append('<div id="rending" style="display: none"></div>');
            $("#rending").loading({mask: false,text:"正在渲染静态线图..."}).show();
            var that = this;
            var gData1 = CData().setData(that.data).line({
                id: that.id,
                params: {
                    visibleNo: 9,
                    xAxis: {
                        type: "datetime"
                    },
                    isLengend: true,
                    title: {
                        text: that.namespace + " - " + that.metricName
                    },
                    legend: {
                        align: 'right',
                        verticalAlign: 'top',
                        layout: 'vertical',
                        y: 30
                    },
                    tooltip: {
                        shared: false,
                        formatter: function () {
                            return '<strong>' + toDateTimeString(new Date(this.x)) + '</strong><br />' + this.point.series.name + ' <span style="color:' + this.point.series.color + ';font-weight:bold;">' + this.y + '</span>';
                        }
                    }, cRenderEnd: function (chart) {
                        $("#rending").css("display", "none");
                        that.finish();
                    }, cclick: function () {

                    }
                }
            });
        },
        finish: function () {
        }
    }
}(window.jQuery);
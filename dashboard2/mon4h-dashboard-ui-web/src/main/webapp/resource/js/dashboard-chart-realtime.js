/**
 * User: huang_jie
 * Date: 7/26/13
 * Time: 12:19 PM
 */
!function ($) {
    $.fn.DashboardRealTimeChart = function (options) {
        new DashboardRealTimeChart(this, options);
    };
    var DashboardRealTimeChart = function (element, options) {
        this.el = $(element);
        this.options = options;
        $.extend(this, this.options);
        this._init();
    }
    DashboardRealTimeChart.prototype = {
        id: "dashboard-data",
        data: null,
        metricName: "",
        namespace: "",
        interval: 99999999,
        lastDate: new Date(),
        _init: function () {
            var that = this;
            this.chart = CData().setData(that.data).line({
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
                    }, cclick: function () {

                    }
                }
            });
            var queryParam = new $.QueryString((window.location.hash));
            that.interval = getInterval(queryParam.get('interval', null));
            that.lastDate = $.hash('end-time');
            var lastTime = moment(this.lastDate, 'YYYY-MM-DD HH:mm:ss').toDate();
            that.lastDate = toDateTimeString(new Date(lastTime.getTime() - that.interval));
            if (that.interval != null) {
                dashboardTimer = $.timer(function () {
                    that._reload();
                });
                dashboardTimer.set({ time: that.interval, autostart: true });
            }
        },
        _reload: function () {
            var queryParam = new $.QueryString((window.location.hash));
            queryParam.set("max-datapoint-count", 1);
            var lastTime = moment(this.lastDate, 'YYYY-MM-DD HH:mm:ss').toDate();
            this.lastDate = toDateTimeString(new Date(lastTime.getTime() + this.interval));
            queryParam.set('start-time',this.lastDate );
            queryParam.set('end-time', toDateTimeString(new Date(lastTime.getTime() + this.interval +6000000)));
            var that = this;
            new DashboardRequest().send(queryParam.toStr(), function (data) {
                if (data == null) {
                    return;
                }
                var shows = that.chart.shows;
                var len = shows.length;
                for (var i = 0; i < len; i++) {
                    var show = shows[i];
                    if (show.addPoint) {
                        show.addPoint(data);
                    } else if (show.refresh) {
                        show.refresh(data);
                    }
                }
            });

        }
    }
}(window.jQuery);
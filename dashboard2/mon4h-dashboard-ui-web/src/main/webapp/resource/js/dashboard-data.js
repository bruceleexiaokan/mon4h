/**
 * Load dashboard data
 * User: huang_jie
 * Date: 7/19/13
 * Time: 1:48 PM
 */
var DashboardData = function (el) {
    this.el = $(el);
}
DashboardData.prototype = {
    load: function () {
        if (dashboardTimer != null) {
            dashboardTimer.stop();
        }
        var that = this;
        var queryParam = new $.QueryString((window.location.hash));
        if (!that._validation(queryParam)) {
            return;
        }
        var chart = queryParam.get("chart", null);
        if (chart != null && chart == 'pie') {
            queryParam.set("max-datapoint-count", 1);
        }
        for (var key in queryParam.p) {
            queryParam.set(key, encodeURIComponent(queryParam.get(key, '')))
        }
        that._showLoading();
        new DashboardRequest().send(queryParam.toStr(),that._render);
    },
    _render:function(data){
        if (data == null) {
            hideLoading();
            return;
        }
        var queryParam = new $.QueryString((window.location.hash));
        var chart = queryParam.get("chart", null);
        var namespace = queryParam.get('namespace', 'Dashboard');
        var metricName = queryParam.get('metric-name', null);
        if (chart == 'pie') {
            $("#dashboard-data").DashboardPieChart({
                data: data,
                metricName: metricName,
                namespace: namespace
            });
        } else if (chart == 'bar') {
            $("#dashboard-data").DashboardBarChart({
                data: data,
                metricName: metricName,
                namespace: namespace
            });
        } else if (chart == 'realtime') {
            console.log('realtime');
            $("#dashboard-data").DashboardRealTimeChart({
                data: data,
                metricName: metricName,
                namespace: namespace
            });
        } else {
            $("#dashboard-data").DashboardLineChart({
                data: data,
                metricName: metricName,
                namespace: namespace
            });
        }
        hideLoading();
    },
    _showLoading: function () {
        this.el.empty();
        this.el.append('<div id="loading" style="display: none"></div>');
        $("#loading").loading({mask: false}).show();
    },
    _validation: function (params) {
        var metricName = params.get('metric-name', null);
        if (metricName == null) {
            alert('请输入相应的Metric Name!');
            $("#metric-name").focus();
            return false;
        }
        var startTime = params.get('start-time', null);
        if (startTime == null) {
            alert('请输入相应的开始时间!');
            $("#start-time").focus();
            return false;
        }
        var endTime = params.get('end-time', null);
        if (endTime == null) {
            alert('请输入相应的结束时间!');
            $("#end-time").focus();
            return false;
        }
        var start = moment(startTime, 'YYYY-MM-DD HH:mm:ss');
        var end = moment(endTime, 'YYYY-MM-DD HH:mm:ss');
        if (!start.isBefore(end)) {
            alert('开始时间不能大于结束时间!');
            $("#start-time").focus();
            return false;
        }
        var interval = params.get('interval', null);
        if (interval == null) {
            alert('请输入相应的时间间隔!');
            $("#interval").focus();
            return false;
        }
        var t = interval.substring(interval.length - 1);
        if(t.match(/^[smhdM]$/)==null) {
            alert('时间间隔单位只支持s,m,h,d,M');
            $("#interval").focus();
            return false;
        }
        if (checkInterval(interval) == null) {
            alert('时间间隔只能为整数单位!');
            $("#interval").focus();
            return false;
        }
        var int = getInterval(interval);
        if(end.toDate()<(new Date(start.toDate().getTime()+int))){
            alert('开始时间+时间间隔不能大于结束时间!');
            $("#interval").focus();
            return false;
        }
        return true;
    }
}


function hideLoading () {
    $("#loading").loading({mask: false}).hide();
}


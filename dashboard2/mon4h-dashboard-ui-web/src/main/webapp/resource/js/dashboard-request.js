/**
 * Dashboard UI Send Request to Server
 * User: huang_jie
 * Date: 7/26/13
 * Time: 4:10 PM
 */
var DashboardRequest = function () {
    this.el = $("#dashboard-data");
}

DashboardRequest.prototype.send = function (param, render) {
    var that = this;
    var url = config.getBaseUrl() + "/data?" + param;
    $.ajax({
        url: url
    }).done(function (data) {
            var obj = null;
            if(typeof data === 'object') {
                obj = data;
            }else{
                obj = JSON.parse( data);
            }
            if (data == '' || data == null) {
                that._showErr();
                if (render && typeof(render) === "function") {
                    render(null);
                }
                return null;
            }
            if (obj['result-code'] != 0) {
                that._showErr(obj['result-info']);
                if (render && typeof(render) === "function") {
                    render(null);
                }
                return null;
            }
            var lastDataPointTs;
            var groupListLen = obj['time-series-group-list'].length;
            if (obj['result-code'] == 0 && obj['time-series-group-list'] != null && obj['time-series-group-list'] != '' && groupListLen > 0) {
                $("#no-found").css("display", "none");
                var baseTime = obj['time-series-group-list'][0]['data-points']['base-time'].toDateTime();
                lastDataPointTs = obj['time-series-group-list'][0]['data-points']['last_datapoint_ts'];
                var interval = obj['time-series-group-list'][0]['data-points']['interval'];
                var categories = [];
                for (var i = 0, n = obj['time-series-group-list'][0]['data-points']['data-points'].length; i < n; i++) {
                    var t = addTime(baseTime, interval, i + 1);
                    categories.push(t);
                }
                var reportData = new Array();
                for (var i = 0; i < groupListLen; i++) {
                    var row = new Array();
                    var group = obj['time-series-group-list'][i]['time-series-group'];
                    var groupTag = new Array();
                    for (var key in group) {
                        groupTag.push(key + "(" + group[key] + ")");
                    }
                    row.push(groupTag.toString());
                    row.push(obj['time-series-group-list'][i]['data-points']['data-points']);
                    reportData.push(row);
                }
                var result = {
                    "format": ["time", categories],
                    "last_datapoint_ts": lastDataPointTs,
                    "results": [
                        {
                            "data-lists": reportData
                        }
                    ]
                };
                if (render && typeof(render) === "function") {
                    render(result);
                }
            } else {
                that._showErr();
                if (render && typeof(render) === "function") {
                    render(null);
                }
                return null;
            }
        });
}

DashboardRequest.prototype._showErr = function (msg) {
    this.el.empty();
    this.el.append('<div class="alert alert-error" id="no-found">查询不到对应的数据，请调整相应的查询条件之后重试！</div>');
    if (msg != null && msg != '') {
        $("#no-found").text(msg);
    }
}
function DashboardUI() {
    this.defualtNamespace = "ns-null";
}

DashboardUI.prototype.init = function () {
    this.initUI();
    this.initValue();
    this.bindEvent();
    if ($.hash('env') == null) {
        $.hash('env', 'TEST');
    }
}

DashboardUI.prototype.initUI = function () {
    var that = this;
    var now = new Date(), previousHour = new Date();

    $("#middle").find("div[data-key]").each(function (index) {
        var dropdown = new DashboardDropdown($(this).attr("data-key"));
        dropdown.init();
    });

    previousHour.setHours(previousHour.getHours() - 1);
    var fromDate = new DashboardDate("start-time");
    fromDate.init(previousHour);

    var toDate = new DashboardDate("end-time");
    toDate.init(now);

    $("#namespace").autocomplete({
        url: "/meta/namespaces",
        key: 'namespaces'
    });

    this.tags = new DashboardTag($("#tag-filter"));

    $("#metric-name").autocomplete({
        key: 'metricNames',
        queryUrl: function () {
            var ns = $("#namespace").val() == '' ? that.defualtNamespace : $("#namespace").val();
            return "/meta/namespaces/" + ns + "/metrics";
        },
        after: function () {
            $("#dashboard-data").empty();
            $("#tag-group").val('');
            $("#max-datapoint-count").val('100');
            $.hash('tags', null);
            that.tags.load();
        }
    });

    $("#tag-group").autocomplete({
        key: 'tagNames',
        autoHash: false,
        hasParent: true,
        multiple: true,
        queryUrl: function () {
            var ns = $("#namespace").val() == '' ? that.defualtNamespace : $("#namespace").val();
            return "/meta/namespaces/" + ns + "/metrics/" + $("#metric-name").val() + "/tagNames";
        }
    });


    this.dashboardData = new DashboardData($("#dashboard-data"));
}

DashboardUI.prototype.initValue = function () {
    var queryParam = new $.QueryString((window.location.hash));
    $("#interval").val(queryParam.get("interval", '1m'));
    var groupBy = queryParam.get("group-by", '');
    if (groupBy != '') {
        $("#tag-group").val(groupBy.substr(1, groupBy.length - 2));
    }
    $("#max-datapoint-count").val(queryParam.get("max-datapoint-count", '100'));
}

DashboardUI.prototype.bindEvent = function () {
    var that = this;
    $("#searchBtn").bind('click', function (e) {
        if (dashboardTimer != null) {
            dashboardTimer.stop();
        }
        var queryParam = new $.QueryString((window.location.hash));
        queryParam.set('namespace', $("#namespace").val());
        queryParam.set('metric-name', $("#metric-name").val());
        var chart = queryParam.get("chart", null);
        queryParam.set('start-time', $("#start-time").val());
        queryParam.set('end-time', $("#end-time").val());
        queryParam.set('interval', $("#interval").val());
        queryParam.set('max-datapoint-count', $("#max-datapoint-count").val());
        if (chart != null && chart == 'realtime') {
            var now = new Date();
            now.setMinutes(now.getMinutes() - 3);
            var interval = getInterval(queryParam.get('interval', '1m'));
            var maxCount = queryParam.get('max-datapoint-count', 60);
            queryParam.set('start-time', toDateTimeString(new Date(now.getTime()-interval*maxCount)));
            queryParam.set('end-time', toDateTimeString(now));
            $("#max-datapoint-count").val(maxCount);
            $("#start-time").val(toDateTimeString(new Date(now.getTime()-interval*maxCount)));
            $("#end-time").val(toDateTimeString(now));
        }
        queryParam.set('tags', that.tags.getQueryParam());
        var groupBy = $("#tag-group").val();
        queryParam.set("group-by", '[' + groupBy + ']');
        $("#middle").find("div[data-key]").each(function (index) {
            var key = $(this).attr("data-key");
            var k = $(this).find("span .label,.label-info").text();
            var value = $(this).find('a:contains("' + k + '")').attr("data-val");
            queryParam.set(key, value);
        });
        window.location.hash = queryParam.toStr();
        that.dashboardData.load();
    });
    $("#resetAllBtn").bind('click', function (e) {
        if (dashboardTimer != null) {
            dashboardTimer.stop();
        }
        window.location.hash = 'env=TEST&chart=line&aggregator=sum&downsampler=sum';
        that.initValue();
        $("#namespace").val('');
        $("#metric-name").val('');
        $("#tag-group").val('');
        $('#tag-filter').empty();
        var now = new Date();
        var previousHour = new Date();
        previousHour.setHours(now.getHours() - 1);
        $("#start-time").val(dateFormat(previousHour));
        $("#end-time").val(dateFormat(now));
    });
    $("#resetBtn").bind('click', function (e) {
        if (dashboardTimer != null) {
            dashboardTimer.stop();
        }
        var queryParam = new $.QueryString((window.location.hash));
        var env = queryParam.get('env', 'TEST');
        window.location.hash = 'env=' + env + '&chart=line&aggregator=sum&downsampler=sum';
        that.initValue();
        $("#namespace").val('');
        $("#metric-name").val('');

        $("#tag-group").val('');
        $('#tag-filter').empty();
        var now = new Date();
        var previousHour = new Date();
        previousHour.setHours(now.getHours() - 1);
        $("#start-time").val(dateFormat(previousHour));
        $("#end-time").val(dateFormat(now));
    });
    $(window).on('hashchange', function () {
        var queryParam = new $.QueryString((window.location.hash));
        for (var key in queryParam.p) {
            var label = $("#" + key).find("span .label,.label-info");
            label.text($("#" + key).find("a[data-val='" + queryParam.get(key, null) + "']").text());
        }
    });
}

/**
 * Dashboard Tag Component
 * User: huang_jie
 * Date: 7/22/13
 * Time: 4:17 PM
 */
var DashboardTag = function (el) {
    this.el = $(el);
    this._init();
};
DashboardTag.prototype._init = function () {
    this.load();
}
DashboardTag.prototype.load = function () {
    var that = this;
    var queryParam = new $.QueryString(window.location.hash);
    var namespace = queryParam.get('namespace', 'ns-null');
    var metricName = queryParam.get('metric-name', null);
    if (metricName == null || metricName == '') {
        return;
    }
    this.el.empty();
    var url = config.getBaseUrl() + "/meta/namespaces/" + encodeURIComponent(namespace) + "/metrics/" + encodeURIComponent(metricName) + "/tagNames";
    $.ajax({
        url: url
    }).done(function (data) {
            if (data == '' || data == null) {
                return;
            }
            var obj = null;
            if(typeof data === 'object') {
                obj = data;
            }else{
                obj = JSON.parse( data);
            }
            if (obj['result-code'] == 0 && obj['tagNames'] != null && obj['tagNames'] != '' && obj['tagNames'].length > 0) {
                $.each(obj['tagNames'], function (index, value) {
                    var val = value;
                    if (value.length > 7) {
                        val = value.substr(0, 7) + "...";
                    }
                    var template = '<div class="btn-group text-left tag-row"><button id="__%%VALUE%%__-name" class="btn" title="__%%VALUE%%__">' + val + '</button><input id="__%%VALUE%%__" type="text"></div>';
                    that.el.append(template.replace(new RegExp("__%%VALUE%%__","g"),value));
                    $('#' + escapeStr(value) + '-name').tooltip({'placement': 'top'});
                    $("#" +escapeStr(value)).autocomplete({
                        key: 'tagValues',
                        autoHash: false,
                        hasParent: true,
                        multiple: true,
                        auto:true,
                        queryUrl: function () {
                            return "/meta/namespaces/" + namespace + "/metrics/" + metricName + "/tagNames/" + encodeURIComponent(value) + "/tagValues";
                        }
                    });
                });
            }

            that._initValue(queryParam.get("tags", null));
        });
}

DashboardTag.prototype._initValue = function (init) {
    if (init != null) {
        var tagObj = JSON.parse(init);
        for (var key in tagObj) {
            var tagValue = tagObj[key];
            if (tagValue != null && tagValue != '') {
                $("#" + key).val(tagValue.toString());
            }
        }
    }
}


DashboardTag.prototype.getQueryParam = function () {
    var tag = new Array();
    var groups = new $.QueryString();
    var inputTag = new $.QueryString();
    var groupBy = $("#tag-group").val();
    if (groupBy != null && groupBy != '') {
        var groupBys = groupBy.split(",");
        for (var i = 0; i < groupBys.length; i++) {
            groups.set(groupBys[i].trim(), '');
        }
    }

    this.el.find('input[type=text]').each(function (index) {
        var th = $(this);
        var value = th.val();
        if (value == '' || value == null) {
            return;
        }
        if (value != null && value.trim() == '*') {
            value = '';
        }
        var key = th.attr('id');
        inputTag.set(key, '');
        var values = value.split(',');
        tag.push('"' + key + '":' + JSON.stringify(values));
    })
    for (var key in groups.p) {
        if (!inputTag.has(key)) {
            tag.push('"' + key + '":[]');
        }
    }
    return '{' + tag.toString() + '}';
};

function DashboardDropdown(id) {
    this.id = id;
}
DashboardDropdown.prototype.init = function () {
    var queryParam = new $.QueryString((window.location.hash));
    var value = queryParam.get(this.id, null);
    if (value != null) {
        var label = $("#" + this.id).find("span .label,.label-info");
        label.text($("#" + this.id).find("a[data-val='" + value + "']").text());
    }

    $("#" + this.id).bind("click", function (e) {
        var target = e.target;
        if (target.tagName == "A") {
            var val = $(target).attr("data-val");
            var key = this.getAttribute("data-key");
            var params = new $.QueryString((window.location.hash));
            params.set(key, val);
            window.location.hash = params.toStr();
        }
    });
}
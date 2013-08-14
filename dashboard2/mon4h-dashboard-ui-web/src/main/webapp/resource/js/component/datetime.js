function DashboardDate(id) {
    this.id = id;
    this.el = $("#" + this.id);
}
DashboardDate.prototype.init = function (date) {
    var queryParam = new $.QueryString((window.location.hash));
    var value = queryParam.get(this.id, null);
    var initValue = '';
    if (value != null) {
        initValue = decodeURIComponent(value);
    } else {
        initValue = dateFormat(date);
    }
    this.el.iCalendar({
        type: 'datetime',
        placeholder: initValue
    });

}


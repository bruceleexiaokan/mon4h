/**
 * Dashboard Common UI Config
 * User: huang_jie
 * Date: 7/22/13
 * Time: 12:48 PM
 */
var DashboardConfig = function () {
    this.config = {
        "DEV": "http://127.0.0.1:8080/dashboard-io",
        "TEST": "http://127.0.0.1:8080/dashboard-io",
        "UAT": "http://192.168.82.16:8080",
        "PROD": "prod.url"
    }
}

DashboardConfig.prototype.getBaseUrl = function () {
    var queryParam = new $.QueryString(decodeURIComponent(window.location.hash));
    var env = queryParam.get('env', null);
    if (env == null) {
        alert("Wrong environment selected. Please select valid environment!");
        return;
    }
    return this.config[env];
}

var config = new DashboardConfig();
var dashboardTimer;

$.QueryString = function (qs) {
    this.p = {};

    if (!qs) qs = location.search;

    if (qs) {
        var reg = /(?:[^a-zA-Z0-9]*)(.*?)=(.*?)(?=&|$)/g, temp;
        while ((temp = reg.exec(qs)) != null) {
            this.p[temp[1]] = temp[2];
        }
    }

    this.set = function (name, value) {
        this.p[name] = value;
        return this;
    };

    this.get = function (name, def) {
        var v = this.p[name];
        return (v != null) ? v : def;
    };

    this.has = function (name) {
        return this.p[name] != null;
    };

    this.toStr = function () {
        var r = '';
        for (var k in this.p) {
            if (this.p[k]) {
                r += k + '=' + this.p[k] + '&';
            }
        }
        if (r.indexOf('&') > 0) {
            r = r.substring(0, r.length - 1);
        }
        return r;
    };
};

/**
 * Created with IntelliJ IDEA.
 * User: huang_jie
 * Date: 7/26/13
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */

var usedRe = {
    interval: /^(\d+)([smhdMy])$/,
    dateTimeString1: /\b(\d)\b/g,
    dateTimeString2: /-/g,
    dateTimeString3: /^(\d+[^\d]+(?:\d+[^\d]+\d+[^\d]+)?)([^\|]*)\|\1/,
    dateTimeString4: /:00$/,
    createStyle: /(\s*)([^\{\}]+)\{/g,
    filterType: /^([@%#])(.+)\1$/,
    validateTags: /^([@%#])(.+)\1$/
}
function escapeStr( str) {
    if( str)
        return str.replace(/([ #;?%&,.+*~\':"!^$[\]()=>|\/@])/g,'\\$1')
    else
        return str;
}
function toDateTimeString(dateTime, isMinify) {
    var t = dateTime.toStdDateTimeString();
    t = t.replace(usedRe.dateTimeString1, '0$1');
    if (isMinify) {
        var l = this._lastDateTime;
        this._lastDateTime = t;
        if (l) {
            var arr = (t + '|' + l).match(usedRe.dateTimeString3);
            if (arr) {
                t = arr[2];
            }
        }
        t = t.replace(usedRe.dateTimeString2, '/');
        t = t.replace(usedRe.dateTimeString4, '');
    }
    return t;
}
function checkInterval(interval) {
    return interval.match(this.usedRe.interval);
}
function getInterval(interval) {
    if (interval == null) {
        return null;
    }
    var arr = interval.match(this.usedRe.interval);
    if (!arr) {
        return null;
    }
    var val = arr[1].toInt();
    var unit = arr[2] || 's';
    switch (unit) {
        case 's':
            return val * 1000;
        case 'm':
            return val * 1000 * 60;
        case 'h':
            return val * 1000 * 60 * 60;
        case 'd':
            return val * 1000 * 60 * 60 * 24;
        case 'M':
            return val * 1000 * 60 * 60 * 24 * 30;
        case 'y':
            return val * 1000 * 60 * 60 * 24 * 30 * 12;
        default:
            return null;
    }
}

function addTime(baseTime, interval, times) {
    var arr = interval.match(usedRe.interval);
    if (!arr) {
        return null;
    }
    var val = arr[1].toInt();
    var unit = arr[2] || 's';
    var ret;
    switch (unit) {
        case 's':
            ret = baseTime.addSeconds(val * times);
            break;
        case 'm':
            ret = baseTime.addMinutes(val * times);
            break;
        case 'h':
            ret = baseTime.addHours(val * times);
            break;
        case 'd':
            ret = baseTime.addDays(val * times);
            break;
        case 'M':
            ret = baseTime.addMonths(val * times);
            break;
        case 'y':
            ret = baseTime.addYears(val * times);
            break;
        default:
            return null;
    }
    return toDateTimeString(new Date(ret));
}

function dateFormat(date, mask) {
    var dateObj = {
        M: date.getMonth() + 1,
        d: date.getDate(),
        h: date.getHours(),
        m: date.getMinutes(),
        s: date.getSeconds()
    };

    mask = (mask || 'yyyy-MM-dd hh:mm:ss').replace(/(M+|d+|h+|m+|s+)/g, function (v) {
        return ((v.length > 1 ? "0" : "") + eval('dateObj.' + v.slice(-1))).slice(-2);
    });
    return mask.replace(/(y+)/g, function (v) {
        return date.getFullYear().toString().slice(-v.length);
    });
}
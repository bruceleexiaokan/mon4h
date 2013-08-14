/**
 * Dashboard auto complete for namespace,metric name, tag name and value
 * User: huang_jie
 * Date: 7/17/13
 * Time: 12:19 PM
 */
!function ($) {
    $.fn.autocomplete = function (options) {
        new autocomplete(this, options);
    };
    var autocomplete = function (element, options) {
        this.el = $(element);
        this.options = options;
        this.id = this.el.attr('id');
        this.oldValue = '';
        $.extend(this, this.options);
        this._init();
        this._bindEvent();
    }
    autocomplete.prototype = {
        url: null,
        limit: 20,
        template: null,
        key: null,
        autoHash: true,
        auto:false,
        hasParent: false,
        multiple: false,
        queryUrl: function () {

        },
        after: function () {

        },
        _setValue: function (value) {
            if (this.multiple) {
                var index = this.oldValue.indexOf(value);
                if (this.oldValue != '' && index < 0) {
                    this.oldValue += ",";
                }
                if (this.oldValue.indexOf(value) < 0) {
                    this.el.val(this.oldValue + value);
                    this.oldValue = this.el.val();
                }
            } else {
                this.el.val(value);
            }
        },
        _init: function () {
            var queryParam = new $.QueryString((window.location.hash));
            var value = queryParam.get(this.id, null);
            if (value != null && this.el.attr("type") == 'text') {
                this.el.val(value);
            }
            this.offset = this.el.offset();
            this.width = this.el.css("width");
            this.template = '<ul id="' + this.id + '-tips" class="dropdown-menu text-left" style="display: none;"></ul>';
            this.el.after(this.template);
            this.tips = $("#" + escapeStr(this.id) + "-tips");
        },
        _showTips: function () {
            if (this.hasParent) {
                this.tips.css("left", this.el.offset().left - this.el.parent().offset().left);
                this.tips.css("top", this.el.offset().top - this.el.parent().offset().top + 30);
            } else {
                this.tips.css("left", this.el.offset().left);
                this.tips.css("top", this.el.offset().top + 30);
            }
            this.tips.css("width", this.width);
            this.tips.css("display", "block");
        },
        _hideTips: function () {
            if (this.autoHash) {
                var queryParam = new $.QueryString((window.location.hash));
                queryParam.set(this.id, (this.el.val()));
                window.location.hash = queryParam.toStr();
            }
            if (this.tips.css('display') == 'block') {
                this.tips.css("display", "none");
                this.tips.find("li").removeAttr("class");
                this.after();
            }
        },
        _bindEvent: function () {
            var that = this;
            this.el.bind("click",function(e){
                if(that.auto) {
                    that._load();
                }
            });
            this.tips.bind("click", function (e) {
                var target = e.target;
                if (target.tagName == "A") {
                    var val = target.getAttribute("data-val");
                    that._setValue(val);
                }
                that._hideTips();
            });
            this.tips.bind("mouseover", function (e) {
                that.el.unbind('blur');
                that.tips.find("li").removeAttr("class");
            });

            this.tips.bind("mouseout", function (e) {
                that.el.bind('blur', function (e) {
                    that._hideTips();
                });
            });

            this.el.bind('blur', function (e) {
                that._hideTips();
            });

            this.el.bind('keyup', function (e) {
                if (e.keyCode == 13) {
                    var active = that.tips.find("li").hasClass("active");
                    if (active) {
                        var val = that.tips.find("li.active a").attr("data-val");
                        that._setValue(val);
                    }
                    that._hideTips();
                    return false;
                } else if (e.keyCode == 38) {
                    that._nextTips();
                } else if (e.keyCode == 40) {
                    that._proviousTips();
                } else {
                    that._load();
                }
            });
        },
        _load:function(){
            var that = this;
            if (!that.auto && ( that.el.val() == null || that.el.val() == '')) {
                that.tips.empty();
                that._hideTips();
                return;
            }

            var index = that.el.val().lastIndexOf(",");
            if(index>0) {
                that.oldValue = that.el.val().substring(0, index);
            }else{
                that.oldValue = '';
            }

            if (that.queryUrl() != null && that.queryUrl() != '') {
                that.url = that.queryUrl();
            }
            if (that.url.substring(that.url.length - 1) != '/') {
                that.url = that.url + "/";
            }
            var query = that.el.val();
            if (that.multiple) {
                var index = query.lastIndexOf(",");
                if (index > 0) {
                    query = query.substring(index + 1);
                }
            }
            if (!that.auto && ( query == '' || query == '.' || query == '..')) {
                return;
            }
            query = encodeURIComponent(query);
            $.ajax({
                url: config.getBaseUrl() + that.url + query,
                data: {limit: that.limit}
            }).done(function (data) {
                    if (data == '' || data == null) {
                        that.tips.empty();
                        that._hideTips();
                        return;
                    }
                    var obj = null;
                    if(typeof data === 'object') {
                        obj = data;
                    }else{
                        obj = JSON.parse( data);
                    }
                    that.tips.empty();
                    if (obj['result-code'] == 0 && obj[that.key] != null && obj[that.key] != '' && obj[that.key].length > 0) {
                        $.each(obj[that.key], function (index, value) {
                            that.tips.append('<li><a href="javascript:void(0);" data-val="' + value + '">' + value + '</a></li>');
                        });
                        that._showTips();
                    } else {
                        that._hideTips();
                    }
                });
        }   ,
        _proviousTips: function () {
            var active = this.tips.find("li").hasClass("active");
            if (!active) {
                this.tips.find("li").first().addClass("active");
            } else {
                var cur = this.tips.find("li.active");
                $(cur).removeClass("active");
                $(cur).next().addClass("active");
                if ($(cur).is(':last-child')) {
                    this.tips.find("li").first().addClass("active");
                }
            }
        },
        _nextTips: function () {
            var active = this.tips.find("li").hasClass("active");
            if (!active) {
                this.tips.find("li").last().addClass("active");
            } else {
                var cur = this.tips.find("li.active");
                $(cur).removeClass("active");
                $(cur).prev().addClass("active");
                if ($(cur).is(':first-child')) {
                    this.tips.find("li").last().addClass("active");
                }
            }
        }
    }
}(window.jQuery);
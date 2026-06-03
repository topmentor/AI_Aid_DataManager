/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function goTarget(target, name1, val1, name2, val2, name3, val3, name4, val4, name5, val5, name6, val6){
    location.href =  getTarget(target, name1, val1, name2, val2, name3, val3, name4, val4 , name5, val5, name6, val6);
}


function getTarget(target, name1, val1, name2, val2, name3, val3, name4, val4, name5, val5, name6, val6){
    var url = target;
    if(name1!=null){
        url += "?";
        url += name1 + "=" + val1;
    }
    if(name2!=null){
        url += "&";
        url += name2 + "=" + val2;
    }
    if(name3!=null){
        url += "&";
        url += name3 + "=" + val3;
    }
    if(name4!=null){
        url += "&";
        url += name4 + "=" + val4;
    }
    if(name5!=null){
        url += "&";
        url += name5 + "=" + val5;
    }
    if(name6!=null){
        url += "&";
        url += name6 + "=" + val6;
    }
    return url;
}

function fn_del_commas(nStr){
    var resultStr = nStr.replace(/\,/g,'');
    return resultStr;
}

function fn_add_commas(nStr)
{
    nStr += '';
    x = nStr.split('.');
    x1 = x[0];
    x2 = x.length > 1 ? '.' + x[1] : '';
    var rgx = /(\d+)(\d{3})/;
    while (rgx.test(x1)) {
        x1 = x1.replace(rgx, '$1' + ',' + '$2');
    }
    return x1 + x2;
}


function checkValidation(data,checkString)
{
    for(var i=0;i<data.length;i++){
        if(checkString.indexOf(data.charAt(i)) >= 0) {
            return false;
        }
    }
    return true;
}

function getDialog(url, title, prefix){
    var data = '<div style="font-size:9pt;overflow:hidden;" id="'+ prefix +'-iframe" title="'+title+'">'
    +'<input type="hidden" id="'+prefix+'-return">'
    +'<iframe src="' + url + '" height="100%" width="100%" marginWidth="0" marginHeight="0" frameBorder="0" scrolling="auto">'
    +'</iframe></div>';
    return data;
}
function createDialog(comp, url, prefix, reload, left, top, width, height, border, background){
    
    $(document).click(function(e){ //메시지 div가 아닌곳을 클릭할때 hide한다.
        

        var flag = false;
        $(comp).children().each(function(){//자식을 클릭했는지
            if(this == e.srcElement)
                flag = true;
        });
        if(comp == e.srcElement){
            flag = true; //자기자신을 클릭했는지
        }
        var iframeDiv = prefix +"-iframe";
        var $myDiv = $("#" + iframeDiv);
        
        if($myDiv.size()==1 && reload){
            $myDiv.remove();
            $myDiv = null;
        }
        
        if(!$myDiv || $myDiv.size()==0){
            $myDiv = $(document.createElement("div"));
            $myDiv.css("border", border);
            $myDiv.css("background-color", background);

            $myDiv.attr("id", iframeDiv);
            $myDiv.attr("z-index", 1000);
            document.body.appendChild($myDiv[0]);
            $myDiv.css("position", "absolute");                        
            $myDiv.css("overflow", "hidden");                        
            $myDiv.css("width", width + "px");
            $myDiv.css("height", height + "px");    
            $myDiv.css("left", left + "px");
            $myDiv.css("top", top + "px");
            var data = '<iframe src="' + url + '" height="100%" width="100%"  frameBorder="0" scrolling="auto"></iframe>'; 
            $myDiv.html(data);
        }        
        if($myDiv){
            if(flag || reload){
                $myDiv.show();
            }else{
                $myDiv.hide();
            }
        }
        e.stopPropagation();
    });
}


/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
    var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
    timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
    timezoneClip = /[^-+\dA-Z]/g,
    pad = function (val, len) {
        val = String(val);
        len = len || 2;
        while (val.length < len) val = "0" + val;
        return val;
    };

    // Regexes and supporting functions are cached through closure
    return function (date, mask, utc) {
        var dF = dateFormat;

        // You can't provide utc if you skip other args (use the "UTC:" mask prefix)
        if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
            mask = date;
            date = undefined;
        }

        // Passing date through Date applies Date.parse, if necessary
        date = date ? new Date(date) : new Date;
        if (isNaN(date)) throw SyntaxError("invalid date");

        mask = String(dF.masks[mask] || mask || dF.masks["default"]);

        // Allow setting the utc argument via the mask
        if (mask.slice(0, 4) == "UTC:") {
            mask = mask.slice(4);
            utc = true;
        }

        var	_ = utc ? "getUTC" : "get",
        d = date[_ + "Date"](),
        D = date[_ + "Day"](),
        m = date[_ + "Month"](),
        y = date[_ + "FullYear"](),
        H = date[_ + "Hours"](),
        M = date[_ + "Minutes"](),
        s = date[_ + "Seconds"](),
        L = date[_ + "Milliseconds"](),
        o = utc ? 0 : date.getTimezoneOffset(),
        flags = {
            d:    d,
            dd:   pad(d),
            ddd:  dF.i18n.dayNames[D],
            dddd: dF.i18n.dayNames[D + 7],
            m:    m + 1,
            mm:   pad(m + 1),
            mmm:  dF.i18n.monthNames[m],
            mmmm: dF.i18n.monthNames[m + 12],
            yy:   String(y).slice(2),
            yyyy: y,
            h:    H % 12 || 12,
            hh:   pad(H % 12 || 12),
            H:    H,
            HH:   pad(H),
            M:    M,
            MM:   pad(M),
            s:    s,
            ss:   pad(s),
            l:    pad(L, 3),
            L:    pad(L > 99 ? Math.round(L / 10) : L),
            t:    H < 12 ? "a"  : "p",
            tt:   H < 12 ? "am" : "pm",
            T:    H < 12 ? "A"  : "P",
            TT:   H < 12 ? "AM" : "PM",
            Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
            o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
            S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
        };

        return mask.replace(token, function ($0) {
            return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
        });
    };
}();

// Some common format strings
dateFormat.masks = {
    "default":      "ddd mmm dd yyyy HH:MM:ss",
    shortDate:      "m/d/yy",
    mediumDate:     "mmm d, yyyy",
    longDate:       "mmmm d, yyyy",
    fullDate:       "dddd, mmmm d, yyyy",
    shortTime:      "h:MM TT",
    mediumTime:     "h:MM:ss TT",
    longTime:       "h:MM:ss TT Z",
    isoDate:        "yyyy-mm-dd",
    isoTime:        "HH:MM:ss",
    isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
    isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
    dayNames: [
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    ],
    monthNames: [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
    ]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
    return dateFormat(this, mask, utc);
};
Date.prototype.diffDays= function(d1, d2){
    var t2 = d2.getTime();
    var t1 = d1.getTime();

    return parseInt((t2-t1)/(24*3600*1000));
}
Date.prototype.addDays = function(days){
    return new Date(this.getTime() + days * 24 * 60 * 60 * 1000);
}
Date.prototype.addSeconds = function(seconds){
    return new Date(this.getTime() + seconds * 1000);
}

/*javascript array remove가 없어서 웹에서 구해서 넣는다.*/
Array.prototype.remove = function(e) {   //@부모짱
    var t, _ref;
    if ((t = this.indexOf(e)) > -1) {
        return ([].splice.apply(this, [t, t - t + 1].concat(_ref = [])), _ref);
    }
};
function loadJavascript(src, type){
    var dynamic;
    if (type=="js"){ //if src is a external JavaScript file
        dynamic = document.createElement('script');
        dynamic.setAttribute("type","text/javascript");
        dynamic.setAttribute("src", src);
    }
    else if (type=="css"){ //if src is an external CSS file
        dynamic = document.createElement("link");
        dynamic.setAttribute("rel", "stylesheet");
        dynamic.setAttribute("type", "text/css");
        dynamic.setAttribute("href", src);
    }
    if (typeof dynamic!="undefined")
        document.getElementsByTagName("head")[0].appendChild(dynamic);
}

function getOptions(width, height, align, valign, option) 
{
    var x,y;
    var window_option = "dialogWidth="+width+"px;dialogHeight="+height +"px";

    if (option!=null) window_option+=";"+option;
    if (align==null) align="center";
    if (valign==null) valign="center";

    if (align=="left") x=0;
    else if (align=="right") x=(screen.width-width);
    else if (align=="center") x=(screen.width-width)/2

    if (valign=="top") y=0;
    else if (valign=="bottom") y=(screen.height-height);
    else if (valign=="center") y=(screen.height-height)/2

    window_option+=";dialogLeft="+x+";dialogTop="+y;


    return window_option;
}


function getMaxFromArray(ar) {
    var max = ar[0];
    var len = ar.length;
    for (var i = 1; i < len; i++) 
        if (ar[i] > max)
            max = ar[i];
    return max;
};

function getMinFromArray(ar) {
    var min = ar[0];
    var len = ar.length;
    for (var i = 1; i < len; i++) 
        if (ar[i] < min) 
            min = ar[i];
    return min;
};
(function($) {
    $.containsArray = function (ar, obj){
        var i = ar.length; 
        while (i--) { 
            if (ar[i] === obj) { 
                return true; 
            } 
        }
        return false; 
    };
})(jQuery);

//Array.prototype.contains = function(obj) { 
//    var i = this.length; 
//    while (i--) { 
//        if (this[i] === obj) { 
//            return true; 
//        } 
//    } 
//    return false; 
//}; 
/*여기서만 사용하는 함수*/
//function setDesignText(id, event, text){
//    if(event=="focus"){
//        if($(id).val()==text){
//            $(id).val("").css("color", "black");
//        }
//    }else if(event=="blur" || event=="default"){
//        if($(id).val()==""){
//            $(id).val(text).css("color", "#DDD");
//        }
//    }
//}
//
///*텍스트 필드의 디폴트 텍스트를 지정하는 함수*/
//function setDesignText_WorkUtil(id, text){
//    setDesignText(id, "default", text);
//    $(id).focus(function(){
//        setDesignText(id, "focus", text);
//    });
//    $(id).blur(function(){
//        setDesignText(id, "blur", text);
//    });        
//}

function clearGarbage(data){
    // IE일 경우 제거
    data = data.replace("<HEAD>","");
    data = data.replace("</HEAD>","");
    data = data.replace("<BODY>","");
    data = data.replace("</BODY>","");
    // 파이어폭스일 경우 제거
    data = data.replace("<head>","");
    data = data.replace("</head>","");
    data = data.replace("<body>","");
    data = data.replace("</body>","");
    data = data.replace(/^\s+|\s+$/g,""); // 공백제거
    return data;
}
    
function stripHTMLtag(string) { 
    var objStrip = new RegExp(); 
    objStrip = /[<][^>]*[>]/gi; 
    return string.replace(objStrip, ""); 
}     

function checkMobilePhone(){ /**/
    var useragent = navigator.userAgent;
    useragent = useragent.toLowerCase();
    if (useragent.indexOf('iphone') != -1 || useragent.indexOf('symbianos') != -1 || useragent.indexOf('ipad') != -1 || useragent.indexOf('ipod') != -1 || useragent.indexOf('android') != -1 || useragent.indexOf('blackberry') != -1 || useragent.indexOf('samsung') != -1 || useragent.indexOf('nokia') != -1 || useragent.indexOf('windows ce') != -1 || useragent.indexOf('sonyericsson') != -1 || useragent.indexOf('webos') != -1 || useragent.indexOf('wap') != -1 || useragent.indexOf('motor') != -1 || useragent.indexOf('symbian') != -1 ) {
        return true;
    } else {
        return false;
    }
}

function fn_showChoiceMenu(urlmap, element){             
    var chref = location.href.toString(); 
    for (var k in urlmap){
        if(chref.indexOf(k)!=-1){
            var targetId = urlmap[k]
            $(targetId).css("font-weight", "bold").css("color", "white").css("font-size", "13px");
            $(targetId + " " + element).css("border-top", "solid 2px #F00");
            break;
        }
    }
};/*function*/

function fn_setMenuElement(menuSelector, contentsSelector){
    _g_menuFlag = false;/*전역변수로 설정해서 계속 남게한다.*/
    $(contentsSelector).menu();
    $(contentsSelector).mouseover(function(){
        _g_menuFlag = true;
    });
    $(contentsSelector).mouseleave(function(){
        if(_g_menuFlag){
            $(this).hide();
            _g_menuFlag = false;
        }
    });
    $(menuSelector).click(function(){
        var _this = this;
        $(contentsSelector).each(function(){
            if(_this.id.indexOf(this.id)!=-1){/*선택된 메뉴는 보이게 한다.clientMenuTile은 clientMenu라는 단어를 포함한다.*/
                var leftPosition = $(_this).attr("leftPosition");
                $(this).css("left", ($(_this).offset().left + parseInt(leftPosition))+"px");
                $(this).show();
            }else{
                $(this).hide();
            }
        });
    });    
};/*function*/
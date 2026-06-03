/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

function sfn_setMenuElement(menuSelector, contentsSelector){
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
                $(this).css("top", ($(_this).offset().top+35) + "px");
                $(this).show();
            }else{
                $(this).hide();
            }
        });
    });    
};/*function*/

function sfn_showChoiceMenu(urlmap, element){             
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

function sfn_checkMobile(){ /**/
    var useragent = navigator.userAgent;
    useragent = useragent.toLowerCase();
    if (useragent.indexOf('iphone') != -1 || useragent.indexOf('symbianos') != -1 || useragent.indexOf('ipad') != -1 || useragent.indexOf('ipod') != -1 || useragent.indexOf('android') != -1 || useragent.indexOf('blackberry') != -1 || useragent.indexOf('samsung') != -1 || useragent.indexOf('nokia') != -1 || useragent.indexOf('windows ce') != -1 || useragent.indexOf('sonyericsson') != -1 || useragent.indexOf('webos') != -1 || useragent.indexOf('wap') != -1 || useragent.indexOf('motor') != -1 || useragent.indexOf('symbian') != -1 ) {
        return true;
    } else {
        return false;
    }
}

//column명에 따라 index를 구해주는 함수, 현재 사용하는 workmanager.$GRID와 colName을 사용
function fmm_getColumnIndex(grid$, columnName) {
    var cm =grid$.jqGrid('getGridParam', 'colModel');
    var i, l = cm.length;
    for (i = 0; i < l; i++){
        if (cm[i].name === columnName) {
            return i; // return the index
        }
    }
    return -1;
}
function goTarget(target, name1, val1, name2, val2, name3, val3, name4, val4, name5, val5, name6, val6){
    location.href =  getTarget(target, name1, val1, name2, val2, name3, val3, name4, val4 , name5, val5, name6, val6);
}
function goTarget(target, name1, val1){
    location.href =  getTarget(target, name1, val1, null , null, null, null, null, null, null, null, null,null );
}
function goOpen(target, name1, val1, name2, val2, name3, val3, name4, val4, name5, val5, name6, val6){
    window.open(getTarget(target, name1, val1, name2, val2, name3, val3, name4, val4 , name5, val5, name6, val6));
}

function sfn_ajax(targetDiv, url, params){
    $.ajax({
        type : "post",
        url : url,
        dataType : "html",
        async : true,
        cache: false,
        data : params,
        success : function(data){
            $(targetDiv).html(data);
        }
    });//ajax            
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

function sfn_getDialog(url, title, prefix){
    var data = '<div style="font-size:9pt;overflow:hidden;" id="'+ prefix +'-iframe" title="'+title+'">'
    +'<input type="hidden" id="'+prefix+'-return">'
    +'<iframe src="' + url + '" height="100%" width="100%" margin:0px; padding:0px;  frameBorder="0" scrolling="auto">'
    +'</iframe></div>';
    return data;
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


function sfn_clearGarbage(data){
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

function Toast(params, className){        
    var _this = this;
    this.toast = document.createElement("div");
    this.toast.className = "ithows-toast";
    this.hideToast = function (second, success){        
        if(!second){
            second=1;
        }
        setTimeout(function(){
            _this.toast.style.opacity = 0;
            document.body.removeChild(_this.toast);
            if(success)
                success();
        }, second);
        return this;
    }
    this.showToast = function (message, second, success){


        _this.toast.innerHTML = message;
        if(className){
            _this.toast.className = className;
        }
        //디폴트값 적용
        _this.toast.style.opacity = .9;
        _this.toast.style.position = "fixed";
        _this.toast.style.background = "#555";
        _this.toast.style.textAlign = "center";
        _this.toast.style.padding = "20px";

        _this.toast.style.fontSize = "12px";
        _this.toast.style.color = "white";
        _this.toast.style.border = "2px solid #454545";
        _this.toast.style.borderRadius = "3px";
        _this.toast.style.left = "50%";
        _this.toast.style.top = "50%";
        _this.toast.style.width = "200px";
        _this.toast.style.height= "20px";
        _this.toast.style.zIndex = 1000;
        if(params){
            for(var key in params){
                _this.toast.style[key] = params[key];
            }
        }
        _this.toast.style.marginTop = (- ((parseInt(_this.toast.style.height))/2  + 20)) + "px";
        _this.toast.style.marginLeft = (-((parseInt(_this.toast.style.width))/2  + 20)) + "px";
        window.document.body.appendChild(_this.toast);
        if(second){
            _this.hideToast(second, success);
        }     

        return this;
    }
}

utilManager = {
    /**
     * 입력값이 사용자가 정의한 포맷 형식인지 체크
     * 자세한 format 형식은 자바스크립트의 ''regular expression''을 참조
     */
    isValidFormat : function (str,format) {
        if (str.search(format) != -1) {
            return true; //올바른 포맷 형식
        }
        return false;
    },

    /**
     * 입력값이 이메일 형식인지 체크
     * ex) if (!isValidEmail(form.email)) {
     * alert("올바른 이메일 주소가 아닙니다.");
     * }
     */
    isValidEmail : function (input) {
        // var format = /^(S+)@(S+).([A-Za-z]+)$/;
        //var format = /^((w|[-.])+)@((w|[-.])+).([A-Za-z]+)$/;
        var format = /^([a-zA-Z0-9_\.\-])+\@(([a-zA-Z0-9\-])+\.)+([a-zA-Z0-9]{2,4})+$/;
        return this.isValidFormat(input,format);
    },

    /**
     * 입력값이 전화번호 형식(숫자-숫자-숫자)인지 체크
     */
    isValidPhone : function (input) {
        var format =  /(01[016789])[-](\d{4}|\d{3})[-]\d{4}$/g;  
        return this.isValidFormat(input,format);
    },
    isNumberOrDash: function (input) {
        var format =  /^(\d+-?)+\d+$/;  
        return this.isValidFormat(input,format);
    },
    
    isFloat : function (input) {
        var format = /^\d*(\.\d+)?$/;  
        return this.isValidFormat(input,format);
    },
    isTimeValue : function (input) {
        var format = /(0|1|2|3|4)\d[:](0|1|2|3|4|5)\d/;  
        return this.isValidFormat(input,format);
    },
    isNumber: function (input) {
        var format =  /^\d+$/;  
        return this.isValidFormat(input,format);
    },
    replaceAll : function(str, findStr, replaceStr){
        if(str == null || str == undefined){
            return "";
        }
        return str.split(findStr).join(replaceStr);
    },
    getNumbers : function(str){
        if(str){
            var regex = /[^0-9]/g;
            return str.replace(regex, '');
        }else{
            return str;
        }
    },
    makeSsnDashNumber : function(ssn){
        if(!ssn){
            return ssn;
        }
        if(!this.isNumberOrDash(ssn)){ //숫자와 대시가 아니라면
            return ssn;
        }   
        var tmp = this.getNumbers(ssn);
        if(tmp.length==13){
            return tmp.substring(0,6) +"-"+ tmp.substring(6,13);
        }else{
            return ssn;
        }
    },
   
    makePhoneDashNumber : function(cellPhone){
        if(!cellPhone){
            return cellPhone;
        }
        if(!this.isNumberOrDash(cellPhone)){ //숫자와 대시가 아니라면
            return cellPhone;
        }
        var tmp = this.getNumbers(cellPhone);
        if(tmp.length==10){
            return tmp.substring(0,3) +"-"+ tmp.substring(3,6) +"-"+ tmp.substring(6,10);
        }else if(tmp.length==11){
            return tmp.substring(0,3) +"-"+ tmp.substring(3,7) +"-"+ tmp.substring(7,11);
        }else{
            return cellPhone;
        }
    },
    isValidSsn : function(ssn){
        if(ssn.indexOf("-1")!=-1 || ssn.indexOf('-2')!=-1  || ssn.indexOf('-3')!=-1  || ssn.indexOf('-4')!=-1){                
            return this.isValidKorSsn(this.getNumbers(ssn));
        }else if(ssn.indexOf("-5")!=-1 || ssn.indexOf('-6')!=-1){
            return this.isValidEngSsn(this.getNumbers(ssn));
        }        
    },
    isValidKorSsn : function (ssnNo){        
        var A = ssnNo.charAt(0);
        var B   = ssnNo.charAt(1);
        var C   = ssnNo.charAt(2);
        var D   = ssnNo.charAt(3);
        var E   = ssnNo.charAt(4);
        var F   = ssnNo.charAt(5);
        var G   = ssnNo.charAt(6);
        var H   = ssnNo.charAt(7);
        var I   = ssnNo.charAt(8);
        var J   = ssnNo.charAt(9);
        var K   = ssnNo.charAt(10);
        var L   = ssnNo.charAt(11);
        var Osub  = ssnNo.charAt(12);

        var SUMM = A*2 + B*3 + C*4 + D*5+ E*6+ F*7 + G*8 + H*9 + I*2 + J*3 + K*4 + L*5;
        var N = SUMM % 11;
        var Modvalue = 11 - N;
        var LapointVal =  Modvalue % 10 ;
                
        if ( Osub == LapointVal ) {
            return true;
        } else {
            return false;
        }        
    }, 
    isValidEngSsn : function(fgnno) {        
        var sum=0;
        var odd=0;
        var buf = new Array(13);
        for(i=0; i<13; i++) {
            buf[i]=parseInt(fgnno.charAt(i));
        }
        odd = buf[7]*10 + buf[8];
        if(odd%2 != 0) {
            return false;
        }
        //        if( (buf[11]!=6) && (buf[11]!=7) && (buf[11]!=8) && (buf[11]!=9) ) {
        //            return false;
        //        }
        var multipliers = [2,3,4,5,6,7,8,9,2,3,4,5];
        for(i=0, sum=0; i<12; i++) {
            sum += (buf[i] *= multipliers[i]);
        }
        sum = 11 - (sum%11);
        if(sum >= 10) {
            sum -= 10;
        }
        sum += 2;
        if(sum >= 10) {
            sum -= 10;
        }
        if(sum != buf[12]) {
            return false
        }
        return true;
    }
}


/*숫자만 받기*/
function onlyNumber(){
    var code = window.event.keyCode;
    if ((code > 34 && code < 41) || (code > 47 && code < 58) || (code > 95 && code < 106) || code == 8 || code == 9 || code == 13 || code == 46){
        window.event.returnValue = true;
        return;
    }
    window.event.returnValue = false;
}
/*숫자와 :만 받기*/
function onlyNumberColon(){
    var code = window.event.keyCode;        
    if ((code > 34 && code < 41) || (code > 47 && code < 58) || (code > 95 && code < 106) || code == 8 || code == 9 || code == 13 || code == 46 || code==186){
        window.event.returnValue = true;
        return;
    }
    window.event.returnValue = false;
}
/*숫자와 -만 받기*/
function onlyNumberDash(){
    var code = window.event.keyCode;            
    if ((code > 34 && code < 41) || (code > 47 && code < 58) || (code > 95 && code < 106) || code == 8 || code == 9 || code == 13 || code == 46 || code == 109 || code == 189){
        window.event.returnValue = true;
        return;
    }
    window.event.returnValue = false;
}
/*숫자와 ,만 받기*/
function onlyNumberComma(){
    var code = window.event.keyCode;             
    if ((code > 34 && code < 41) || (code > 47 && code < 58) || (code > 95 && code < 106) || code == 8 || code == 9 || code == 13 || code == 46 || code == 188){
        window.event.returnValue = true;
        return;
    }
    window.event.returnValue = false;
}
/*숫자와 .만 받기*/
function onlyNumberPoint(){
    var code = window.event.keyCode;                   
    if ((code > 34 && code < 41) || (code > 47 && code < 58) || (code > 95 && code < 106) || code == 8 || code == 9 || code == 13 || code == 46 || code == 110 || code == 190){
        window.event.returnValue = true;
        return;
    }
    window.event.returnValue = false;
}

function yshow(target, youtubeFrame){    
    if(target){
        var $target = $("#"+target); 
        var astr = $target.html();
        var bstr = youtubeFrame;
        var asrc = astr.substring(astr.indexOf("http://"), astr.indexOf("hd=1")-1);
        var bsrc = bstr.substring(bstr.indexOf("http://"), bstr.indexOf("hd=1")-1);     
        if( $target.html().toLowerCase().indexOf("<iframe")!=-1){
            if(asrc!=bsrc){
                $target.hide();
                $target.html(youtubeFrame);
                $target.show();
            }else{
                $target.hide();
                $target.empty();
            }
        }else if( $target.html()=="" || $target.html().toLowerCase().indexOf("<iframe")!=-1 || $target.html()!=youtubeFrame){
            $target.hide();
            $target.html(youtubeFrame);
            $target.show();
        }else{
            $target.hide();
            $target.empty();
        }
    }
}

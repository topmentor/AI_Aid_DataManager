/**
 * Util 함수들
 * Create By S.O.X 2020.04
 */


function calculateGridId(lnt, lat, level) {
    
    let orgMinX = 124.54117;
    let orgMinY = 32.928463;
    let orgMaxX = 130.57113;
    let orgMaxY = 42.344405;

    let OFFSET_5M_X = 0.0000555;
    let OFFSET_5M_Y = 0.0000460;

    let xId = Math.floor((lnt - orgMinX) / (OFFSET_5M_X * level)) + 1;
    let yId = Math.floor((lat - orgMinY) / (OFFSET_5M_Y * level)) + 1;

    let minX = orgMinX + ((xId - 1) * OFFSET_5M_X * level);
    let minY = orgMinY + ((yId - 1) * OFFSET_5M_Y * level);
    let maxX = minX + (OFFSET_5M_X * level);
    let maxY = minY + (OFFSET_5M_Y * level);
    
    let result = {};
    result.gridId = {
        "xId" : xId,
        "yId" : yId
    };
    
    result.extent = {
        "minX" : minX,
        "minY" : minY,
        "maxX" : maxX,
        "maxY" : maxY
    };
    
    return result;
    
}

function countMatchingElements(csv1, csv2) {
    // Split the CSV strings into arrays
    const array1 = csv1.split(',');
    const array2 = csv2.split(',');

    // Create a Set from the first array for efficient lookup
    const set1 = new Set(array1);

    // Count the matching elements
    let matchCount = 0;
    array2.forEach(element => {
        if (set1.has(element)) {
            matchCount++;
        }
    });

    return matchCount;
}


// 서빙셀 갯수 구하기
// const csvData = '177_2500_2_0,177_275_2_0,177_2850_2_0,177_3200_2_0,25_2500_2_0,25_275_0_7245683,25_2850_2_0,25_3200_2_0';
// console.log(countNonZeroElements(csvData)); // 결과: 1
function countServingCells(csvString) {
    const elements = csvString.split(',');

    const filteredElements = elements.filter(item => {
        const parts = item.split('_');
        return parts.length === 4 && parts[3] !== '0';
    });

    // 결과 개수 반환
    return filteredElements.length;
}

// 서빙셀 CSV 문자열을 반환
function getServingCellCSV(csvString) {
    // CSV 문자열을 ','로 분리
    const elements = csvString.split(',');

    // 필터링하여 네 번째 요소가 0이 아닌 경우를 찾기
    const filteredElements = elements.filter(item => {
        const parts = item.split('_');
        return parts.length === 4 && parts[3] !== '0';
    });

    // 필터링된 요소들을 다시 CSV 문자열로 반환
    return filteredElements.join(',');
}




function countLTECorp(csv1) {

    if(csv1==''){
        return 0;
    }

    const elements = csv1.split(',');

    // 3번째 값이 0인 경우의 갯수 계산
    const count = elements.filter(element => {
        // '_'로 분리하여 배열로 변환
        const parts = element.split('_');
        // 3번째 값이 0인지 확인
        return parts[2] === '0';
    }).length;

    return count;
}


// 텍스트를 파일로 다운로드
function downloadText(filename, text) {
    var link = document.createElement('a');
    link.setAttribute('download', filename);
    link.href = makeTextFile(text);
    document.body.appendChild(link);

    // wait for the link to be added to the document
    window.requestAnimationFrame(function () {
      var event = new MouseEvent('click');
      link.dispatchEvent(event);
      document.body.removeChild(link);
    });
}
var dnTextFile = null;
function makeTextFile(text) {
    var data = new Blob([text], {type: 'text/plain'});

    if (dnTextFile !== null) {
      window.URL.revokeObjectURL(dnTextFile);
    }

    dnTextFile = window.URL.createObjectURL(data);

    return dnTextFile;
};

// 텍스트를 파일로 다운로드 2
function downloadText2(filename, text) {
  var element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
  element.setAttribute('download', filename);

  element.style.display = 'none';
  document.body.appendChild(element);

  element.click();

  document.body.removeChild(element);
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

function stringReplaceAll(str, findStr, replaceStr){
    if(str == null || str == undefined){
        return "";
    }
    return str.split(findStr).join(replaceStr);
};

function call_html_ajax(targetDiv, url, params){
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

function call_json_ajax(targetDiv, url, params){
//    var params = {
//        time : nowTime
//    };
    
    $.ajax({
        type : "post",
        url : url,
        contentType: "application/json",            
        dataType:"json",
        data: JSON.stringify(params),
        cache: false,
        success : function(data){
            $(targetDiv).html(JSON.stringify(data));
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


/*javascript array remove가 없어서 웹에서 구해서 넣는다.*/
Array.prototype.remove = function(e) {   
    var t, _ref;
    if ((t = this.indexOf(e)) > -1) {
        return ([].splice.apply(this, [t, t - t + 1].concat(_ref = [])), _ref);
    }
};


function ArrayToSet(a) {
    var temp = {};
    for (var i = 0; i < a.length; i++)
        temp[a[i]] = true;
    var r = [];
    for (var k in temp)
        r.push(k);
    return r;
}

function populateTypeAhead(csv, delimiter) {
    var typeAheadSource = [];
    var lines = csv.split("\n");
    for (var i = lines.length - 1; i >= 1; i--) {
        var items = lines[i].split(delimiter);
        for (var j = items.length - 1; j >= 0; j--) {
            var item = items[j].strip();
            item = item.replace(/"/g, '');
            if (item.indexOf("http") !== 0 && isNaN(parseFloat(item))) {
                typeAheadSource.push(item);
                var words = item.split(/\W+/);
                for (var k = words.length - 1; k >= 0; k--) {
                    typeAheadSource.push(words[k]);
                }
            }
        }
    }
    return typeAheadSource;
}



function fnEventPrevent(event) {
    if (!window.event) {
        return false;
    } else {
        window.event.returnValue = false; //IE
    }
}


/////////////////////////////////////////////////////
//  메세지 박스 관련

function fnMessageBox(tit, time) {
    swal({
        title: tit,
        type: "info",
        confirmButtonText: "확인",
        showCancelButton: false,
        animation: false
    });

    fnEventPrevent();

    if (time != null) {
        setTimeout(function () {
            swal.close();
        }, time);
    }
}

function fnInfoMessage(tit, time) {

    swal({
        title: tit,
        type: "info",
        showCancelButton: false,
        showConfirmButton: false,
        animation: false
    });

    if (time != null) {
        setTimeout(function () {
            swal.close();
        }, time);
    }
}


function fnSuccessMessage(tit, time) {

    swal({
        title: tit,
        type: "success",
        showCancelButton: false,
        showConfirmButton: false,
        animation: false
    });

    if (time != null) {
        setTimeout(function () {
            swal.close();
        }, time);
    }
}

function fnConfirmMessage(tit, callback) {

    swal({
        title: tit,
        type: "question",
        showCancelButton: true,
        showConfirmButton: true,
        confirmButtonColor: "#FF0000",
        confirmButtonText: 'OK',
        cancelButtonText: "Cancle",
        animation: false
    }).then((result) => {
        if (result) {
            if (callback != null) {
                callback.call();
            }
        }
    }, function (dismiss) {
        if (dismiss === 'cancel') {
        }
    });

}

function fnAlert(tit, time) {
    swal({
        title: tit,
        type: "error",
        showCancelButton: false,
        showConfirmButton: false,
        animation: false
    });

    if (time != null) {
        setTimeout(function () {
            swal.close();
        }, time);
    }
}

function fnGetFormData(myForm, key) {
    for (es = myForm.entries(); !(e = es.next()).done && (pair = e.value); ) {

        if (pair[0] == key) {
            return pair[1];
        }
    }
    return null;
}




/////////////////////////////////////////////////////
//  날짜 관련

function getDateNow() {
    var n = Date.now();
    var d = new Date(n);
    return d;
}

function getTimeNow(){
    var n = Date.now();
    var d = new Date(n);

    var year = d.getFullYear();
    var month = d.getMonth()+1
    var day = d.getDate();
    if(month < 10){
        month = "0"+month;
    }
    if(day < 10){
        day = "0"+day;
    }

    var hours = d.getHours();
    if(hours < 10){
        hours = "0"+hours;
    }
    var minutes = d.getMinutes();
    if(minutes < 10){
        minutes = "0"+minutes;
    }
    var sec = d.getSeconds();
    if(sec < 10){
        sec = "0"+sec;
    }

    var strd = year+'-'+ month+'-'+day+' '+ hours + ':' + minutes + ':' + sec;
    return strd;
}

function getTime4File(){
    var n = Date.now();
    var d = new Date(n);

    var year = d.getFullYear();
    var month = d.getMonth()+1
    var day = d.getDate();
    if(month < 10){
        month = "0"+month;
    }
    if(day < 10){
        day = "0"+day;
    }

    var hours = d.getHours();
    if(hours < 10){
        hours = "0"+hours;
    }
    var minutes = d.getMinutes();
    if(minutes < 10){
        minutes = "0"+minutes;
    }
    var sec = d.getSeconds();
    if(sec < 10){
        sec = "0"+sec;
    }

    var strd = year+''+ month+''+day+'_'+ hours + '' + minutes + '' + sec;
    return strd;
}


function getTimeYesterday(n) {
    var d = new Date(n);

    var year = d.getFullYear();
    var month = d.getMonth();     // 텍스트로 쏠 때는 1을 더해야 하고, Date로 다룰 때에는 그대로 써야 한다
    var day = d.getDate() - 1;
    if (month < 10) {
        month = "0" + month;
    }
    if (day < 10) {
        day = "0" + day;
    }

    if (day == 0 && (month == 0 || month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10)) {
        day = 31;
        month = month - 1;
    } else if (day == 0 && (month == 4 || month == 6 || month == 9 || month == 11)) {
        day = 30;
        month = month - 1;
    } else if (day == 0 && month == 2) {
        day = 28;
        month = month - 1;
    }

    var hours = d.getHours();
    if (hours < 10) {
        hours = "0" + hours;
    }
    var minutes = d.getMinutes();
    if (minutes < 10) {
        minutes = "0" + minutes;
    }
    var sec = d.getSeconds();
    if (sec < 10) {
        sec = "0" + sec;
    }

    var strd = year + '/' + month + '/' + day;
    return new Date(year, month, day);
}

function convertDateFormat(n) {
    var d = new Date(n);

    var year = d.getFullYear();
    var month = d.getMonth() + 1;
    var day = d.getDate();
    if (month < 10) {
        month = "0" + month;
    }
    if (day < 10) {
        day = "0" + day;
    }

    var hours = d.getHours();
    if (hours < 10) {
        hours = "0" + hours;
    }
    var minutes = d.getMinutes();
    if (minutes < 10) {
        minutes = "0" + minutes;
    }
    var sec = d.getSeconds();
    if (sec < 10) {
        sec = "0" + sec;
    }

    var strd = year + '/' + month + '/' + day;
    return strd;
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

var soxDateFormat = function () {
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
soxDateFormat.masks = {
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
soxDateFormat.i18n = {
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
    return soxDateFormat(this, mask, utc);
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

/////////////////////////////////////////////////////////////////
//

function jsonBeautify(json) {
    return JSON.stringify(json ,null,2);
}

function jsonBeautifyHtml(json) {
    if (typeof json != 'string') {
         json = JSON.stringify(json, undefined, 2);
    }
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function (match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}


//////////////////////////////////////////////////////////
// 파일 다운로드 관련

// 파일 다운로드 ajax 
function sox_downloadFile(fid) {

    $.ajax({
        url: "/MakeRandom/downloadFile.do",
        type: "get",
        data : {
            "fid" : fid
        },            
        xhrFields: {
            responseType: 'blob' // to avoid binary data being mangled on charset conversion
        },
        success: function(blob, status, xhr) {
            // check for a filename
            var filename = "";
            var disposition = xhr.getResponseHeader('Content-Disposition');
            if (disposition && disposition.indexOf('attachment') !== -1) {
                var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                var matches = filenameRegex.exec(disposition);
                if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
            }

            if (typeof window.navigator.msSaveBlob !== 'undefined') {
                // IE workaround for "HTML7007: One or more blob URLs were revoked by closing the blob for which they were created. These URLs will no longer resolve as the data backing the URL has been freed."
                window.navigator.msSaveBlob(blob, filename);
            } else {
                var URL = window.URL || window.webkitURL;
                var downloadUrl = URL.createObjectURL(blob);


                if (filename) {
                    // use HTML5 a[download] attribute to specify filename
                    var a = document.createElement("a");
                    // safari doesn't support this yet
                    if (typeof a.download === 'undefined') {
                        window.location.href = downloadUrl;
                    } else {
                        a.href = downloadUrl;
                        a.download = filename;
                        document.body.appendChild(a);
                        a.click();
                    }
                } else {
                    window.location.href = downloadUrl;
                }

                setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100); // cleanup
            }
        },
        error: function(xmlhttprequest, textstatus, message) {
           console.log(xmlhttprequest, textstatus, message);
        }
    });

}


function fileDownload(id){
    $.ajax({
           type : "post",
           url : "/MakeRandom/downloadFile.do",
           dataType:"json",
           cache: false,
           data : {
               "fId" : id
           },
           success : function(data){   
               if(data['result']=="OK"){

                   let a = document.createElement('a');
                   var url = data['msg'];
                   console.log(window.location.origin + "/" +  url);

                   a.href = window.location.origin + "/" + url;
                   a.download = url.split('/').pop();
                   document.body.appendChild(a);
                   a.click();
                   document.body.removeChild(a);


               }else{
                   fnAlert(data['msg'], 2000);
               }
           } 

       });  

}


function goView(id){
   $.ajax({
           type : "post",
           url : "/MakeRandom/dataDownload.do",
           dataType:"json",
           cache: false,
           data : {
               "fId" : id
           },
           success : function(data){   
               if(data['result']=="OK"){

                   let a = document.createElement('a');
                   var url = data['msg'];
                   console.log(window.location.origin + "/" +  url);

                   a.href = window.location.origin + "/" + url;
                   a.target = "_blank";
                   document.body.appendChild(a);
                   a.click();
                   document.body.removeChild(a);


               }else{
                   fnAlert(data['msg'], 2000);
               }
           } 

       }); 
}


function sox_loadList() {
    $.ajax({
        url: "${servletPath}/service/mainList.do",
        type: "post",
        dataType: "html",
        data : {
        },
        success: function(data) {
            $("#sox_archiveList").html(data); // 결과 뿌리기

        },
        error: function(a, b, c) {
            console.log(a, b, c);
        }
    });

}

// mac 주소 변환
function convertMacString(mac){
        
    if(mac.includes(":")){
        return mac.toLowerCase();
    }

    if(mac.includes("-")){
        mac = stringReplaceAll(mac,"-", ":");
        return mac.toLowerCase();
    }

    var str = "";

    for ( i=0; i < mac.length ; i++) {
        str += mac[i];
        if(i < (mac.length - 1) && i%2 == 1){
            str += ":";
        }
    }

    return str.toLowerCase();
}
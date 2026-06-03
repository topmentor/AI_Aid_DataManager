// goTarget 함수 - 페이지 이동
function goTarget(url, paramName, paramValue) {
    if (paramName && paramValue) {
        var form = $('<form></form>');
        form.attr('method', 'POST');
        form.attr('action', url);
        
        var input = $('<input>');
        input.attr('type', 'hidden');
        input.attr('name', paramName);
        input.attr('value', paramValue);
        
        form.append(input);
        $('body').append(form);
        form.submit();
    } else {
        window.location.href = url;
    }
}


function doLogin(){

    var timeStr = getTimeNow();
    console.log(timeStr);

    var passwd = $('#passwordInput').val();

    $.ajax({
        type : "post",
        url : servletPath + "/login.do",
        dataType:"json",
        cache: false,
        data : {
            timeNow : timeStr,
            passwd : passwd
        },
        success : function(data){
            if(data['result']=="OK"){
                console.log(data);
                goTarget(servletPath +"/main.do","userId", data['msg']);
            }else{
                CustomAlert.error(data['msg'], 1000);
            }
        }

    });
}



$(document).ready(function() {
    // 페이지 로드 시 ID 입력란에 포커스
    $('#passwordInput').focus();

    // Enter 키로 로그인
    $('#passwordInput').keyup(function(event) {
        if (event.keyCode == 13) {
            doLogin();
        }
    });


    // 로그인 버튼 클릭
    $('#loginBtn').on('click', function() {
        doLogin();
    });

    // 회원가입 버튼 클릭 - 팝업 열기
    $('#signinBtn').on('click', function() {
        CustomAlert.error("사용 불가", 1000);
    });

});

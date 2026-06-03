/**

 * Custom Alert Module
 * 프로젝트 전역에서 사용 가능한 커스텀 알림 모듈
 * 
 * 사용법:
 * CustomAlert.error('에러 메시지', 2000);
 * CustomAlert.info('정보 메시지');
 * CustomAlert.success('성공 메시지', 2000);
 */

var CustomAlert = (function() {
    'use strict';
    
    var isInitialized = false;
    var isConfirmInitialized = false;
    var $alertPopup, $alertOverlay, $alertMessage, $alertIcon, $alertConfirmBtn;
    var $confirmPopup, $confirmOverlay, $confirmMessage, $confirmIcon, $confirmOkBtn, $confirmCancelBtn;
    var confirmCallback = null;
    
    // HTML 생성 및 DOM에 추가
    function initialize() {
        if (isInitialized) return;
        
        var alertHTML = 
            '<div class="custom-alert-popup hidden" id="customAlertPopup">' +
            '  <div class="custom-alert-popup_content">' +
            '    <div class="custom-alert-popup_icon" id="customAlertIcon"></div>' +
            '    <div class="custom-alert-popup_message" id="customAlertMessage"></div>' +
            '    <div class="custom-alert-popup_buttonGroup">' +
            '      <button class="custom-alert-popup_confirmBtn" id="customAlertConfirmBtn">\uD655\uC778</button>' +
            '    </div>' +
            '  </div>' +
            '</div>' +
            '<div class="custom-alert-overlay hidden" id="customAlertOverlay"></div>';
        
        $('body').append(alertHTML);
        
        // DOM 요소 캐싱
        $alertPopup = $('#customAlertPopup');
        $alertOverlay = $('#customAlertOverlay');
        $alertMessage = $('#customAlertMessage');
        $alertIcon = $('#customAlertIcon');
        $alertConfirmBtn = $('#customAlertConfirmBtn');
        
        // 이벤트 리스너 등록
        $alertConfirmBtn.on('click', close);
        $alertOverlay.on('click', close);
        
        // ESC 키로 닫기
        $(document).on('keydown', function(e) {
            if (e.key === 'Escape' && !$alertPopup.hasClass('hidden')) {
                close();
            }
        });
        
        isInitialized = true;
    }
    
    // Confirm HTML 생성 및 DOM에 추가
    function initializeConfirm() {
        if (isConfirmInitialized) return;
        
        var confirmHTML = 
            '<div class="custom-alert-popup hidden" id="customConfirmPopup">' +
            '  <div class="custom-alert-popup_content">' +
            '    <div class="custom-alert-popup_icon" id="customConfirmIcon"></div>' +
            '    <div class="custom-alert-popup_message" id="customConfirmMessage"></div>' +
            '    <div class="custom-alert-popup_buttonGroup">' +
            '      <button class="custom-alert-popup_cancelBtn" id="customConfirmCancelBtn">\uCDE8\uC18C</button>' +
            '      <button class="custom-alert-popup_confirmBtn" id="customConfirmOkBtn">\uD655\uC778</button>' +
            '    </div>' +
            '  </div>' +
            '</div>' +
            '<div class="custom-alert-overlay hidden" id="customConfirmOverlay"></div>';
        
        $('body').append(confirmHTML);
        
        // DOM 요소 캐싱
        $confirmPopup = $('#customConfirmPopup');
        $confirmOverlay = $('#customConfirmOverlay');
        $confirmMessage = $('#customConfirmMessage');
        $confirmIcon = $('#customConfirmIcon');
        $confirmOkBtn = $('#customConfirmOkBtn');
        $confirmCancelBtn = $('#customConfirmCancelBtn');
        
        // 이벤트 리스너 등록
        $confirmOkBtn.on('click', function() {
            closeConfirm(true);
        });
        $confirmCancelBtn.on('click', function() {
            closeConfirm(false);
        });
        $confirmOverlay.on('click', function() {
            closeConfirm(false);
        });
        
        // ESC 키로 닫기
        $(document).on('keydown', function(e) {
            if (e.key === 'Escape' && !$confirmPopup.hasClass('hidden')) {
                closeConfirm(false);
            }
        });
        
        isConfirmInitialized = true;
    }
    
    // Confirm 표시
    function showConfirm(message, callback) {
        if (!isConfirmInitialized) {
            initializeConfirm();
        }
        
        confirmCallback = callback;
        
        // 메시지 설정
        $confirmMessage.text(message);
        
        // 아이콘 타입 설정 (question)
        $confirmIcon.removeClass('error info success');
        $confirmIcon.addClass('info');
        $confirmIcon.text('?');
        
        // 팝업 표시
        $confirmPopup.removeClass('hidden');
        $confirmOverlay.removeClass('hidden');
    }
    
    // Confirm 닫기
    function closeConfirm(result) {
        if (!isConfirmInitialized) return;
        
        $confirmPopup.addClass('hidden');
        $confirmOverlay.addClass('hidden');
        
        if (confirmCallback && result) {
            confirmCallback();
        }
        confirmCallback = null;
    }
    
    // Alert 표시
    function show(message, type, autoClose) {
        if (!isInitialized) {
            initialize();
        }
        
        // 메시지 설정
        $alertMessage.text(message);
        
        // 아이콘 타입 설정
        $alertIcon.removeClass('error info success');
        if (type === 'error') {
            $alertIcon.addClass('error');
            $alertIcon.html('✕');
        } else if (type === 'success') {
            $alertIcon.addClass('success');
            $alertIcon.html('✓');
        } else {
            $alertIcon.addClass('info');
            $alertIcon.html('ℹ');
        }
        
        // 팝업 표시
        $alertPopup.removeClass('hidden');
        $alertOverlay.removeClass('hidden');
        
        // 자동 닫기
        if (autoClose) {
            setTimeout(function() {
                close();
            }, autoClose);
        }
    }
    
    // Alert 닫기
    function close() {
        if (!isInitialized) return;
        
        $alertPopup.addClass('hidden');
        $alertOverlay.addClass('hidden');
    }
    
    // 공개 API
    return {
        // 초기화 (선택적으로 미리 호출 가능)
        init: function() {
            initialize();
        },
        
        // 에러 메시지
        error: function(message, autoClose) {
            show(message, 'error', autoClose);
        },
        
        // 정보 메시지
        info: function(message, autoClose) {
            show(message, 'info', autoClose);
        },
        
        // 성공 메시지
        success: function(message, autoClose) {
            show(message, 'success', autoClose);
        },
        
        // 수동으로 닫기
        close: function() {
            close();
        },
        
        // 호환성을 위한 별칭 함수들
        alert: function(message, autoClose) {
            show(message, 'error', autoClose);
        },
        
        message: function(message, autoClose) {
            show(message, 'info', autoClose);
        },
        
        // Confirm 다이얼로그
        confirm: function(message, callback) {
            showConfirm(message, callback);
        }
    };
})();

// 전역 함수로도 사용 가능하게 (하위 호환성)
function fnAlert(message, time) {
    CustomAlert.error(message, time);
}

function fnMessage(message, time) {
    CustomAlert.info(message, time);
}

function fnSuccess(message, time) {
    CustomAlert.success(message, time);
}

function fnInfoMessage(message, time) {
    CustomAlert.info(message, time);
}








<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Claude Chat Demo</title>

  <!-- Claude Chat 스타일 -->
  <link rel="stylesheet" href="${pageContext.request.contextPath}/claude-chat/claude-chat.css">

  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Noto Sans KR", sans-serif;
      background: #1e1e1e;
      color: #d4d4d4;
      height: 100vh;
      display: flex;
      flex-direction: column;
    }
    header {
      padding: 10px 16px;
      background: #252525;
      border-bottom: 1px solid #333;
      font-size: 14px;
      font-weight: 600;
      flex-shrink: 0;
    }
    #chat-container {
      flex: 1;
      overflow: hidden;
      max-width: 860px;
      width: 100%;
      margin: 0 auto;
      border-left: 1px solid #333;
      border-right: 1px solid #333;
    }
  </style>
</head>
<body>

<header>Claude Chat — JSP Demo</header>

<div id="chat-container"></div>

<!-- Claude Chat 위젯 스크립트 -->
<script src="${pageContext.request.contextPath}/claude-chat/claude-chat.js"></script>

<script>
  // ── 멘션 아이템 예시 (데이터 소스, 파일, 태그 등 무엇이든 가능) ──
  var mentionItems = [
    { id: 'ds-1', name: 'sales_data', type: 'CSV'     },
    { id: 'ds-2', name: 'users_db',   type: 'MariaDB' },
    { id: 'ds-3', name: 'orders',     type: 'JSON'    },
  ];

  var chat = new ClaudeChat('#chat-container', {
    // Servlet 컨텍스트 경로 자동 반영
    apiBase: '${pageContext.request.contextPath}/api/claude-chat',

    mentionItems: mentionItems,

    placeholder: '분석 요청을 입력하세요 (@소스명으로 멘션, Enter: 전송)',
    emptyHint:   'Claude에게 질문하거나 데이터 분석을 요청해보세요',

    onSessionCreate: function (session) {
      console.log('새 세션 생성:', session.id, 'cwd:', session.cwd);
    },

    onMentionSelect: function (sessionId, item) {
      console.log('@' + item.name + ' 선택 (type: ' + item.type + ')');
    },

    // 옵션 버튼 클릭 핸들러 — true 반환 시 Claude 재호출 건너뜀
    onOptionSelect: function (sessionId, option, index) {
      // 예시: 특정 옵션은 서버 API 직접 호출
      // return fetch('/api/run-query', { method: 'POST', body: JSON.stringify({ option }) })
      //   .then(function () { return true; });
      return Promise.resolve(false); // false: 기본 동작(Claude 재전송)
    },
  });
</script>

</body>
</html>

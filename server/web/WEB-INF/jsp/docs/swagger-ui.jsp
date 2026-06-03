<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>SSF2026 API Documentation</title>
  <link rel="shortcut icon" href="./favicon.ico" type="image/x-icon">
  <link rel="icon" href="./favicon.ico" type="image/x-icon">
    
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css">
  <style>
    body { margin: 0; padding: 0; }
    #swagger-ui { max-width: 1460px; margin: 0 auto; }
    .swagger-ui .topbar { display: none; }
    .custom-header {
      background: #2c3e50; color: white; padding: 15px 30px;
      font-family: sans-serif; display: flex; align-items: center; justify-content: space-between;
    }
    .custom-header h1 { margin: 0; font-size: 20px; }
    .custom-header .info { font-size: 13px; opacity: 0.8; }
    .custom-header .refresh-btn {
      background: #3498db; border: none; color: white; padding: 8px 16px;
      border-radius: 4px; cursor: pointer; font-size: 13px;
    }
    .custom-header .refresh-btn:hover { background: #2980b9; }
  </style>
</head>
<body>
  <div class="custom-header">
    <div>
      <h1>SSF2026 API Documentation</h1>
      <div class="info">서비스 REST API 문서</div>
    </div>
    <button class="refresh-btn" onclick="location.reload()">Refresh</button>
  </div>
  <div id="swagger-ui"></div>
  <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
  <script>
    SwaggerUIBundle({
      url: '${pageContext.request.contextPath}/docs/api-docs?refresh=true',
      dom_id: '#swagger-ui',
      deepLinking: true,
      presets: [SwaggerUIBundle.presets.apis, SwaggerUIBundle.SwaggerUIStandalonePreset],
      layout: 'BaseLayout',
      defaultModelsExpandDepth: -1,
      docExpansion: 'list',
      filter: true,
      tryItOutEnabled: true
    });
  </script>
</body>
</html>

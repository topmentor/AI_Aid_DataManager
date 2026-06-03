# AIDA 실행 (개발 모드) — Electron이 SSF 백엔드 jar를 자동 기동
# 사용: .\run.ps1            의존성 확인 + jar 준비 + 앱 실행
#       .\run.ps1 -Rebuild  jar를 새로 빌드한 뒤 실행
param([switch]$Rebuild)
. "$PSScriptRoot\tools\_common.ps1"

Assert-Deps
Ensure-Npm
Ensure-Jar -Force:$Rebuild

_step "앱 실행 (Ctrl+C로 종료, 종료 후 잔여 서버는 .\stop.ps1 로 정리)"
Push-Location $AidaApp
try { npm run dev } finally { Pop-Location }

# AidClaude SSF 서버 헬스 스모크 테스트
# 사용법: powershell -File server/smoke/health.ps1 [-Port 8765]
param([int]$Port = 8765)
$ErrorActionPreference = "Stop"
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent   # repo root
$jar  = Join-Path $root "server\target\aidclaude-server.jar"
$web  = Join-Path $root "server\web"
$acHome = Join-Path $env:TEMP ("aidclaude_smoke_" + [guid]::NewGuid().ToString("N"))
$log  = Join-Path $env:TEMP ("aidclaude_smoke_" + $Port + ".log")

if (-not (Test-Path $jar)) { throw "jar 없음: $jar (먼저 mvn package)" }

Write-Host "[smoke] home=$acHome port=$Port"
$p = Start-Process java -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err" -ArgumentList @(
  "-Daidclaude.home=$acHome", "-Dserver.port=$Port",
  "-Dwebapp.base=$web", "-jar", $jar)

try {
  $ok = $false
  for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 750
    try {
      $r = Invoke-RestMethod "http://localhost:$Port/AidClaude/api/checkHealth.do" -TimeoutSec 3
      if ($r) { Write-Host "[smoke] checkHealth:" ($r | ConvertTo-Json -Compress); $ok = $true; break }
    } catch { }
  }
  if (-not $ok) {
    Write-Host "[smoke] FAILED — server log tail:" -ForegroundColor Red
    if (Test-Path $log) { Get-Content $log -Tail 40 }
    if (Test-Path "$log.err") { Get-Content "$log.err" -Tail 40 }
    exit 1
  }
  Write-Host "[smoke] OK" -ForegroundColor Green
} finally {
  if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force }
  Remove-Item -Recurse -Force $acHome -ErrorAction SilentlyContinue
}

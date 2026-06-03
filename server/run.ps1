# AIDA SSF 백엔드 실행 (개발/단독)
# 사용: .\server\run.ps1 [-Port 8765] [-AcHome <dir>] [-Build]
param(
    [int]$Port = 8765,
    [string]$AcHome = (Join-Path $env:USERPROFILE ".aida"),
    [switch]$Build
)
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$dir = $PSScriptRoot
$jar = Join-Path $dir "target\aida-server.jar"
$web = Join-Path $dir "web"

if ($Build -or -not (Test-Path $jar)) {
    & (Join-Path $dir "build.ps1") -SkipTests
}

Write-Host "[run] http://localhost:$Port/AIDA  (home=$AcHome)" -ForegroundColor Cyan
& java "-Daida.home=$AcHome" "-Dserver.port=$Port" "-Dwebapp.base=$web" -jar $jar

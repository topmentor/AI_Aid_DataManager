# AIDA SSF 백엔드 빌드 — 실행 가능 fat-jar 생성
# 사용: .\server\build.ps1 [-Smoke]
#   (스모크는 기본 미실행 — 검증하려면 -Smoke)
param([switch]$Smoke)
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$dir = $PSScriptRoot

Write-Host "[build] mvn package (server/pom.xml)" -ForegroundColor Cyan
& mvn -f (Join-Path $dir "pom.xml") clean package -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "[build] 실패" -ForegroundColor Red; exit 1 }

$jar = Join-Path $dir "target\aida-server.jar"
if (-not (Test-Path $jar)) { Write-Host "[build] jar 없음: $jar" -ForegroundColor Red; exit 1 }
Write-Host "[build] OK: $jar" -ForegroundColor Green

if ($Smoke) {
    Write-Host "[build] 스모크 실행..." -ForegroundColor Cyan
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $dir "smoke\all.ps1")
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

# 전체 스모크 일괄 실행. 사용: powershell -File server/smoke/all.ps1
$ErrorActionPreference = "Continue"
$scripts = @(
    @{ name = "health";    port = 8801 },
    @{ name = "settings";  port = 8802 },
    @{ name = "catalog";   port = 8803 },
    @{ name = "schema";    port = 8804 },
    @{ name = "job";       port = 8805 },
    @{ name = "db";        port = 8806 },
    @{ name = "json";      port = 8807 },
    @{ name = "shapefile"; port = 8808 },
    @{ name = "extras";    port = 8809 }
)
$fail = 0
foreach ($s in $scripts) {
    $path = Join-Path $PSScriptRoot ($s.name + ".ps1")
    if (-not (Test-Path $path)) { Write-Host "SKIP $($s.name) (없음)" -ForegroundColor Yellow; continue }
    Write-Host "`n===== $($s.name) =====" -ForegroundColor Cyan
    & powershell -NoProfile -ExecutionPolicy Bypass -File $path -Port $s.port
    if ($LASTEXITCODE -ne 0) { $fail++ }
}
Write-Host ""
if ($fail -gt 0) { Write-Host "SMOKE SUITE FAILED ($fail)" -ForegroundColor Red; exit 1 }
Write-Host "SMOKE SUITE ALL PASS" -ForegroundColor Green

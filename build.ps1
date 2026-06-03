# AIDA 빌드 (SSF jar + Electron 패키지 인스톨러)
# 사용: .\build.ps1            전체: jar 빌드 + 렌더러 빌드 + 인스톨러 패키징(app\dist)
#       .\build.ps1 -NoPackage 인스톨러 없이 jar + electron-vite build 까지만
#       .\build.ps1 -SkipSmoke  jar 빌드 시 스모크 생략
param([switch]$NoPackage, [switch]$SkipSmoke)
. "$PSScriptRoot\tools\_common.ps1"

Assert-Deps -NeedMaven
Ensure-Npm

# 1) SSF 백엔드 fat-jar (+ 스모크)
_step "SSF 백엔드 빌드"
if ($SkipSmoke) {
    & (Join-Path $AidaServer "build.ps1") -SkipTests
} else {
    & (Join-Path $AidaServer "build.ps1")
}
if ($LASTEXITCODE -ne 0) { throw "SSF jar 빌드 실패" }
if (-not (Test-Path $AidaJar)) { throw "jar 없음: $AidaJar" }
_ok "jar: $AidaJar"

Push-Location $AidaApp
try {
    if ($NoPackage) {
        _step "렌더러/메인 빌드 (electron-vite)"
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "electron-vite build 실패" }
        _ok "빌드 산출물: app\out\"
    } else {
        _step "Electron 패키지 빌드 (electron-builder)"
        npm run package
        if ($LASTEXITCODE -ne 0) { throw "electron-builder 패키징 실패" }
        $dist = Join-Path $AidaApp "dist"
        _ok "인스톨러 산출물: $dist"
        Get-ChildItem $dist -Filter *setup*.exe -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    → $($_.Name)" -ForegroundColor Green }
    }
} finally { Pop-Location }

_step "완료"

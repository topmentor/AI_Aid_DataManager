# AIDA 빌드 (SSF jar + 동봉 JRE + Electron 패키지 인스톨러)
# 사용: .\build.ps1            전체: jar 빌드 + JRE(jlink) + 인스톨러 패키징(app\dist)
#       .\build.ps1 -NoPackage 인스톨러 없이 jar + electron-vite build 까지만
#       .\build.ps1 -Smoke     jar 빌드 시 스모크 테스트 실행(기본은 미실행)
#       .\build.ps1 -NoJre     JRE 동봉 생략(시스템 Java 사용)
param([switch]$NoPackage, [switch]$Smoke, [switch]$NoJre)
. "$PSScriptRoot\tools\_common.ps1"

# jlink로 최소 런타임(JRE)을 app\.jre 에 생성 (electron-builder가 resources/jre로 동봉)
function Build-Jre {
    $jreDir = Join-Path $AidaApp ".jre"
    $jlink = (Get-Command jlink -ErrorAction SilentlyContinue).Source
    if (-not $jlink -and $env:JAVA_HOME) { $jlink = Join-Path $env:JAVA_HOME "bin\jlink.exe" }
    if (-not $jlink) { $jc = (Get-Command java -ErrorAction SilentlyContinue).Source; if ($jc) { $jlink = Join-Path (Split-Path $jc) "jlink.exe" } }
    if (-not $jlink -or -not (Test-Path $jlink)) { throw "jlink를 찾을 수 없습니다 — JDK 17 설치 + JAVA_HOME 확인" }
    $jmods = Join-Path (Split-Path (Split-Path $jlink)) "jmods"
    if (-not (Test-Path $jmods)) { throw "jmods 없음: $jmods (JRE가 아닌 JDK 필요)" }

    Write-Host "    jlink → $jreDir" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $jreDir -ErrorAction SilentlyContinue
    & $jlink --module-path $jmods --add-modules ALL-MODULE-PATH `
        --output $jreDir --strip-debug --no-header-files --no-man-pages --compress=2
    if ($LASTEXITCODE -ne 0) { throw "jlink 실패" }
    _ok "JRE 생성 완료: $jreDir"
}

Assert-Deps -NeedMaven
Ensure-Npm

# 1) SSF 백엔드 fat-jar (스모크는 -Smoke 일 때만)
_step "SSF 백엔드 빌드"
& (Join-Path $AidaServer "build.ps1") -Smoke:$Smoke
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
        if ($NoJre) {
            # JRE 미동봉 — 빈 폴더로 두어 extraResources 오류 방지(런타임은 시스템 Java 사용)
            $jreDir = Join-Path $AidaApp ".jre"
            Remove-Item -Recurse -Force $jreDir -ErrorAction SilentlyContinue
            New-Item -ItemType Directory -Force -Path $jreDir | Out-Null
            _warn "JRE 동봉 생략(-NoJre) — 패키지 앱은 시스템 Java 17 필요"
        } else {
            _step "동봉 JRE 생성 (jlink)"
            Build-Jre
        }
        _step "Electron 패키지 빌드 (electron-builder)"
        npm run package
        if ($LASTEXITCODE -ne 0) { throw "electron-builder 패키징 실패" }
        $dist = Join-Path $AidaApp "dist"
        _ok "인스톨러 산출물: $dist"
        Get-ChildItem $dist -Filter *setup*.exe -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    → $($_.Name)" -ForegroundColor Green }
    }
} finally { Pop-Location }

_step "완료"

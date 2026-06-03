# AidClaude — 의존성 확인 + 빌드 스크립트 (SSF 백엔드 + Electron)
# 사용법:
#   .\setup.ps1          의존성 확인 + SSF jar 빌드
#   .\setup.ps1 -Dev     확인 후 개발 실행 (Electron이 SSF jar를 자동 기동)
#   .\setup.ps1 -Build   확인 후 배포 빌드 (jar + electron-vite build)

param(
    [switch]$Dev,
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$Root      = $PSScriptRoot
$AppDir    = Join-Path $Root "app"
$ServerDir = Join-Path $Root "server"
$Jar       = Join-Path $ServerDir "target\aidclaude-server.jar"
$AllOk     = $true

function Step($n, $msg)  { Write-Host "`n[$n] $msg" -ForegroundColor Cyan }
function Ok($msg)         { Write-Host "    OK  $msg" -ForegroundColor Green }
function Warn($msg)       { Write-Host "  WARN  $msg" -ForegroundColor Yellow }
function Fail($msg)       { Write-Host "  FAIL  $msg" -ForegroundColor Red; $script:AllOk = $false }

# ── 1. Node.js ≥ 20 ────────────────────────────────────────────────────────
Step 1 "Node.js"
try {
    $ver = node --version 2>&1
    $major = ([int]($ver -replace 'v','').Split('.')[0])
    if ($major -ge 20) { Ok "$ver" } else { Fail "$ver — v20 이상 필요 (https://nodejs.org)" }
} catch { Fail "node를 찾을 수 없습니다 (https://nodejs.org)" }

# ── 2. Java ≥ 17 + Maven ───────────────────────────────────────────────────
Step 2 "Java / Maven (SSF 백엔드)"
try {
    $jv = (java -version 2>&1 | Select-Object -First 1)
    if ($jv -match '"(\d+)') { $jmajor = [int]$Matches[1] } else { $jmajor = 0 }
    if ($jmajor -ge 17) { Ok "$jv" } else { Fail "Java 17+ 필요 (현재: $jv)" }
} catch { Fail "java를 찾을 수 없습니다 (Java 17+ 설치 필요)" }
try {
    $mv = (mvn -version 2>&1 | Select-Object -First 1)
    Ok "$mv"
} catch { Fail "maven(mvn)을 찾을 수 없습니다" }

# ── 3. Python ≥ 3.8 + 필수 패키지 (분석 기능) ──────────────────────────────
Step 3 "Python"
$PythonCmd = $null
foreach ($cmd in "python","python3") {
    try {
        $ver = & $cmd --version 2>&1
        if ($ver -match "Python (\d+)\.(\d+)") {
            if ([int]$Matches[1] -ge 3 -and [int]$Matches[2] -ge 8) { Ok "$ver  ($cmd)"; $PythonCmd = $cmd; break }
        }
    } catch { }
}
if (-not $PythonCmd) {
    Warn "Python 3.8+ 없음 — analyze.py 실행 기능 사용 불가"
} else {
    foreach ($pkg in "pandas","matplotlib") {
        $out = & $PythonCmd -c "import $pkg; print($pkg.__version__)" 2>&1
        if ($LASTEXITCODE -eq 0) { Ok "  $pkg $out" } else { Warn "  $pkg 없음  →  $PythonCmd -m pip install $pkg" }
    }
}

# ── 4. Claude Code CLI ─────────────────────────────────────────────────────
Step 4 "Claude Code CLI"
try { Ok "$(claude --version 2>&1)" } catch { Warn "claude CLI 없음 — 채팅 기능 사용 불가 (https://claude.ai/code)" }

# ── 5. npm 패키지 (변경 감지) ──────────────────────────────────────────────
Step 5 "npm 패키지"
$NmDir    = Join-Path $AppDir "node_modules"
$PkgLock  = Join-Path $AppDir "package-lock.json"
$NmMarker = Join-Path $NmDir  ".package-lock.json"
$needInstall = (-not (Test-Path $NmDir))
if (-not $needInstall -and (Test-Path $PkgLock) -and (Test-Path $NmMarker)) {
    $needInstall = (Get-Item $PkgLock).LastWriteTime -gt (Get-Item $NmMarker).LastWriteTime
}
if ($needInstall) {
    Write-Host "    npm install 실행 중..." -ForegroundColor Yellow
    Push-Location $AppDir; npm install; $code = $LASTEXITCODE; Pop-Location
    if ($code -ne 0) { Fail "npm install 실패"; exit 1 }
    Ok "설치 완료"
} else { Ok "node_modules 최신 상태" }

# ── 6. SSF 백엔드 jar 빌드 (없거나 -Build/-Dev 시) ─────────────────────────
Step 6 "SSF 백엔드 jar"
if ($AllOk -and ((-not (Test-Path $Jar)) -or $Build -or $Dev)) {
    Write-Host "    mvn package 실행 중..." -ForegroundColor Yellow
    & (Join-Path $ServerDir "build.ps1") -SkipTests
    if ($LASTEXITCODE -ne 0) { Fail "SSF jar 빌드 실패"; exit 1 }
    Ok "jar 빌드 완료: $Jar"
} elseif (Test-Path $Jar) { Ok "jar 존재: $Jar" }

# ── 요약 ───────────────────────────────────────────────────────────────────
Write-Host ""
if (-not $AllOk) {
    Write-Host "필수 항목 누락이 있습니다. 위 메시지를 확인하고 설치 후 다시 실행하세요." -ForegroundColor Red
    exit 1
}
Write-Host "의존성 확인 완료." -ForegroundColor Green

# ── 빌드 / 실행 ─────────────────────────────────────────────────────────────
Set-Location $AppDir
if ($Build) {
    Write-Host "`n배포 빌드 중..." -ForegroundColor Cyan
    npm run build
    if ($LASTEXITCODE -ne 0) { exit 1 }
    Write-Host "빌드 완료: app\out\  (인스톨러 패키징은 electron-builder 설정 추가 시 — 후속)" -ForegroundColor Green
} elseif ($Dev) {
    Write-Host "`n개발 실행 (Electron이 SSF jar를 자동 기동, Ctrl+C로 종료)" -ForegroundColor Cyan
    npm run dev
} else {
    Write-Host ""
    Write-Host "  개발 실행:  .\setup.ps1 -Dev"
    Write-Host "  배포 빌드:  .\setup.ps1 -Build"
}

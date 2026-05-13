# AidClaude — 의존성 확인 + 빌드 스크립트
# 사용법:
#   .\setup.ps1          의존성만 확인·설치
#   .\setup.ps1 -Dev     확인 후 개발 서버 실행
#   .\setup.ps1 -Build   확인 후 배포 빌드

param(
    [switch]$Dev,
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$AppDir  = Join-Path $PSScriptRoot "app"
$AllOk   = $true

# ── 출력 헬퍼 ──────────────────────────────────────────────────────────────
function Step($n, $msg)  { Write-Host "`n[$n] $msg" -ForegroundColor Cyan }
function Ok($msg)         { Write-Host "    OK  $msg" -ForegroundColor Green }
function Warn($msg)       { Write-Host "  WARN  $msg" -ForegroundColor Yellow }
function Fail($msg)       { Write-Host "  FAIL  $msg" -ForegroundColor Red; $script:AllOk = $false }

# ── 1. Node.js ≥ 20 ────────────────────────────────────────────────────────
Step 1 "Node.js"
try {
    $ver = node --version 2>&1
    $major = ([int]($ver -replace 'v','').Split('.')[0])
    if ($major -ge 20) { Ok "$ver" }
    else               { Fail "$ver — v20 이상 필요 (https://nodejs.org)" }
} catch {
    Fail "node를 찾을 수 없습니다 (https://nodejs.org)"
}

# ── 2. Python ≥ 3.8 + 필수 패키지 ─────────────────────────────────────────
Step 2 "Python"
$PythonCmd = $null
foreach ($cmd in "python","python3") {
    try {
        $ver = & $cmd --version 2>&1
        if ($ver -match "Python (\d+)\.(\d+)") {
            if ([int]$Matches[1] -ge 3 -and [int]$Matches[2] -ge 8) {
                Ok "$ver  ($cmd)"
                $PythonCmd = $cmd
                break
            }
        }
    } catch { }
}
if (-not $PythonCmd) {
    Warn "Python 3.8+ 없음 — analyze.py 실행 기능 사용 불가"
    Warn "      설치: https://www.python.org/downloads/"
} else {
    foreach ($pkg in "pandas","matplotlib") {
        $out = & $PythonCmd -c "import $pkg; print($pkg.__version__)" 2>&1
        if ($LASTEXITCODE -eq 0) { Ok "  $pkg $out" }
        else                     { Warn "  $pkg 없음  →  $PythonCmd -m pip install $pkg" }
    }
}

# ── 3. Claude Code CLI ─────────────────────────────────────────────────────
Step 3 "Claude Code CLI"
try {
    $ver = claude --version 2>&1
    Ok "$ver"
} catch {
    Warn "claude CLI 없음 — 채팅 기능 사용 불가"
    Warn "      설치: https://claude.ai/code"
}

# ── 4. npm install (변경 감지) ─────────────────────────────────────────────
Step 4 "npm 패키지"
$NmDir     = Join-Path $AppDir "node_modules"
$PkgLock   = Join-Path $AppDir "package-lock.json"
$NmMarker  = Join-Path $NmDir  ".package-lock.json"

$needInstall = (-not (Test-Path $NmDir))
if (-not $needInstall -and (Test-Path $PkgLock) -and (Test-Path $NmMarker)) {
    $needInstall = (Get-Item $PkgLock).LastWriteTime -gt (Get-Item $NmMarker).LastWriteTime
}

if ($needInstall) {
    Write-Host "    npm install 실행 중..." -ForegroundColor Yellow
    Set-Location $AppDir
    npm install
    if ($LASTEXITCODE -ne 0) { Fail "npm install 실패"; exit 1 }
    Ok "설치 완료 (postinstall: electron-rebuild 포함)"
} else {
    Ok "node_modules 최신 상태"
}

# ── 5. better-sqlite3 네이티브 모듈 ────────────────────────────────────────
Step 5 "better-sqlite3 (Electron 네이티브 빌드)"
$NativeNode = Join-Path $AppDir "node_modules\better-sqlite3\build\Release\better_sqlite3.node"

if (-not (Test-Path $NativeNode)) {
    Write-Host "    electron-rebuild 실행 중..." -ForegroundColor Yellow
    Set-Location $AppDir
    npx electron-rebuild -f -w better-sqlite3
    if ($LASTEXITCODE -ne 0) { Fail "electron-rebuild 실패"; exit 1 }
    Ok "rebuild 완료"
} else {
    # Electron 버전과 .node 파일의 최종 수정 시간으로 재빌드 필요 여부 판단
    $ElectronExe = Join-Path $AppDir "node_modules\electron\dist\electron.exe"
    if ((Test-Path $ElectronExe) -and
        (Get-Item $ElectronExe).LastWriteTime -gt (Get-Item $NativeNode).LastWriteTime) {
        Write-Host "    Electron 업데이트 감지 — electron-rebuild 실행 중..." -ForegroundColor Yellow
        Set-Location $AppDir
        npx electron-rebuild -f -w better-sqlite3
        if ($LASTEXITCODE -ne 0) { Fail "electron-rebuild 실패"; exit 1 }
        Ok "rebuild 완료"
    } else {
        $ElectronVer = ""
        $VerFile = Join-Path $AppDir "node_modules\electron\dist\version"
        if (Test-Path $VerFile) { $ElectronVer = " (Electron $(Get-Content $VerFile -Raw))" }
        Ok "better_sqlite3.node 유효$ElectronVer"
    }
}

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
    Write-Host "빌드 완료: app\out\" -ForegroundColor Green
} elseif ($Dev) {
    Write-Host "`n개발 서버 시작 (Ctrl+C로 종료)" -ForegroundColor Cyan
    npm run dev
} else {
    Write-Host ""
    Write-Host "  개발 서버:  .\setup.ps1 -Dev"
    Write-Host "  배포 빌드:  .\setup.ps1 -Build"
}

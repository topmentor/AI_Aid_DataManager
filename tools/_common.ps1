# AIDA 공통 헬퍼 (run.ps1 / build.ps1 에서 dot-source)
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$global:AidaRoot   = Split-Path $PSScriptRoot -Parent
$global:AidaApp    = Join-Path $AidaRoot "app"
$global:AidaServer = Join-Path $AidaRoot "server"
$global:AidaJar    = Join-Path $AidaServer "target\aida-server.jar"

function _ok($m)   { Write-Host "    OK  $m" -ForegroundColor Green }
function _warn($m) { Write-Host "  WARN  $m" -ForegroundColor Yellow }
function _fail($m) { Write-Host "  FAIL  $m" -ForegroundColor Red }
function _step($m) { Write-Host "`n[AIDA] $m" -ForegroundColor Cyan }

# 필수/선택 의존성 확인. 필수 누락 시 throw.
function Assert-Deps {
    param([switch]$NeedMaven)
    _step "의존성 확인"
    $allOk = $true

    # Node >= 20
    try {
        $ver = node --version 2>&1
        $major = [int](($ver -replace 'v', '').Split('.')[0])
        if ($major -ge 20) { _ok "Node $ver" } else { _fail "Node 20+ 필요 (현재 $ver)"; $allOk = $false }
    } catch { _fail "node 없음 (https://nodejs.org)"; $allOk = $false }

    # Java >= 17 (java -version 은 stderr → cmd /c 로 캡처)
    if (Get-Command java -ErrorAction SilentlyContinue) {
        $jv = (cmd /c "java -version 2>&1" | Select-Object -First 1)
        $jmajor = if ($jv -match '"(\d+)') { [int]$Matches[1] } else { 0 }
        if ($jmajor -ge 17) { _ok "Java ($jv)" } else { _fail "Java 17+ 필요 (현재 $jv)"; $allOk = $false }
    } else { _fail "java 없음 (Java 17+ 설치)"; $allOk = $false }

    if ($NeedMaven) {
        if (Get-Command mvn -ErrorAction SilentlyContinue) { _ok "Maven" } else { _fail "maven(mvn) 없음"; $allOk = $false }
    }

    # 선택: Python(분석) / Claude·Codex(터미널)는 경고만
    $py = $null
    foreach ($c in "python", "python3") { if (Get-Command $c -ErrorAction SilentlyContinue) { $py = $c; break } }
    if ($py) { _ok "Python ($py)" } else { _warn "Python 없음 — analyze.py 실행 불가(앱 내 설치 가능)" }
    if (-not (Get-Command claude -ErrorAction SilentlyContinue)) { _warn "claude CLI 없음 — 터미널 패널에서 설치 가능" }

    if (-not $allOk) { throw "필수 의존성이 누락되었습니다. 위 FAIL 항목을 설치 후 다시 실행하세요." }
}

# app 의존성 설치(변경 감지)
function Ensure-Npm {
    _step "npm 패키지"
    $nm  = Join-Path $AidaApp "node_modules"
    $lock = Join-Path $AidaApp "package-lock.json"
    $marker = Join-Path $nm ".package-lock.json"
    $need = -not (Test-Path $nm)
    if (-not $need -and (Test-Path $lock) -and (Test-Path $marker)) {
        $need = (Get-Item $lock).LastWriteTime -gt (Get-Item $marker).LastWriteTime
    }
    # package.json 이 lock 보다 새로우면(의존성 추가) 설치
    $pkg = Join-Path $AidaApp "package.json"
    if (-not $need -and (Test-Path $pkg) -and (Test-Path $marker)) {
        $need = (Get-Item $pkg).LastWriteTime -gt (Get-Item $marker).LastWriteTime
    }
    if ($need) {
        Write-Host "    npm install 실행 중..." -ForegroundColor Yellow
        Push-Location $AidaApp; npm install; $code = $LASTEXITCODE; Pop-Location
        if ($code -ne 0) { throw "npm install 실패" }
        _ok "설치 완료"
    } else { _ok "node_modules 최신" }
}

# SSF 서버 jar 보장(없거나 -Force 시 빌드)
function Ensure-Jar {
    param([switch]$Force)
    _step "SSF 백엔드 jar"
    if ($Force -or -not (Test-Path $AidaJar)) {
        & (Join-Path $AidaServer "build.ps1")
        if ($LASTEXITCODE -ne 0) { throw "SSF jar 빌드 실패" }
        _ok "jar 빌드 완료"
    } else { _ok "jar 존재: $AidaJar" }
}

param([int]$Port = 8797)
# Agent PTY 배관 스모크: start(command=whoami) → WS 수신 → kill
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$jar  = Join-Path $root "server\target\aida-server.jar"
$web  = Join-Path $root "server\web"
$acHome = Join-Path $env:TEMP ("acpty_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $acHome | Out-Null
$fail = 0
function Check($n, $c) { if ($c) { Write-Host "  PASS  $n" -ForegroundColor Green } else { Write-Host "  FAIL  $n" -ForegroundColor Red; $script:fail++ } }

$p = Start-Process java -PassThru -RedirectStandardOutput "$env:TEMP\acpty.log" -RedirectStandardError "$env:TEMP\acpty.err" -ArgumentList @(
    "-Daida.home=$acHome", "-Dserver.port=$Port", "-Dwebapp.base=$web", "-jar", $jar)
$base = "http://localhost:$Port/AIDA"
try {
    $up = $false
    for ($i = 0; $i -lt 40; $i++) { Start-Sleep -Milliseconds 700; try { Invoke-RestMethod "$base/api/checkHealth.do" -TimeoutSec 3 | Out-Null; $up = $true; break } catch {} }
    Check "server up" $up
    # WS endpoint 등록 로그 확인
    $log = Get-Content "$env:TEMP\acpty.log" -Raw -ErrorAction SilentlyContinue
    Check "WS endpoint registered" ($log -match "agent-pty WebSocket endpoint registered")

    # start (command=whoami 로 claude 없이 PTY 배관 검증; cwd=home 하위 허용)
    $body = "agent=claude&command=whoami&workingDirectory=" + [Uri]::EscapeDataString($acHome)
    $start = Invoke-RestMethod "$base/api/agent-pty/local" -Method Post -ContentType "application/x-www-form-urlencoded" -Body $body -TimeoutSec 10
    Check "start returns sessionId" (-not [string]::IsNullOrEmpty($start.sessionId))
    $sid = $start.sessionId

    # WebSocket 연결 + 출력 수신
    $ws = New-Object System.Net.WebSockets.ClientWebSocket
    $ct = [System.Threading.CancellationToken]::None
    $uri = [Uri]("ws://localhost:$Port/AIDA/ws/agent-pty/local/$([Uri]::EscapeDataString($sid))?cols=80&rows=24")
    $ws.ConnectAsync($uri, $ct).Wait(5000) | Out-Null
    Check "ws connected" ($ws.State -eq 'Open')

    $buf = New-Object byte[] 8192
    $seg = New-Object System.ArraySegment[byte](,$buf)
    $got = ""
    for ($i = 0; $i -lt 8; $i++) {
        $rcv = $ws.ReceiveAsync($seg, $ct)
        if ($rcv.Wait(2000)) { $r = $rcv.Result; if ($r.Count -gt 0) { $got += [System.Text.Encoding]::UTF8.GetString($buf, 0, $r.Count) } }
        if ($got.Trim().Length -gt 0) { break }
    }
    Check "ws received PTY output" ($got.Trim().Length -gt 0)
    try { $ws.Dispose() } catch {}

    # kill
    $kill = Invoke-RestMethod "$base/api/agent-pty/local/$sid/kill" -Method Post -TimeoutSec 5
    Check "kill ok" ($kill.sessionId -eq $sid)

    # ── 설치 확인(check) ──
    $chk = Invoke-RestMethod "$base/api/agent-pty/check?agent=claude" -TimeoutSec 5
    Check "check returns installed bool" ($chk.installed -is [bool])
    Check "check returns installCommand" (-not [string]::IsNullOrEmpty($chk.installCommand))

    # ── 설치 실행(install, 무해한 echo로 셸 PTY 경로 검증) ──
    $ibody = "agent=claude&command=" + [Uri]::EscapeDataString("echo installtest") + "&workingDirectory=" + [Uri]::EscapeDataString($acHome)
    $inst = Invoke-RestMethod "$base/api/agent-pty/install" -Method Post -ContentType "application/x-www-form-urlencoded" -Body $ibody -TimeoutSec 10
    Check "install returns sessionId" (-not [string]::IsNullOrEmpty($inst.sessionId))
    $iws = New-Object System.Net.WebSockets.ClientWebSocket
    $iuri = [Uri]("ws://localhost:$Port/AIDA/ws/agent-pty/local/$([Uri]::EscapeDataString($inst.sessionId))?cols=80&rows=24")
    $iws.ConnectAsync($iuri, $ct).Wait(5000) | Out-Null
    $igot = ""
    for ($i = 0; $i -lt 8; $i++) {
        $r2 = $iws.ReceiveAsync($seg, $ct)
        if ($r2.Wait(2000)) { $rr = $r2.Result; if ($rr.Count -gt 0) { $igot += [System.Text.Encoding]::UTF8.GetString($buf, 0, $rr.Count) } }
        if ($igot -match "installtest") { break }
    }
    Check "install shell PTY output" ($igot -match "installtest")
    try { $iws.Dispose() } catch {}
} finally {
    if ($p -and -not $p.HasExited) { Stop-Process -Id $p.Id -Force }
    Remove-Item -Recurse -Force $acHome -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force (Join-Path $env:TEMP "aida-tomcat-$Port") -ErrorAction SilentlyContinue
}
if ($fail -gt 0) { Write-Host "[agentpty] FAILED ($fail)" -ForegroundColor Red; Get-Content "$env:TEMP\acpty.err" -Tail 20 -ErrorAction SilentlyContinue; exit 1 }
Write-Host "[agentpty] ALL PASS" -ForegroundColor Green

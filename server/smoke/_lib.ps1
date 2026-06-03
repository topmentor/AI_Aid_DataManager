# 공통 스모크 헬퍼: 서버 1회 기동 후 스크립트블록 실행
# 사용: . $PSScriptRoot\_lib.ps1 ; Invoke-WithServer -Port 8770 -Body { param($base) ... }
$ErrorActionPreference = "Stop"

function Repo-Root { Split-Path (Split-Path $PSScriptRoot -Parent) -Parent }

function Start-AcServer {
    param([int]$Port, [string]$AcHome)
    $root = Repo-Root
    $jar  = Join-Path $root "server\target\aida-server.jar"
    $web  = Join-Path $root "server\web"
    if (-not (Test-Path $jar)) { throw "jar 없음: $jar (mvn package 먼저)" }
    $log = Join-Path $env:TEMP ("acsmoke_" + $Port + ".log")
    $p = Start-Process java -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err" -ArgumentList @(
        "-Daida.home=$AcHome", "-Dserver.port=$Port", "-Dwebapp.base=$web", "-jar", $jar)
    $base = "http://localhost:$Port/AIDA"
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Milliseconds 700
        try { Invoke-RestMethod "$base/api/checkHealth.do" -TimeoutSec 3 | Out-Null; return @{ Proc = $p; Base = $base; Log = $log } } catch {}
    }
    Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
    Get-Content $log -Tail 30 -ErrorAction SilentlyContinue
    Get-Content "$log.err" -Tail 30 -ErrorAction SilentlyContinue
    throw "서버 기동 실패 (port $Port)"
}

function Invoke-WithServer {
    param([int]$Port = 8770, [scriptblock]$Body)
    $acHome = Join-Path $env:TEMP ("acsmoke_home_" + [guid]::NewGuid().ToString("N"))
    $srv = Start-AcServer -Port $Port -AcHome $acHome
    try { & $Body $srv.Base $acHome }
    finally {
        if ($srv.Proc -and -not $srv.Proc.HasExited) { Stop-Process -Id $srv.Proc.Id -Force -ErrorAction SilentlyContinue }
        Remove-Item -Recurse -Force $acHome -ErrorAction SilentlyContinue
        Remove-Item -Recurse -Force (Join-Path $env:TEMP "aida-tomcat-$Port") -ErrorAction SilentlyContinue
    }
}

$script:AcFail = 0
function Check($name, $cond) {
    if ($cond) { Write-Host "  PASS  $name" -ForegroundColor Green }
    else { Write-Host "  FAIL  $name" -ForegroundColor Red; $script:AcFail++ }
}
function Post($base, $path, $obj) {
    $json  = ($obj | ConvertTo-Json -Depth 10)
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $r = Invoke-WebRequest "$base/$path" -Method Post -ContentType "application/json; charset=utf-8" -Body $bytes -TimeoutSec 15 -UseBasicParsing
    return [System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json
}
function Get-Ac($base, $path) {
    $r = Invoke-WebRequest "$base/$path" -TimeoutSec 15 -UseBasicParsing
    return [System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()) | ConvertFrom-Json
}

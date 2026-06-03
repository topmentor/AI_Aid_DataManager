# AIDA 프로세스 정지 — SSF 백엔드(java) + Electron 앱 + 잔여 PTY 정리
# 사용: .\stop.ps1
$ErrorActionPreference = "SilentlyContinue"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$root = $PSScriptRoot
$killed = 0

function Kill-ByCmdline($procName, $pattern, $label) {
    $procs = Get-CimInstance Win32_Process -Filter "Name='$procName'" |
             Where-Object { $_.CommandLine -and ($_.CommandLine -match $pattern) }
    foreach ($p in $procs) {
        try { Stop-Process -Id $p.ProcessId -Force; Write-Host "  정지: $label (PID $($p.ProcessId))" -ForegroundColor Green; $script:killed++ }
        catch { Write-Host "  실패: $label (PID $($p.ProcessId)) — $_" -ForegroundColor Red }
    }
}

Write-Host "[AIDA] 프로세스 정지" -ForegroundColor Cyan

# 1) SSF 백엔드 (java -jar ...aida-server.jar)
Kill-ByCmdline "java.exe" "aida-server" "SSF 백엔드(java)"

# 2) 개발 모드 Electron (이 저장소 경로에서 실행된 electron)
$escaped = [Regex]::Escape($root)
Kill-ByCmdline "electron.exe" $escaped "Electron(dev)"

# 3) 패키지 앱 (productName AIDA → AIDA.exe)
$aida = Get-Process -Name "AIDA" -ErrorAction SilentlyContinue
foreach ($p in $aida) { try { Stop-Process -Id $p.Id -Force; Write-Host "  정지: AIDA.exe (PID $($p.Id))" -ForegroundColor Green; $script:killed++ } catch {} }

# 4) Tomcat 임시 작업 디렉터리 정리
foreach ($base in @($root, (Join-Path $root "app"), (Join-Path $root "server"))) {
    Get-ChildItem -Path $base -Directory -Filter "tomcat.*" -ErrorAction SilentlyContinue |
        ForEach-Object { Remove-Item -Recurse -Force $_.FullName -ErrorAction SilentlyContinue; Write-Host "  정리: $($_.FullName)" -ForegroundColor DarkGray }
}

if ($killed -eq 0) { Write-Host "[AIDA] 실행 중인 프로세스가 없습니다." -ForegroundColor Yellow }
else { Write-Host "[AIDA] $killed개 프로세스 정지 완료." -ForegroundColor Green }

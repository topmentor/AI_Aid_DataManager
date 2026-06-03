param([int]$Port = 8771)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[settings] $base"

    $g1 = Get-Ac $base "api/getSettings.do"
    Check "getSettings result=OK" ($g1.result -eq "OK")
    Check "default claudeBin=claude" ($g1.resultMap.claudeBin -eq "claude")
    Check "default workspaceRoot non-empty" ([string]::IsNullOrEmpty($g1.resultMap.workspaceRoot) -eq $false)

    $s1 = Post $base "api/setSettings.do" @{ workspaceRoot = "X:\custom"; pythonBin = "py" }
    Check "setSettings result=OK" ($s1.result -eq "OK")
    Check "setSettings reflects workspaceRoot" ($s1.resultMap.workspaceRoot -eq "X:\custom")

    $g2 = Get-Ac $base "api/getSettings.do"
    Check "persisted workspaceRoot" ($g2.resultMap.workspaceRoot -eq "X:\custom")
    Check "persisted pythonBin" ($g2.resultMap.pythonBin -eq "py")
    Check "untouched claudeBin default" ($g2.resultMap.claudeBin -eq "claude")
}
if ($script:AcFail -gt 0) { Write-Host "[settings] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[settings] ALL PASS" -ForegroundColor Green

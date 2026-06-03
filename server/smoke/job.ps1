param([int]$Port = 8776)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[job] $base"

    $csv = Join-Path $acHome "sales.csv"
    $content = "region,amount`nSeoul,100`nBusan,200`nSeoul,50`n"
    [System.IO.File]::WriteAllText($csv, $content, (New-Object System.Text.UTF8Encoding($false)))

    $a = Post $base "api/addSource.do" @{ name = "매출"; type = "csv"; config = @{ filePath = $csv } }
    $sid = $a.resultMap.id

    $j = Post $base "api/createJob.do" @{ userRequest = "지역별 합계"; sourceIds = @($sid) }
    Check "createJob result=OK" ($j.result -eq "OK")
    Check "job has id" (-not [string]::IsNullOrEmpty($j.resultMap.id))
    Check "job status idle" ($j.resultMap.status -eq "idle")
    $ws = $j.resultMap.workspaceDir
    Check "workspaceDir returned" (-not [string]::IsNullOrEmpty($ws))
    Check "data.db created" (Test-Path (Join-Path $ws "data.db"))
    Check "CLAUDE.md created" (Test-Path (Join-Path $ws "CLAUDE.md"))
    Check "data_helpers.py created" (Test-Path (Join-Path $ws "data_helpers.py"))
    Check "output dir created" (Test-Path (Join-Path $ws "output"))

    $claude = [System.IO.File]::ReadAllText((Join-Path $ws "CLAUDE.md"), [System.Text.Encoding]::UTF8)
    Check "CLAUDE.md lists table" ($claude -match "매출")
    Check "CLAUDE.md row count" ($claude -match "3행")

    $l = Get-Ac $base "api/listJobs.do"
    Check "listJobs has 1" ($l.count -eq 1)
    Check "listJobs userRequest" ($l.resultList[0].userRequest -eq "지역별 합계")

    $rf = Post $base "api/refreshJobSources.do" @{ jobId = $j.resultMap.id }
    Check "refreshJobSources OK" ($rf.result -eq "OK")
}
if ($script:AcFail -gt 0) { Write-Host "[job] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[job] ALL PASS" -ForegroundColor Green

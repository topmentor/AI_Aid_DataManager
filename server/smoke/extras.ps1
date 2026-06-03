param([int]$Port = 8782)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[extras] $base"
    $enc = New-Object System.Text.UTF8Encoding($false)

    # ── Files: writeText / readText (한글) ──
    $scratch = Join-Path $acHome "scratch.txt"
    $w = Post $base "api/writeText.do" @{ path = $scratch; content = "안녕 AIDA" }
    Check "writeText OK" ($w.result -eq "OK")
    $rt = Post $base "api/readText.do" @{ path = $scratch }
    Check "readText korean roundtrip" ($rt.resultMap.content -eq "안녕 AIDA")

    # 경로 탈출 차단
    $bad = Post $base "api/readText.do" @{ path = "C:\Windows\win.ini" }
    Check "path traversal blocked" ($bad.result -eq "ERROR")

    # copyToData (홈 밖 원본 → data/)
    $src = Join-Path $env:TEMP ("acsrc_" + [guid]::NewGuid().ToString("N") + ".txt")
    [System.IO.File]::WriteAllText($src, "hello", $enc)
    $cp = Post $base "api/copyToData.do" @{ srcPath = $src }
    Check "copyToData OK" ($cp.result -eq "OK")
    Check "copied into data dir" ($cp.resultMap.path -like (Join-Path $acHome "data") + "*")
    Remove-Item $src -ErrorAction SilentlyContinue

    # ── job + getSqlOptions / listQueryHistory ──
    $csv = Join-Path $acHome "sales.csv"
    [System.IO.File]::WriteAllText($csv, "region,amount`nSeoul,100`nBusan,200`nSeoul,50`n", $enc)
    $a = Post $base "api/addSource.do" @{ name = "sales"; type = "csv"; config = @{ filePath = $csv } }
    $j = Post $base "api/createJob.do" @{ userRequest = "q"; sourceIds = @($a.resultMap.id) }
    $jobId = $j.resultMap.id; $ws = $j.resultMap.workspaceDir

    $qsql = "-- [옵션 1] 합계`nDROP TABLE IF EXISTS result; CREATE TABLE result AS SELECT region, SUM(CAST(amount AS INTEGER)) t FROM sales GROUP BY region;`n-- [옵션 2] 카운트`nDROP TABLE IF EXISTS result; CREATE TABLE result AS SELECT region, COUNT(*) c FROM sales GROUP BY region;"
    Post $base "api/writeText.do" @{ path = (Join-Path $ws "query.sql"); content = $qsql } | Out-Null
    $opt = Post $base "api/getSqlOptions.do" @{ jobId = $jobId }
    Check "getSqlOptions 2 options" ($opt.count -eq 2)
    Check "option1 title" ($opt.resultList[0].title -eq "합계")

    $ra = Post $base "api/runJobAnalysis.do" @{ jobId = $jobId }
    Check "runJobAnalysis(query.sql) ok" ($ra.resultMap.ok -eq $true)
    $hist = Post $base "api/listQueryHistory.do" @{ jobId = $jobId }
    Check "queryHistory has 1" ($hist.count -ge 1)

    # ── orphan tables ──
    Post $base "api/runJobSql.do" @{ jobId = $jobId; sql = "CREATE TABLE junk_xyz AS SELECT 1 AS a;" } | Out-Null
    $orph = Get-Ac $base "api/listOrphanTables.do"
    $hasJunk = $false
    foreach ($g in $orph.resultList) { if ($g.tables -contains "junk_xyz") { $hasJunk = $true } }
    Check "orphan junk detected" ($hasJunk)
    $drop = Post $base "api/dropOrphanTables.do" @{}
    Check "dropOrphanTables dropped>=1" ($drop.resultMap.dropped -ge 1)
    $lt = Post $base "api/listTables.do" @{ jobId = $jobId }
    Check "junk dropped, sales kept" (($lt.resultList -notcontains "junk_xyz") -and ($lt.resultList -contains "sales"))

    # ── Python analyze.py (AST validated) ──
    $py = "import data_helpers as dh`nimport pandas as pd`ndf = dh.load('sales')`nout = df.groupby('region').size().reset_index(name='cnt')`nout.to_csv('output/result.csv', index=False)`nprint('rows', len(out))"
    Post $base "api/writeText.do" @{ path = (Join-Path $ws "analyze.py"); content = $py } | Out-Null
    $pa = Post $base "api/runJobAnalysis.do" @{ jobId = $jobId }
    Check "python analyze ok" ($pa.resultMap.ok -eq $true)
    $files = @($pa.resultMap.outputFiles | ForEach-Object { $_.name })
    Check "output result.csv produced" ($files -contains "result.csv")

    # AST 차단: import os
    $bad2 = "import os`nimport data_helpers as dh`nprint(1)"
    Post $base "api/writeText.do" @{ path = (Join-Path $ws "analyze.py"); content = $bad2 } | Out-Null
    $pb = Post $base "api/runJobAnalysis.do" @{ jobId = $jobId }
    Check "AST blocks import os" ($pb.resultMap.ok -eq $false)
}
if ($script:AcFail -gt 0) { Write-Host "[extras] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[extras] ALL PASS" -ForegroundColor Green

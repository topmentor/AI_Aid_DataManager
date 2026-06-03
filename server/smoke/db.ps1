param([int]$Port = 8777)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[db] $base"

    $csv = Join-Path $acHome "sales.csv"
    [System.IO.File]::WriteAllText($csv, "region,amount`nSeoul,100`nBusan,200`nSeoul,50`n", (New-Object System.Text.UTF8Encoding($false)))
    $a = Post $base "api/addSource.do" @{ name = "sales"; type = "csv"; config = @{ filePath = $csv } }
    $j = Post $base "api/createJob.do" @{ userRequest = "sum"; sourceIds = @($a.resultMap.id) }
    $jobId = $j.resultMap.id

    # SQL 실행: result 테이블 생성
    $r = Post $base "api/runJobSql.do" @{ jobId = $jobId; sql = "DROP TABLE IF EXISTS result; CREATE TABLE result AS SELECT region, SUM(CAST(amount AS INTEGER)) AS total FROM sales GROUP BY region ORDER BY region;" }
    Check "runJobSql result=OK" ($r.result -eq "OK")
    Check "runJobSql ok=true" ($r.resultMap.ok -eq $true)

    $lt = Post $base "api/listTables.do" @{ jobId = $jobId }
    Check "listTables has result" ($lt.resultList -contains "result")
    Check "listTables has sales" ($lt.resultList -contains "sales")

    $pv = Post $base "api/previewTable.do" @{ jobId = $jobId; tableName = "result" }
    Check "preview result headers" ($pv.resultMap.headers -contains "region" -and $pv.resultMap.headers -contains "total")
    Check "preview result 2 rows" ($pv.resultMap.rows.Count -eq 2)
    $map = @{}; foreach ($row in $pv.resultMap.rows) { $map[[string]$row[0]] = [string]$row[1] }
    Check "preview Busan total 200" ($map["Busan"] -eq "200")
    Check "preview Seoul total 150" ($map["Seoul"] -eq "150")

    # 두 번째 실행 → result_bak_001 생성(백업)
    $r2 = Post $base "api/runJobSql.do" @{ jobId = $jobId; sql = "DROP TABLE IF EXISTS result; CREATE TABLE result AS SELECT 1 AS x;" }
    Check "second runJobSql OK" ($r2.resultMap.ok -eq $true)
    $lt2 = Post $base "api/listTables.do" @{ jobId = $jobId }
    Check "result_bak_001 created" ($lt2.resultList -contains "result_bak_001")

    # 소스로 저장
    $sv = Post $base "api/saveTableAsSource.do" @{ jobId = $jobId; tableName = "sales"; sourceName = "sales_복사" }
    Check "saveTableAsSource OK" ($sv.result -eq "OK")
    Check "saved source name" ($sv.resultMap.name -eq "sales_복사")
    $ls = Get-Ac $base "api/listSources.do"
    Check "catalog has 2 sources now" ($ls.count -eq 2)

    # 임의 데이터 저장
    $sd = Post $base "api/saveDataAsSource.do" @{ sourceName = "manual"; headers = @("a","b"); rows = @(@("1","2"),@("3","4")) }
    Check "saveDataAsSource OK" ($sd.result -eq "OK")

    # 잘못된 SQL → 오류
    $bad = Post $base "api/runJobSql.do" @{ jobId = $jobId; sql = "SELECT * FROM nonexistent_xyz;" }
    Check "bad SQL ok=false" ($bad.resultMap.ok -eq $false)
}
if ($script:AcFail -gt 0) { Write-Host "[db] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[db] ALL PASS" -ForegroundColor Green

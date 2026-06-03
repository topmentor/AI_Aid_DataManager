param([int]$Port = 8778)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[json] $base"
    $enc = New-Object System.Text.UTF8Encoding($false)

    # JSON 배열
    $ja = Join-Path $acHome "arr.json"
    [System.IO.File]::WriteAllText($ja, '[{"id":1,"name":"가"},{"id":2,"name":"나","extra":"x"}]', $enc)
    $a = Post $base "api/addSource.do" @{ name = "jarr"; type = "json"; config = @{ filePath = $ja } }
    $sc = Post $base "api/getSchema.do" @{ id = $a.resultMap.id }
    Check "json array rowCount=2" ($sc.resultMap.rowCount -eq 2)
    Check "json array union columns 3" ($sc.resultMap.columns.Count -eq 3)
    $pv = Post $base "api/previewData.do" @{ id = $a.resultMap.id }
    Check "json array preview korean" ($pv.resultMap.rows[0][1] -eq "가")

    # JSON 객체 (key/value)
    $jo = Join-Path $acHome "obj.json"
    [System.IO.File]::WriteAllText($jo, '{"alpha":1,"beta":"two"}', $enc)
    $b = Post $base "api/addSource.do" @{ name = "jobj"; type = "json"; config = @{ filePath = $jo } }
    $scb = Post $base "api/getSchema.do" @{ id = $b.resultMap.id }
    Check "json object key/value 2 cols" ($scb.resultMap.columns.Count -eq 2)
    Check "json object 2 rows" ($scb.resultMap.rowCount -eq 2)

    # JSONL
    $jl = Join-Path $acHome "lines.jsonl"
    [System.IO.File]::WriteAllText($jl, "{`"a`":1}`n{`"a`":2}`n{`"a`":3}`n", $enc)
    $c = Post $base "api/addSource.do" @{ name = "jl"; type = "jsonl"; config = @{ filePath = $jl } }
    $scc = Post $base "api/getSchema.do" @{ id = $c.resultMap.id }
    Check "jsonl rowCount=3" ($scc.resultMap.rowCount -eq 3)

    # 적재 + SQL
    $j = Post $base "api/createJob.do" @{ userRequest = "t"; sourceIds = @($a.resultMap.id, $c.resultMap.id) }
    $lt = Post $base "api/listTables.do" @{ jobId = $j.resultMap.id }
    Check "job loaded jarr table" ($lt.resultList -contains "jarr")
    Check "job loaded jl table" ($lt.resultList -contains "jl")
    $pvj = Post $base "api/previewTable.do" @{ jobId = $j.resultMap.id; tableName = "jarr" }
    Check "jarr table 2 rows" ($pvj.resultMap.rows.Count -eq 2)
}
if ($script:AcFail -gt 0) { Write-Host "[json] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[json] ALL PASS" -ForegroundColor Green

param([int]$Port = 8775)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[schema] $base"

    # 따옴표/콤마/개행/한글 포함 CSV (UTF-8, no BOM)
    $csv = Join-Path $acHome "data.csv"
    $content = "id,name,note`n1,Alice,`"hello, world`"`n2,홍길동,`"줄1`n줄2`"`n3,Bob,plain`n"
    [System.IO.File]::WriteAllText($csv, $content, (New-Object System.Text.UTF8Encoding($false)))

    $a = Post $base "api/addSource.do" @{ name = "csv1"; type = "csv"; config = @{ filePath = $csv } }
    $id = $a.resultMap.id

    $sc = Post $base "api/getSchema.do" @{ id = $id }
    Check "schema result=OK" ($sc.result -eq "OK")
    Check "schema type=csv" ($sc.resultMap.type -eq "csv")
    Check "schema rowCount=3" ($sc.resultMap.rowCount -eq 3)
    Check "schema 3 columns" ($sc.resultMap.columns.Count -eq 3)
    Check "schema col0=id" ($sc.resultMap.columns[0].name -eq "id")

    $pv = Post $base "api/previewData.do" @{ id = $id; limit = 10 }
    Check "preview result=OK" ($pv.result -eq "OK")
    Check "preview headers 3" ($pv.resultMap.headers.Count -eq 3)
    Check "preview 3 rows" ($pv.resultMap.rows.Count -eq 3)
    Check "preview cell with comma intact" ($pv.resultMap.rows[0][2] -eq "hello, world")
    Check "preview korean row" ($pv.resultMap.rows[1][1] -eq "홍길동")
    Check "preview multiline cell" ($pv.resultMap.rows[1][2] -eq "줄1`n줄2")
}
if ($script:AcFail -gt 0) { Write-Host "[schema] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[schema] ALL PASS" -ForegroundColor Green

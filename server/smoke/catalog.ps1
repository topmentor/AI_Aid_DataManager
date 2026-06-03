param([int]$Port = 8772)
. "$PSScriptRoot\_lib.ps1"
Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[catalog] $base"

    # 임시 CSV 생성
    $csv = Join-Path $acHome "sample.csv"
    "id,name`n1,Alice`n2,""Bob, Jr.""" | Set-Content -Path $csv -Encoding UTF8

    $l0 = Get-Ac $base "api/listSources.do"
    Check "empty list initially" ($l0.count -eq 0)

    $a = Post $base "api/addSource.do" @{ name = "샘플CSV"; type = "csv"; config = @{ filePath = $csv } }
    Check "add result=OK" ($a.result -eq "OK")
    Check "add returns id" (-not [string]::IsNullOrEmpty($a.resultMap.id))
    Check "add config.filePath preserved" ($a.resultMap.config.filePath -eq $csv)
    $id = $a.resultMap.id

    $l1 = Get-Ac $base "api/listSources.do"
    Check "list has 1" ($l1.count -eq 1)
    Check "list item name (한글)" ($l1.resultList[0].name -eq "샘플CSV")

    $t = Post $base "api/testConnection.do" @{ id = $id }
    Check "testConnection ok=true" ($t.resultMap.ok -eq $true)

    $u = Post $base "api/updateSource.do" @{ id = $id; name = "변경됨"; type = "csv"; config = @{ filePath = $csv; delimiter = ";" } }
    Check "update name" ($u.resultMap.name -eq "변경됨")
    Check "update delimiter" ($u.resultMap.config.delimiter -eq ";")

    $tBad = Post $base "api/testConnection.do" @{ id = "no-such-id" }
    Check "testConnection missing id -> ERROR" ($tBad.result -eq "ERROR")

    $r = Post $base "api/removeSource.do" @{ id = $id }
    Check "remove result=OK" ($r.result -eq "OK")
    $l2 = Get-Ac $base "api/listSources.do"
    Check "list empty after remove" ($l2.count -eq 0)
}
if ($script:AcFail -gt 0) { Write-Host "[catalog] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[catalog] ALL PASS" -ForegroundColor Green

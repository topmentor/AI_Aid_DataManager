param([int]$Port = 8781)
. "$PSScriptRoot\_lib.ps1"

function Write-Field($bw, $name, $type, $len) {
    $nb = New-Object byte[] 11
    $src = [System.Text.Encoding]::ASCII.GetBytes($name)
    [Array]::Copy($src, $nb, [Math]::Min($src.Length, 11))
    $bw.Write($nb)                       # 0-10 name
    $bw.Write([byte][char]$type)         # 11 type
    $bw.Write((New-Object byte[] 4))     # 12-15 reserved
    $bw.Write([byte]$len)                # 16 length
    $bw.Write([byte]0)                   # 17 decimal
    $bw.Write((New-Object byte[] 14))    # 18-31 reserved
}
function Pad-Field($value, $len, $cp) {
    $enc = if ($cp -eq 'euckr') { [System.Text.Encoding]::GetEncoding(51949) } else { [System.Text.Encoding]::ASCII }
    $b = $enc.GetBytes($value)
    $out = New-Object byte[] $len
    for ($i = 0; $i -lt $len; $i++) { $out[$i] = 0x20 }   # space pad
    [Array]::Copy($b, $out, [Math]::Min($b.Length, $len))
    return ,$out
}
function New-TestDbf($path) {
    $ms = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter($ms)
    $bw.Write([byte]0x03); $bw.Write([byte]124); $bw.Write([byte]1); $bw.Write([byte]1)
    $bw.Write([uint32]2)      # record count
    $bw.Write([uint16]97)     # header length (32 + 2*32 + 1)
    $bw.Write([uint16]16)     # record length (1 + 10 + 5)
    $bw.Write((New-Object byte[] 20))
    Write-Field $bw "NAME" 'C' 10
    Write-Field $bw "VAL"  'C' 5
    $bw.Write([byte]0x0D)
    # records: 0x20 flag + fields
    $bw.Write([byte]0x20); $bw.Write((Pad-Field "Seoul" 10 'ascii')); $bw.Write((Pad-Field "100" 5 'ascii'))
    $bw.Write([byte]0x20); $bw.Write((Pad-Field "서울" 10 'euckr'));  $bw.Write((Pad-Field "200" 5 'ascii'))
    $bw.Flush()
    [System.IO.File]::WriteAllBytes($path, $ms.ToArray())
}

Invoke-WithServer -Port $Port -Body {
    param($base, $acHome)
    Write-Host "[shapefile] $base"
    $dbf = Join-Path $acHome "pts.dbf"
    New-TestDbf $dbf

    $a = Post $base "api/addSource.do" @{ name = "포인트"; type = "shapefile"; config = @{ shpPath = $dbf; encoding = "euc-kr" } }
    $sc = Post $base "api/getSchema.do" @{ id = $a.resultMap.id }
    Check "shp schema type" ($sc.resultMap.type -eq "shapefile")
    Check "shp rowCount=2" ($sc.resultMap.rowCount -eq 2)
    Check "shp 2 cols" ($sc.resultMap.columns.Count -eq 2)
    Check "shp col NAME" ($sc.resultMap.columns[0].name -eq "NAME")

    $pv = Post $base "api/previewData.do" @{ id = $a.resultMap.id }
    Check "shp preview 2 rows" ($pv.resultMap.rows.Count -eq 2)
    Check "shp ascii value" ($pv.resultMap.rows[0][0] -eq "Seoul")
    Check "shp euckr korean decoded" ($pv.resultMap.rows[1][0] -eq "서울")

    $j = Post $base "api/createJob.do" @{ userRequest = "t"; sourceIds = @($a.resultMap.id) }
    $pvt = Post $base "api/previewTable.do" @{ jobId = $j.resultMap.id; tableName = "포인트" }
    Check "shp loaded into job 2 rows" ($pvt.resultMap.rows.Count -eq 2)
}
if ($script:AcFail -gt 0) { Write-Host "[shapefile] FAILED ($script:AcFail)" -ForegroundColor Red; exit 1 }
Write-Host "[shapefile] ALL PASS" -ForegroundColor Green

param([string]$Path)
Add-Type -AssemblyName System.IO.Compression.FileSystem
$global:z=[IO.Compression.ZipFile]::OpenRead($Path)
Write-Output (($z.Entries | % FullName) -join "`n")
function ReadEntry($name){$e=$global:z.GetEntry($name);$sr=New-Object IO.StreamReader($e.Open());$s=$sr.ReadToEnd();$sr.Dispose();return $s}
$wb=[xml](ReadEntry 'xl/workbook.xml'); $rels=[xml](ReadEntry 'xl/_rels/workbook.xml.rels')
$ss=@(); if($global:z.GetEntry('xl/sharedStrings.xml')){$x=[xml](ReadEntry 'xl/sharedStrings.xml'); foreach($si in $x.sst.si){$ss += (($si.t | % { $_.'#text' }) -join '')}}
$n=0; foreach($sh in $wb.SelectNodes("//*[local-name()='sheet']")){$n++;$target="xl/worksheets/sheet$n.xml";$doc=[xml](ReadEntry $target); Write-Output "SHEET=$($sh.name)"; foreach($row in $doc.SelectNodes("//*[local-name()='sheetData']/*[local-name()='row']")){$vals=@(); foreach($c in $row.SelectNodes("./*[local-name()='c']")){$v=$c.v; if($c.t -eq 's' -and $v -ne $null){$vals += $ss[[int]$v]} else {$vals += [string]$v}}; Write-Output (($vals -join "`t"))}}
$global:z.Dispose()

$ErrorActionPreference='Stop'
$html='C:\Users\Admin\Desktop\lpsx\tmp\resume_ai.html';$docx='C:\claude\职业喵\郭浩远简历_AI应用开发版.docx';$pdf='C:\claude\职业喵\郭浩远简历_AI应用开发版.pdf'
$w=New-Object -ComObject Word.Application;$w.Visible=$false;$w.DisplayAlerts=0;$d=$null
try{$d=$w.Documents.Open($html);$d.SaveAs2($docx,16);$d.ExportAsFixedFormat($pdf,17);$d.Close($false);$d=$w.Documents.Open($docx,$false,$true);$d.Repaginate();$p=$d.ComputeStatistics(2);if($p -ne 1){throw "DOCX为$p页"};"PAGES=$p`nDOCX=$docx`nPDF=$pdf"}finally{try{if($d){$d.Close($false)}}catch{};try{$w.Quit()}catch{}}




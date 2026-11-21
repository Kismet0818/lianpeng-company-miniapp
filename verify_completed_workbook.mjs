import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";
const path = "C:/Users/Admin/Desktop/lpsx/outputs/feature_class_20261021/待完善特色班数据_初中635所_已完善.xlsx";
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
const summary = await wb.inspect({ kind: "workbook,sheet,table", maxChars: 5000, tableMaxRows: 3, tableMaxCols: 18 });
console.log(summary.ndjson);
for (const range of ["A6:R7","A38:R39","A163:R165","A309:R310","A441:R442","A494:R499","A559:R560","A624:R625"]) {
  const detail = await wb.inspect({ kind: "table", sheetId: "待完善635所", range, maxChars: 6000, tableMaxRows: 10, tableMaxCols: 18 });
  console.log(detail.ndjson);
}
for (const id of [196, 96, 637, 638, 380, 249, 723, 727, 559, 169, 147]) {
  const hit = await wb.inspect({ kind: "match", searchTerm: String(id), options: { useRegex: false, maxResults: 10 }, summary: `school_id ${id}` });
  console.log(hit.ndjson);
}
const errors = await wb.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!", options: { useRegex: true, maxResults: 100 }, summary: "saved file formula errors" });
console.log(errors.ndjson);

import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = "C:/Users/Admin/Downloads/待完善特色班数据_初中635所.xlsx";
const outDir = "C:/Users/Admin/Desktop/lpsx/.codex_work";
await fs.mkdir(outDir, { recursive: true });
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
const summary = await workbook.inspect({
  kind: "workbook,sheet,table,definedName,drawing",
  maxChars: 12000,
  tableMaxRows: 8,
  tableMaxCols: 16,
  tableMaxCellChars: 120,
});
console.log(summary.ndjson);
for (const sheet of workbook.worksheets.items) {
  const used = sheet.getUsedRange();
  console.log(`USED\t${sheet.name}\t${used?.address ?? ""}`);
  if (used) {
    const rows = Math.min(12, used.rowCount);
    const cols = Math.min(20, used.columnCount);
    const region = await workbook.inspect({
      kind: "region",
      sheetId: sheet.name,
      range: sheet.getRangeByIndexes(0, 0, rows, cols).address,
      maxChars: 12000,
    });
    console.log(region.ndjson);
    const preview = await workbook.render({ sheetName: sheet.name, range: sheet.getRangeByIndexes(0, 0, Math.min(30, used.rowCount), cols).address, scale: 1.3, format: "png" });
    await fs.writeFile(`${outDir}/preview_${sheet.name.replace(/[\\/:*?"<>|]/g, "_")}.png`, new Uint8Array(await preview.arrayBuffer()));
  }
}

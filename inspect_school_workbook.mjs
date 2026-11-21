import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const inputPath = 'C:/Users/Admin/Desktop/二三梯队小学.xlsx';
const outDir = 'C:/Users/Admin/Desktop/lpsx/tmp/school_inspect';
await fs.mkdir(outDir, { recursive: true });
const input = await FileBlob.load(inputPath);
const wb = await SpreadsheetFile.importXlsx(input);
const summary = await wb.inspect({kind:'workbook,sheet,table', maxChars:12000, tableMaxRows:12, tableMaxCols:15, tableMaxCellChars:120});
console.log(summary.ndjson);
for (const sh of wb.worksheets.items) {
  const used = sh.getUsedRange();
  console.log('SHEET', sh.name, 'USED', used ? used.address : 'none');
  if (used) {
    const preview = await wb.inspect({kind:'table', sheetId:sh.name, range:used.address, include:'values,formulas', tableMaxRows:80, tableMaxCols:20, tableMaxCellChars:160, maxChars:20000});
    console.log(preview.ndjson);
    const blob = await wb.render({sheetName:sh.name, autoCrop:'all', scale:1, format:'png'});
    await fs.writeFile(`${outDir}/${sh.name.replace(/[\\/:*?"<>|]/g,'_')}.png`, new Uint8Array(await blob.arrayBuffer()));
  }
}

import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const p='C:/Users/Admin/Desktop/资料合集/2026年招生合集-全(3)/2026年招生合集-全/2026年各区招生合集/2026年浦东新区招生合集.xlsx';
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(p));
console.log((await wb.inspect({kind:'sheet',include:'id,name'})).ndjson);
const s=wb.worksheets.getItemAt(0); console.log(JSON.stringify(s.getRange('A1:G20').values));

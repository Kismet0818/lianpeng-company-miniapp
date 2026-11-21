import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const targetPath = 'C:/Users/Admin/Desktop/lpsx/junior_high_school_info.xlsx';
const sourcePath = 'C:/Users/Admin/Desktop/资料合集/2026年招生合集-全(3)/2026年招生合集-全/2026年各区招生合集/2026年闵行区招生合集.xlsx';

for (const [label, path] of [['TARGET', targetPath], ['SOURCE', sourcePath]]) {
  const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
  console.log(`=== ${label} ===`);
  console.log((await wb.inspect({kind:'sheet', include:'id,name'})).ndjson);
  console.log((await wb.inspect({kind:'workbook,sheet,table', maxChars:12000, tableMaxRows:8, tableMaxCols:10, tableMaxCellChars:100})).ndjson);
}

const twb = await SpreadsheetFile.importXlsx(await FileBlob.load(targetPath));
const tsh = twb.worksheets.getItem('学校数据');
console.log('TARGET_HEADERS', JSON.stringify(tsh.getRange('A1:BB3').values));
const tv = tsh.getRange('A1:BB400').values;
console.log('TARGET_MINHANG', JSON.stringify(tv.filter(r => r.some(v => String(v ?? '').includes('闵行')))));

const swb = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
const ssh = swb.worksheets.getItem('公办初中招生划片范围');
console.log('SOURCE_ROWS', JSON.stringify(ssh.getRange('A1:D58').values));
console.log('TARGET_DUIKOU', JSON.stringify(tv.filter(r => r[5] === '闵行区').map(r => ({id:r[0],name:r[1],addr:r[3],cols:r.slice(43,54)}))));
console.log('TARGET_NONEMPTY_DUIKOU', JSON.stringify(tv.filter(r => r.slice(43,54).some(v => v != null)).slice(0,10).map(r => ({id:r[0],name:r[1],region:r[5],cols:r.slice(43,54)}))));

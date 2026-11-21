import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const targetPath = 'C:/Users/Admin/Desktop/lpsx/junior_high_school_info.xlsx';
const sourcePath = 'C:/Users/Admin/Desktop/资料合集/2026年招生合集-全(3)/2026年招生合集-全/2026年各区招生合集/2026年闵行区招生合集.xlsx';
const outputDir = 'C:/Users/Admin/Desktop/lpsx/outputs/minhang_duikou';
const outputPath = `${outputDir}/junior_high_school_info_minhang_completed.xlsx`;

const targetWb = await SpreadsheetFile.importXlsx(await FileBlob.load(targetPath));
const targetSheet = targetWb.worksheets.getItem('学校数据');
const sourceWb = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
const sourceSheet = sourceWb.worksheets.getItem('公办初中招生划片范围');

const targetRange = targetSheet.getRange('A1:BB400');
const targetValues = targetRange.values;
const sourceRows = sourceSheet.getRange('A1:D58').values.slice(2, 55).filter(r => r[1]);
const byName = new Map(sourceRows.map(r => [r[1], r]));
const alias = new Map([
  ['上海市闵行区莘松中学', ['上海市闵行区莘松中学（莘松校区）', '上海市闵行区莘松中学（春申校区）']],
  ['上海市古美学校（前身上海市古美中学）', ['上海市实验学校西校（南校区）']],
]);

let updated = 0;
const changed = [];
for (let i = 1; i < targetValues.length; i++) {
  const row = targetValues[i];
  if (row[5] !== '闵行区' || row[7] !== '公立') continue;
  const name = row[1];
  let ranges = [];
  if (byName.has(name)) ranges = [byName.get(name)[3]];
  else if (alias.has(name)) ranges = alias.get(name).map(n => byName.get(n)?.[3]).filter(Boolean);
  if (!ranges.length) continue;
  const html = ranges.length === 1
    ? `<p>2026年公办初中招生划片范围：${ranges[0]}</p>`
    : ranges.map((r, idx) => `<p>2026年公办初中招生划片范围（校区${idx + 1}）：${r}</p>`).join('');
  row[44] = html;
  changed.push({row: i + 1, id: row[0], name, sourceRows: ranges.length});
  updated++;
}

targetRange.values = targetValues;
targetWb.recalculate();
const check = await targetWb.inspect({kind:'table', sheetId:'学校数据', range:'A1:BB400', include:'values,formulas', tableMaxRows:400, tableMaxCols:54, tableMaxCellChars:80, maxChars:3000});
const errors = await targetWb.inspect({kind:'match', searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!', options:{useRegex:true,maxResults:100}, summary:'final formula error scan'});
console.log('UPDATED_COUNT', updated);
console.log('CHANGED', JSON.stringify(changed));
console.log('ERROR_SCAN', errors.ndjson);
await fs.mkdir(outputDir, {recursive:true});
const preview = await targetWb.render({sheetName:'学校数据', range:'A1:BB30', scale:1, format:'png'});
await fs.writeFile(`${outputDir}/preview.png`, new Uint8Array(await preview.arrayBuffer()));
const out = await SpreadsheetFile.exportXlsx(targetWb);
await out.save(outputPath);
console.log('OUTPUT', outputPath);

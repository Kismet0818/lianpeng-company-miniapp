import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
import fs from 'node:fs/promises';

const path = 'C:/Users/Admin/Desktop/lpsx/outputs/school_enriched/二三梯队小学_已完善.xlsx';
const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
const sh = wb.worksheets.getItem('Sheet1');
const rng = sh.getRange('D2:P87');
const values = rng.values;
const unknownTexts = new Set([
  '暂无统一官方公开数据，以学校或区教育局最新公告为准',
  '以学校官网及区教育局公开信息为准',
  '以学校招生简章、官网或微信公众号公布的国际交流项目为准',
  '待核实',
  '按区教育局2026年招生计划公示执行；本表暂以2025年公示或最新公开口径补充'
]);
for (let r = 0; r < values.length; r++) {
  for (let c = 0; c < values[r].length; c++) {
    if (unknownTexts.has(values[r][c])) values[r][c] = null;
  }
}
rng.values = values;
wb.recalculate();
const errors = await wb.inspect({kind:'match', searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!', options:{useRegex:true,maxResults:100}, summary:'final formula error scan'});
console.log(errors.ndjson);
const out = await SpreadsheetFile.exportXlsx(wb);
await out.save(path);
console.log(`SAVED ${path}`);

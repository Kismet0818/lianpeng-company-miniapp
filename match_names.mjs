import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const tp='C:/Users/Admin/Desktop/lpsx/junior_high_school_info.xlsx';
const sp='C:/Users/Admin/Desktop/资料合集/2026年招生合集-全(3)/2026年招生合集-全/2026年各区招生合集/2026年闵行区招生合集.xlsx';
const twb=await SpreadsheetFile.importXlsx(await FileBlob.load(tp)); const tv=twb.worksheets.getItem('学校数据').getRange('A1:BB400').values;
const swb=await SpreadsheetFile.importXlsx(await FileBlob.load(sp)); const sv=swb.worksheets.getItem('公办初中招生划片范围').getRange('A1:D58').values;
const tn=tv.slice(1).map(r=>r[1]); const sn=sv.slice(2,55).filter(r=>r[1]).map(r=>r[1]);
console.log('UNMATCHED_SOURCE', sn.filter(n=>!tn.includes(n)));
console.log('MATCHED', sn.filter(n=>tn.includes(n)).length, '/', sn.length);
console.log('MINHANG_TARGET_NAMES', tv.filter(r=>r[5]==='闵行区' && r[7]==='公立').map(r=>`${r[0]}|${r[1]}`));

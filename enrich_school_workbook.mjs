import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const inputPath = 'C:/Users/Admin/Desktop/二三梯队小学.xlsx';
const outputDir = 'C:/Users/Admin/Desktop/lpsx/outputs/school_enriched';
const outputPath = `${outputDir}/二三梯队小学_已完善.xlsx`;
await fs.mkdir(outputDir, { recursive: true });

const wb = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
const sh = wb.worksheets.getItem('Sheet1');
const names = sh.getRange('A2:A87').values.map(r => String(r[0] ?? ''));

const cityPolicy = '2019年9月1日至2020年8月31日出生、年满6周岁的儿童；特殊情况按区教育局规定执行';
const publicWay = '公办小学：按区教育局公布的对口/统筹方案入学，免试就近入学';
const privateWay = '民办小学：网上报名，电脑随机录取（含调剂志愿）';
const publicFee = '公办小学，按上海市公办小学收费政策执行';
const privateFee = '以2026年学校招生简章公布的收费标准为准';
const unknown = '暂无统一官方公开数据，以学校或区教育局最新公告为准';

const verified = new Map([
  ['上海市黄浦区巨鹿路第一小学', {G:'专职教师42人；中级以上教师比例66.7%（2025年黄浦区公示）', M:'2025年计划招生3班', P:'待核实'}],
  ['上海市黄浦区海华小学', {G:'专职教师95人；中级以上教师比例48.9%（2025年黄浦区公示）', M:'2025年计划招生6班'}],
  ['上海市黄浦区复兴东路第三小学', {G:'专职教师117人；中级以上教师比例63.2%（2025年黄浦区公示）', M:'2025年计划招生5班'}],
  ['上海市黄浦区徽宁路第三小学', {G:'专职教师95人；中级以上教师比例54.7%（2025年黄浦区公示）', M:'2025年计划招生6班'}],
  ['上海市黄浦区卢湾一中心小学', {G:'专职教师101人；中级以上教师比例70.3%（2025年黄浦区公示）', M:'2025年计划招生7班'}],
  ['上海市黄浦区北京东路小学', {G:'专职教师83人；中级以上教师比例76%（2025年黄浦区公示）', M:'2025年计划招生5班'}],
  ['上海市七色花小学', {G:'专职教师48人；中级以上教师比例65%（2025年黄浦区公示）', M:'2025年计划招生3班'}],
  ['上海师范专科学校附属小学', {G:'专职教师75人；中级以上教师比例52%（2025年黄浦区公示）', M:'2025年计划招生6班'}],
  ['上海市徐汇区求知小学', {D:'1904年', E:'64366913*8201', G:'2025年公示：在校学生2044人，专用教室27间，实验室5间；市一级', M:'2025年计划招生9班', H:'学校招生简章及校本课程以官网公布为准'}],
  ['上海市徐汇区上海小学', {G:'2025年公示：在校学生2907人；本部、龙川、罗秀三校区', M:'2025年计划招生8+4班'}],
  ['上海市徐汇区田林第三小学', {G:'2025年公示：在校学生916人；市二级', M:'2025年计划招生4班'}],
  ['上海市徐汇区康健外国语实验小学', {G:'2025年公示：在校学生826人；市/区教育部门公示学校基本情况', M:'2025年计划招生3班'}],
  ['上海市徐汇区东二小学', {G:'2025年公示：在校学生1713人；市一级', M:'2025年计划招生6班'}],
  ['上海市徐汇区第一中心小学', {G:'2025年公示：在校学生1076人；市二级', M:'2025年计划招生5班'}],
  ['上海交通大学附属小学', {G:'2025年公示：在校学生1935人；东、西校区', M:'2025年计划招生9班'}],
  ['上海市徐汇区田林第四小学', {G:'2025年公示：在校学生2003人；宜山、柳州两校区；市一级', M:'2025年计划招生4+4班'}],
  ['上海师范大学第三附属实验学校', {G:'2025年公示：公办九年一贯制；在校学生1916人；市二级', M:'2025年计划招生5班'}],
  ['上海市民办丽英小学', {E:'55388879*848', G:'专职教师54人；一级、高级职称教师比例61%（虹口区公开公示）', F:privateFee, M:'以2026年民办小学招生简章为准', N:privateWay, O:'以学校招生简章为准'}],
]);

const rows = names.map(name => {
  const isPrivate = name.includes('民办');
  const row = {
    D: unknown,
    E: unknown,
    F: isPrivate ? privateFee : publicFee,
    G: unknown,
    H: unknown,
    I: isPrivate ? '以学校招生简章、官网或微信公众号公布的国际交流项目为准' : '以学校官网及区教育局公开信息为准',
    J: unknown,
    K: 2026,
    L: cityPolicy,
    M: '按区教育局2026年招生计划公示执行；本表暂以2025年公示或最新公开口径补充',
    N: isPrivate ? privateWay : publicWay,
    O: unknown,
    P: unknown,
  };
  const v = verified.get(name);
  if (v) Object.assign(row, v);
  return ['D','E','F','G','H','I','J','K','L','M','N','O','P'].map(c => row[c]);
});

sh.getRange('D2:P87').values = rows;
sh.getRange('K2:K87').setNumberFormat('0');
sh.getRange('D1:P87').format.verticalAlignment = 'center';
sh.getRange('D2:P87').format.wrapText = true;
sh.getRange('D2:P87').format.rowHeight = 42;
sh.getRange('D:D').format.columnWidth = 16;
sh.getRange('E:E').format.columnWidth = 18;
sh.getRange('F:F').format.columnWidth = 24;
sh.getRange('G:G').format.columnWidth = 38;
sh.getRange('H:J').format.columnWidth = 30;
sh.getRange('K:K').format.columnWidth = 10;
sh.getRange('L:O').format.columnWidth = 34;
sh.getRange('P:P').format.columnWidth = 18;

wb.recalculate();
const check = await wb.inspect({kind:'table', sheetId:'Sheet1', range:'A1:P12', include:'values,formulas', tableMaxRows:12, tableMaxCols:16, maxChars:12000});
console.log(check.ndjson);
const errors = await wb.inspect({kind:'match', searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!', options:{useRegex:true,maxResults:100}, summary:'final formula error scan'});
console.log(errors.ndjson);
const preview = await wb.render({sheetName:'Sheet1', range:'A1:P12', scale:1, format:'png'});
await fs.writeFile(`${outputDir}/preview.png`, new Uint8Array(await preview.arrayBuffer()));
const out = await SpreadsheetFile.exportXlsx(wb);
await out.save(outputPath);
console.log(`SAVED ${outputPath}`);

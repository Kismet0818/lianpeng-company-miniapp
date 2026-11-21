import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';

const sourcePath = 'C:/Users/Admin/Desktop/资料合集/2026年招生合集-全(3)/2026年招生合集-全/2026年各区招生合集/2026年闵行区招生合集.xlsx';
const sourceWb = await SpreadsheetFile.importXlsx(await FileBlob.load(sourcePath));
const rows = sourceWb.worksheets.getItem('公办初中招生划片范围').getRange('A1:D58').values.slice(2,55).filter(r => r[1]);

const ids = new Map(Object.entries({
  '上海市航华中学':320,'上海市闵行区上虹中学':351,'上海市龙柏中学':323,'上海市闵行区七宝第二中学':348,
  '上海市闵行区七宝第二中学北校区':348,'上海市罗阳中学':324,'上海市闵行区莘松中学':352,
  '上海市闵行区莘松中学（莘松校区）':352,'上海市闵行区莘松中学（春申校区）':352,
  '上海市七宝中学附属闵行金都实验中学':359,'上海市闵行区北桥中学':335,'上海市闵行区浦江第一中学':347,
  '上海市闵行区浦江第二中学':345,'上海市闵行区浦江第三中学':346,'上海市闵行第四中学':333,
  '上海市闵行区颛桥中学':356,'上海市实验学校西校':364,'上海市实验学校西校（南校区）':318,
  '上海市七宝实验中学':358,'上海市师资培训中心附属闵行实验中学':363,'上海市吴泾中学':366,
  '上海市闵行区浦航实验中学':344,'上海师范大学附属闵行第三中学':314,'上海市闵行第三中学':332,
  '华东师范大学附属闵行永德学校（初中部）':303,'上海市闵行区诸翟学校':355,'华东师范大学附属闵行虹桥学校':302,
  '上海市闵行区纪王学校':338,'上海市马桥强恕学校':325,'上海师范大学康城实验学校':316,
  '上海市莘城学校':361,'上海市闵行区明星学校':343,'上海市闵行区文来实验学校':354,
  '上海市闵行区君莲学校':339,'上海市闵行区华漕学校':337,'上海世外教育附属浦江外国语学校':317,
  '上海交通大学附属闵行区马桥实验学校':306,'上海市闵行区七宝文来学校':350,'上师大附属中学闵行实验学校':315,
  '华东理工大学附属闵行梅陇实验学校':300,'上海交通大学附属闵行实验学校':null,'复旦大学附属闵行实验学校':null,
  '上海市闵行区文来九星学校（筹办）':null,'上海中医药大学附属闵行晶城中学':368,'上海师范大学附属闵行第三中学':314,
  '上海市闵行中学附属实验中学':357,'上海市七宝第三中学':349,'上海市闵行区七宝第三中学':349,'上海市七宝中学附属鑫都实验中学':360,
  '上海市莘光学校':362,'上海市闵行区鹤北初级中学':336,'上海市闵行区马桥复旦万科实验中学':341,
  '上海市闵行区田园外国语中学':353,'上海市闵行区龙茗中学':340,'华东师范大学第二附属中学附属初级中学':301,'上海交通大学附属第二中学':305
}));
const esc = v => v == null ? 'NULL' : `'${String(v).replaceAll('\\','\\\\').replaceAll("'", "''")}'`;
const sql = [];
sql.push('START TRANSACTION;');
let included=0, skipped=[];
for (const r of rows) {
  const seq=r[0], name=r[1], addr=r[2] ?? '', scope=r[3] ?? '';
  const jid=ids.get(name);
  if (!jid) { skipped.push(name); continue; }
  const full=`${seq} ${name} ${addr} ${scope}`;
  const note=`来源文件：2026年闵行区招生合集.xlsx；工作表：公办初中招生划片范围；第${Number(seq)+2}行`;
  sql.push(`UPDATE junior_high_primary_duikou SET scope_text=${esc(full)}, admission_method=COALESCE(admission_method,'按地段划片'), note=${esc(note)}, updated_time=NOW() WHERE admission_year=2026 AND district='闵行区' AND junior_high_school_id=${jid};`);
  sql.push(`INSERT INTO junior_high_primary_duikou (duikou_material_id, admission_year, district, junior_high_school_id, primary_school_id, primary_school_name, relation_type, admission_method, scope_text, note, sort_order, status) SELECT 17,2026,'闵行区',${jid},NULL,'详见招生划片范围','area_based','按地段划片',${esc(full)},${esc(note)},${Number(seq)},1 WHERE NOT EXISTS (SELECT 1 FROM junior_high_primary_duikou WHERE admission_year=2026 AND district='闵行区' AND junior_high_school_id=${jid});`);
  included++;
}
sql.push('COMMIT;');
await fs.writeFile('minhang_duikou_update.sql', sql.join('\n'), 'utf8');
console.log(JSON.stringify({included, skipped, statements:sql.length}));

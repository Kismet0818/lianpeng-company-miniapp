import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const inputPath = "C:/Users/Admin/Downloads/待完善特色班数据_初中635所.xlsx";
const researchPath = "C:/Users/Admin/Desktop/lpsx/research_feature_classes.tsv";
const outputDir = "C:/Users/Admin/Desktop/lpsx/outputs/feature_class_20261021";
const outputPath = `${outputDir}/待完善特色班数据_初中635所_已完善.xlsx`;

const sourceUrls = {
  vsxue_2026: "https://www.vsxue.com/3000.html（2026年第三方汇总，需校方复核）",
  jzcj_official: "https://www.jzcj.sh.cn/m/guizhangzhidu.html（学校官网课程介绍）",
  changning_gov_2022: "https://zwgk.shcn.gov.cn/xxgk/jyta-whjzhzw/2022/267/33270/c8450d1c757d4bc1bc9d68f13667fc8a.pdf（长宁区政府公开材料）",
  baike_yaoliansheng: "https://www.baike.com/wikiid/4265260242253033457（二手百科资料，需校方复核）",
};

const aliasBySchoolId = new Map([
  [638, "西南位育"],
  [637, "西南模范"],
  [559, "青浦世外"],
  [196, "嘉定世外"],
  [249, "永昌中学"],
  [169, "大同初级"],
  [96, "奉贤世外"],
  [380, "交中初级"],
  [723, "新泾中学"],
  [727, "姚连生中学"],
]);

const tsv = await fs.readFile(researchPath, "utf8");
const lines = tsv.replace(/^\uFEFF/, "").trimEnd().split(/\r?\n/);
const headers = lines[0].split("\t");
const research = lines.slice(1).map(line => {
  const cells = line.split("\t");
  return Object.fromEntries(headers.map((h, i) => [h, cells[i] ?? ""]));
});
const byAlias = new Map();
for (const row of research) {
  if (!byAlias.has(row.alias)) byAlias.set(row.alias, []);
  byAlias.get(row.alias).push(row);
}

const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(inputPath));
const sheet = workbook.worksheets.getItemAt(0);
const original = sheet.getRange("A1:R636").values;
const header = original[0];
const outputRows = [];
const matchedSchoolIds = [];
for (const base of original.slice(1)) {
  const schoolId = Number(base[2]);
  const alias = aliasBySchoolId.get(schoolId);
  const featureRows = alias ? (byAlias.get(alias) ?? []) : [];
  if (featureRows.length) {
    matchedSchoolIds.push(schoolId);
    for (const item of featureRows) {
      const row = [...base];
      if (item.admission_2026) row[10] = item.admission_2026;
      row[11] = item.class_type || null;
      row[12] = item.description || null;
      row[13] = item.class_count || null;
      row[14] = item.placement || null;
      row[15] = item.overall_classes || null;
      row[16] = sourceUrls[item.source] ?? item.source;
      row[17] = item.confidence || null;
      outputRows.push(row);
    }
  } else {
    const row = [...base];
    if (schoolId === 147) {
      row[12] = "2026年招生信息明确承诺均衡分班，不开设重点班、快慢班、特色班、实验班。";
      row[16] = "https://www.shangnaxue.net/school/1161623646194393090.html（2026招生信息）";
      row[17] = "高（招生简章口径）";
    } else {
      row[16] = "公开网络检索（截至2026-10-21）：未检索到可核验信息";
      row[17] = "未确认";
    }
    outputRows.push(row);
  }
}
outputRows.forEach((row, i) => row[0] = i + 1);

const oldTable = sheet.tables.items[0];
const oldTableName = oldTable?.name ?? "FeatureClassTable";
const oldTableStyle = oldTable?.style;
if (oldTable) oldTable.delete();
sheet.getRange(`A2:R${Math.max(636, outputRows.length + 1)}`).clear({ applyTo: "contents" });
sheet.getRange("A2").write(outputRows);
const lastRow = outputRows.length + 1;
const table = sheet.tables.add(`A1:R${lastRow}`, true, oldTableName);
if (oldTableStyle) table.style = oldTableStyle;
table.showFilterButton = true;
sheet.freezePanes.freezeRows(1);
sheet.getRange(`L2:R${lastRow}`).format.verticalAlignment = "center";
sheet.getRange(`M2:Q${lastRow}`).format.wrapText = true;
sheet.getRange(`L2:L${lastRow}`).format.wrapText = false;

workbook.recalculate();
const featureInspect = await workbook.inspect({
  kind: "match",
  searchTerm: "高（学校官网）|中（第三方汇总）|低（百科二手资料）",
  options: { useRegex: true, maxResults: 100 },
  summary: "populated feature-class rows",
});
console.log(featureInspect.ndjson);
const errors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!|#SPILL!|#CALC!",
  options: { useRegex: true, maxResults: 100 },
  summary: "final formula error scan",
});
console.log(errors.ndjson);
await fs.mkdir(outputDir, { recursive: true });
const preview = await workbook.render({ sheetName: sheet.name, range: `A1:R28`, scale: 1.2, format: "png" });
await fs.writeFile(`${outputDir}/preview.png`, new Uint8Array(await preview.arrayBuffer()));
const out = await SpreadsheetFile.exportXlsx(workbook);
await out.save(outputPath);
console.log(JSON.stringify({ outputPath, originalRows: original.length - 1, outputRows: outputRows.length, featureRows: outputRows.filter(r => r[11]).length, matchedSchoolIds: [...new Set(matchedSchoolIds)] }));

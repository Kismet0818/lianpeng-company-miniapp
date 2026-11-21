START TRANSACTION;

CREATE TEMPORARY TABLE tmp_local_district_guides (
  title varchar(200) NOT NULL PRIMARY KEY,
  summary varchar(500) NOT NULL,
  content text NOT NULL,
  sort_order int NOT NULL
);

INSERT INTO tmp_local_district_guides(title,summary,content,sort_order) VALUES
('2026年徐汇区公办初中入学方式',
 '徐汇区小学对口、电脑派位、对口与派位结合及九年一贯制等公办初中入学方式说明。',
 '<p><strong>资料范围：</strong>2026年徐汇区公办初中入学方式。</p><p><strong>核心规则：</strong>徐汇区继续按照“相对就近、免试入学”原则，结合学生户籍性质，采用小学对口入学和划块电脑派位等方式。</p><ul><li>资料列出的序号1—9为电脑派位；</li><li>序号10—12为电脑派位和对口入学相结合；</li><li>序号13—24为对口入学；</li><li>序号25—28为九年一贯制学校。</li></ul><p><strong>重要提醒：</strong>对口入学和电脑派位学生须满足文件所列本区学籍年限等条件；每户地址的同校对口机会及例外情形，以当年区教育局原文和学校通知为准。</p><p><strong>资料来源：</strong>本机资料合集中的2026年徐汇区招生公开资料汇编。</p>',11),
('2026年嘉定区公办初中划片范围与招生计划',
 '嘉定区公办初中的划片范围、招生方式及招生班数，适合按居住地核对对口学校。',
 '<p><strong>资料范围：</strong>2026年嘉定区义务教育阶段公办初中划片范围、招生方式与招生计划。</p><p>汇编按学校逐项列出划片边界和招生班数，覆盖嘉定镇街道、菊园新区及其他街镇。部分学校还注明未来年份学籍地对口规则或配套学校启用前的临时安排。</p><p><strong>查询建议：</strong>先按街镇定位，再同时核对道路边界、小区或社区名称和备注年份。新建学校、临时划片及跨年度调整不能仅凭学校距离判断。</p><p><strong>重要提醒：</strong>划片表适合规划参考，最终入学资格须以区教育局当年公告、户籍和房产材料审核结果为准。</p><p><strong>资料来源：</strong>本机资料合集中的2026年嘉定区招生公开资料汇编。</p>',12),
('2026年闵行区公办初中招生划片范围',
 '闵行区公办初中学校地址、招生划片范围和招生班级计划的集中说明。',
 '<p><strong>资料范围：</strong>2026年闵行区公办初中招生划片范围及招生计划。</p><p>汇编按学校列出校址和细化到小区、道路门牌或村居的招生范围，并另列起始年级招生计划班数；原始资料包含50余所公办初中及一贯制学校记录。</p><p><strong>查询建议：</strong>门牌和小区名称较长，核对时应同时确认学校名称、校区地址、完整门牌范围和当年招生计划，不要只按街道或直线距离判断。</p><p><strong>重要提醒：</strong>个别小区可能存在暂时安置、统筹或配套学校开办前的过渡安排，最终以闵行区教育局和学校当年审核口径为准。</p><p><strong>资料来源：</strong>本机资料合集中的2026年闵行区招生公开资料汇编。</p>',13),
('2026年虹口区公办初中招生计划与对口小学',
 '虹口区公办初中的地址、联系方式、拟招班级数和对口小学汇总说明。',
 '<p><strong>资料范围：</strong>2026年虹口区义务教育阶段公办初中招生计划和联系方式。</p><p>资料按学校列出学校地址、办学性质、联系电话、学校官网或公众号、拟招班级数和对口小学，可用于集中比较招生计划与小学对口关系。</p><p><strong>查询建议：</strong>同一学校可能按不同来源或招生简章分列计划，阅读时要把同校多行信息合并核对；联系方式适合用于确认特殊情况，不代表预录取。</p><p><strong>重要提醒：</strong>对口小学和招生班数可能因当年生源、校区安排调整，最终以虹口区教育局和学校公告为准。</p><p><strong>资料来源：</strong>本机资料合集中的2026年虹口区招生公开资料汇编。</p>',14),
('2026年青浦区中小学学区划分与招生计划',
 '青浦区公办学校招生计划，以及小学、初中按街镇、居委和路牌细分的学区信息说明。',
 '<p><strong>资料范围：</strong>2026年青浦区义务教育阶段公办学校招生计划和中小学学区划分。</p><p>原始汇编包含公办学校招生计划与资源配置，并将小学和初中学区细化到学校、街镇、居委或村、路牌及备注；其中小学、初中学区均有大量门牌级记录。</p><p><strong>查询建议：</strong>按“学校名称—街镇—居委/村—路牌—备注”五个层级逐项核对。相同道路不同号段、同一小区不同期数可能对应不同学校。</p><p><strong>重要提醒：</strong>门牌级信息适合初筛，实际入学仍需结合户籍、房产、居住证和统筹规则，以青浦区教育局当年审核结果为准。</p><p><strong>资料来源：</strong>本机资料合集中的2026年青浦区招生公开资料汇编。</p>',15);

UPDATE guide_article g
JOIN tmp_local_district_guides t ON t.title=g.title
SET g.category='升学规划',g.path_name='规划须知',g.summary=t.summary,
    g.content=t.content,g.thumbnail_url=NULL,g.sort_order=t.sort_order;

INSERT INTO guide_article(category,path_name,title,content,summary,thumbnail_url,sort_order)
SELECT '升学规划','规划须知',t.title,t.content,t.summary,NULL,t.sort_order
FROM tmp_local_district_guides t
WHERE NOT EXISTS (SELECT 1 FROM guide_article g WHERE g.title=t.title);

DROP TEMPORARY TABLE tmp_local_district_guides;
COMMIT;

-- 本地 lpsx：补录《上海外籍人员子女学校信息.docx》中的 HQIS。
-- 使用连续且当前无任何关联数据的 school.id=208；重复执行不会重复插入。
START TRANSACTION;

INSERT INTO school (
  id, name, city, district, curriculum, tuition_min, tuition_max,
  feature_tags, grades, description, graduation_result, boarding_info,
  class_size, teacher_ratio, logo_url, is_active, teaching_mode,
  founding_time, campus_area, school_type, address, official_website,
  schedule, teaching_features
)
SELECT
  208,
  '上海虹桥国际外籍人员子女学校',
  '上海',
  '长宁区',
  '萌发课程、IB PYP课程、美国中学课程、AP课程',
  9.846,
  26.00,
  'IB PYP,美国中学课程,AP课程,15年一贯制,多语言教育',
  '托班至12年级',
  '上海虹桥国际外籍人员子女学校成立于1997年，学校位于市中心古北黄金城道，是上海首批认证的国际学校之一，也是上海最早一批获得IBO认证的IB世界学校。经过28年脚踏实地的办学，目前学校已经发展成集幼儿园、小学、初高中于一体，15年一贯制的综合性国际学校，在校生来自35个国家和地区，旨在培养出具有世界公民精神的终身学习者。接受学生年龄范围为18个月至18岁。学生来自美国、欧洲、中国香港和台湾、韩国、日本等约35个国家和地区。',
  'HQIS的毕业生足迹遍布全球顶尖学府，收获了包括QS世界排名Top 50的不列颠哥伦比亚大学、清华大学、纽约大学、香港科技大学、悉尼大学；全美Top 50的范德堡大学、埃默里大学、波士顿大学、南加利福尼亚大学、加州大学尔湾分校、加州大学戴维斯分校等；世界排名Top 1的顶尖音乐学府英国皇家音乐学院；世界Top 10的顶尖艺术院校芝加哥艺术学院的录取通知书。',
  '无',
  '16~20人',
  '1:8',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/logo/2b98a63ae8ee45b9ae4652e8e43c9346.png',
  1,
  '英语为主；另设中文、法语、意大利语、韩语、日语',
  '1997年',
  25000.00,
  '外籍人员子女学校',
  '上海市长宁区伊犁南路218号',
  'https://www.hqis.org',
  '8:00—15:45',
  '学校拥有种类丰富的拓展课程、课后班课程及多个学生社团，包括英语阅读、EAL、SSAT、第二语言学习、数学、足球、篮球、击剑、高尔夫、跆拳道、太极、科学探索、AI编程、魔术、国际象棋、乐高、钢琴、管弦乐、打击乐、芭蕾、音乐剧和戏剧等，并配有学习支持老师。学校是美国私校理事会（EMA）理事学校，也是SSAT、SAT和AP考试考点。设HQIS奖学金（G6~G12）。新校园配备地热取暖和冷却系统、空气净化和新风系统、图书馆、彩虹桥剧场、屋顶有机花园、多媒体教室、艺术中心、音乐中心、室内标准体育场、室外篮球场、足球场、室内游泳池和室内运动馆，并有全职护士。联系电话：（021）6268 9773 /（021）6268 3121；邮箱：admissions@hqis.org；公众号：上海虹桥国际学校HQIS。'
WHERE NOT EXISTS (
  SELECT 1 FROM school
  WHERE id = 208 OR name = '上海虹桥国际外籍人员子女学校'
);

SELECT ROW_COUNT() AS inserted_rows;
COMMIT;

SELECT id, name, city, district, curriculum, grades, school_type,
       founding_time, campus_area, address, official_website, is_active
FROM school
WHERE id = 208 OR name = '上海虹桥国际外籍人员子女学校';

-- 依据用户提供的栏目截图修正本地文章目录；不删除已提取图片。
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='高中阶段可以报考哪些类型的学校', capture_status='complete' WHERE source_article_no='01';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='政策性照顾加分和同分优待情况' WHERE source_article_no='07';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='各区教育局官方网站、公众号汇总' WHERE source_article_no='08';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='招生志愿信息样表长什么样' WHERE source_article_no='09';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='图说上海市中招志愿类别和招生录取批次' WHERE source_article_no='11';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='招生代码怎么用' WHERE source_article_no='12';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='自主招生有哪几种' WHERE source_article_no='13';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='什么是名额分配综合评价录取志愿' WHERE source_article_no='14';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='什么是统一招生录取志愿' WHERE source_article_no='15';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='末位同分时怎么处理' WHERE source_article_no='16';
UPDATE admission_content_article SET source_category='志愿填报小知识', article_year=NULL, title='中考有哪些科目，分数如何分配', image_count=0, image_urls=JSON_ARRAY(), local_image_files=JSON_ARRAY(), capture_status='title_only', capture_note='标题来自用户提供的完整栏目截图，正文图片待后续单独抓取' WHERE source_article_no='missing-subject-score';

UPDATE admission_content_article SET source_category='详细文章', article_year=2024, title='2024年闵行、宝山、浦东、松江、青浦新增15所高中介绍' WHERE source_article_no='10';
UPDATE admission_content_article SET source_category='详细文章', article_year=2024, title='2024年市特色普通高中自主招生情况' WHERE source_article_no='17';
UPDATE admission_content_article SET source_category='详细文章', article_year=2024, title='2024年市级优秀体育学生招收学校和项目' WHERE source_article_no='18';
UPDATE admission_content_article SET source_category='详细文章', article_year=2024, title='2024年市级艺术骨干学生招收学校和项目' WHERE source_article_no='19';
UPDATE admission_content_article SET source_category='详细文章', article_year=2025, title='2025年徐汇、普陀、虹口、闵行、嘉定、浦东、松江新增11所高中介绍' WHERE source_article_no='22';
UPDATE admission_content_article SET source_category='详细文章', article_year=2025, title='2025年市级优秀体育学生招收学校和项目' WHERE source_article_no='23';
UPDATE admission_content_article SET source_category='详细文章', article_year=2025, title='2025年市级艺术骨干学生招收学校和项目' WHERE source_article_no='24';
UPDATE admission_content_article SET source_category='详细文章', article_year=2025, title='2025年20所市特色普通高中自主招生情况' WHERE source_article_no='25';
UPDATE admission_content_article SET source_category='详细文章', article_year=2026, title='2026新开17所高中介绍（黄浦、徐汇、静安、普陀、闵行……）', image_count=0, image_urls=JSON_ARRAY(), local_image_files=JSON_ARRAY(), capture_status='title_only', capture_note='标题来自用户提供的2026年详细文章栏目截图，正文待后续单独抓取' WHERE source_article_no='detail-2026-new-17-high-schools';

INSERT INTO admission_content_article
 (source_platform,source_category,source_article_no,title,article_year,content_format,content_text,source_url,image_urls,local_image_files,image_count,capture_status,capture_note,sort_order)
VALUES
 ('kylx365','详细文章','detail-2025-international','2025年国际课程班和中外合作办学自主招生计划情况',2025,'metadata_only',NULL,NULL,JSON_ARRAY(),JSON_ARRAY(),0,'title_only','标题来自用户提供的2025年详细文章栏目截图，正文待后续单独抓取',1),
 ('kylx365','详细文章','detail-2024-international','2024年国际课程班和中外合作办学自主招生计划情况',2024,'metadata_only',NULL,NULL,JSON_ARRAY(),JSON_ARRAY(),0,'title_only','标题来自用户提供的2024年详细文章栏目截图，正文待后续单独抓取',1)
ON DUPLICATE KEY UPDATE title=VALUES(title),article_year=VALUES(article_year),capture_status=VALUES(capture_status),capture_note=VALUES(capture_note),sort_order=VALUES(sort_order);

UPDATE admission_content_article
SET content_format = 'text_with_images',
    content_text = '上海市初中学业水平考试科目和分值设置（图片标注年份为2023年，使用时请以当年教育主管部门官方发布为准）。\n\n语文150分。\n数学150分。\n外语150分：笔试满分140分（含听力25分），外语听说测试满分10分。\n综合测试150分：物理试题70分、化学试题50分、跨学科案例分析15分、理化实验操作考试15分。\n道德与法治60分：统一开卷考试30分、日常考核30分。\n历史60分：统一开卷考试30分、日常考核30分。\n体育与健身30分：统一考试15分、日常考核15分。\n总计750分。\n\n投档末位同分时，依次比较语文、数学、英语三科合计成绩，数学成绩，语文成绩，综合测试成绩，高者优先。',
    image_urls = JSON_ARRAY(),
    local_image_files = JSON_ARRAY('志愿填报小知识/2023上海中考科目和分值设置_无水印重制版.png'),
    image_count = 1,
    capture_status = 'text_complete',
    capture_note = '正文依据用户截图人工整理；原图右上角含第三方小程序码与品牌文字，已按数据重新绘制无水印版本，未直接擦除来源标识；图片年份为2023年',
    update_time = NOW()
WHERE source_platform = 'kylx365'
  AND source_article_no = 'missing-subject-score';

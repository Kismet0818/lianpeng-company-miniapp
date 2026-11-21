UPDATE admission_content_article SET
  oss_image_urls=JSON_ARRAY('https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/07/required-chart.png'),
  capture_status='text_complete', image_count=1,
  local_image_files=JSON_ARRAY('最终文章图片/07-required-chart.png')
WHERE source_article_no='07';

UPDATE admission_content_article SET
  oss_image_urls=JSON_ARRAY('https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/08/required-chart.png'),
  capture_status='text_complete', image_count=1,
  local_image_files=JSON_ARRAY('最终文章图片/08-required-chart.png')
WHERE source_article_no='08';

UPDATE admission_content_article SET
  oss_image_urls=JSON_ARRAY('https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/09/sample-form.png'),
  capture_status='text_complete', image_count=1,
  local_image_files=JSON_ARRAY('最终文章图片/09-sample-form.png')
WHERE source_article_no='09';

UPDATE admission_content_article SET
  oss_image_urls=JSON_ARRAY(
    'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/required-flow.png',
    'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/required-schedule.png'),
  capture_status='text_complete', image_count=2,
  local_image_files=JSON_ARRAY('最终文章图片/11-required-flow.png','最终文章图片/11-required-schedule.png'),
  capture_note='标题和过渡句均使用数据库正文；仅保留完整的三批录取流程图和完整招生日程表'
WHERE source_article_no='11';

UPDATE admission_content_article SET
  oss_image_urls=JSON_ARRAY('https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/missing-subject-score/01.png'),
  capture_status='text_complete', image_count=1,
  local_image_files=JSON_ARRAY('最终文章图片/subject-score.png')
WHERE source_article_no='missing-subject-score';

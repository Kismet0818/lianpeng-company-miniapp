ALTER TABLE admission_content_article
  ADD COLUMN oss_image_urls JSON NULL COMMENT '按正文展示顺序排列的自有OSS图片URL数组' AFTER local_image_files;

UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY(
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/07/0701.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/07/0702.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/07/0703.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/07/0704.png'
) WHERE source_article_no='07';
UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY(
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/08/0801.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/08/0802.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/08/0803.png'
) WHERE source_article_no='08';
UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY(
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/09/0901.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/09/0902.png'
) WHERE source_article_no='09';
UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY(
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/1101.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/1102.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/1103.png',
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/11/1104.png'
) WHERE source_article_no='11';
UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY(
  'https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/missing-subject-score/01.png'
) WHERE source_article_no='missing-subject-score';

UPDATE admission_content_article SET oss_image_urls=JSON_ARRAY()
WHERE image_count=0 AND oss_image_urls IS NULL;

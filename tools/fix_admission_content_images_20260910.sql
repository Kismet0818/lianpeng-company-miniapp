UPDATE admission_content_article SET
  image_count=1,
  local_image_files=JSON_ARRAY('志愿填报小知识/上海中考政策性照顾加分及同分优待（新doc_0703）.png'),
  oss_image_urls=JSON_ARRAY(),
  capture_status='text_complete_image_ready',
  capture_note='正文已文字化；仅保留不可替代的完整政策性照顾表格，标题、说明段落不再作为图片展示；完整品牌图已就绪，待明确授权上传OSS后恢复展示'
WHERE source_article_no='07';

UPDATE admission_content_article SET
  image_count=1,
  local_image_files=JSON_ARRAY('志愿填报小知识/上海中考招生查询录取去向（新doc_0802＋doc_0803）.png'),
  oss_image_urls=JSON_ARRAY(),
  capture_status='text_complete_image_ready',
  capture_note='正文已文字化；旧0802和0803是同一张图的残缺切片，已改为完整长图，标题0801不再作为图片展示；完整品牌图已就绪，待明确授权上传OSS后恢复展示'
WHERE source_article_no='08';

UPDATE admission_content_article SET
  image_count=1,
  local_image_files=JSON_ARRAY('志愿填报小知识/doc_0902.png'),
  oss_image_urls=JSON_ARRAY('https://lianpeng.oss-cn-shanghai.aliyuncs.com/admission-content/09/0902.png'),
  capture_note='正文已文字化；仅保留志愿信息样表，标题0901不再作为图片展示'
WHERE source_article_no='09';

UPDATE admission_content_article SET
  image_count=0,
  local_image_files=JSON_ARRAY(),
  oss_image_urls=JSON_ARRAY(),
  capture_status='text_complete_image_pending',
  capture_note='标题1101、过渡句1102/1104已文字化且不应作为图片；1103流程图不完整，暂不展示，需重新抓取完整流程图后再补1张必要图片'
WHERE source_article_no='11';

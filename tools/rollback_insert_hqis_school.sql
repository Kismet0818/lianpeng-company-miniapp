-- 如需撤销 HQIS 补录，先确认该学校尚未产生业务关联，再运行本文件。
START TRANSACTION;

DELETE FROM school
WHERE id = 208
  AND name = '上海虹桥国际外籍人员子女学校'
  AND NOT EXISTS (SELECT 1 FROM school_image WHERE school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM user_favorite WHERE school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM admission_info WHERE school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM open_day WHERE school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM school_reaction WHERE school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM blessing WHERE school_source = 'international' AND school_id = 208)
  AND NOT EXISTS (SELECT 1 FROM user_activity WHERE school_source = 'international' AND school_id = 208);

SELECT ROW_COUNT() AS deleted_rows;
COMMIT;

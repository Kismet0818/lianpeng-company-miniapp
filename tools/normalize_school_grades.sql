-- 规范本地 lpsx.school.grades，仅保留前端可筛选的：小学、初中、高中。
-- 执行前备份全表 grades；学校性质用于修复原数据遗漏。

CREATE TABLE school_grades_backup_20260908 AS
SELECT id, name, school_type, grades
FROM school;

START TRANSACTION;

UPDATE school
SET grades = CASE
  WHEN school_type = '民办小学' THEN '小学'
  WHEN school_type = '民办初中' THEN '初中'
  WHEN school_type = '民办九年一贯制' THEN '小学,初中'
  WHEN school_type IN ('民办十二年一贯制', '民办十二年制') THEN '小学,初中,高中'
  WHEN school_type IN ('民办完中', '民办完全中学') THEN '初中,高中'
  WHEN school_type = '国际课程班' THEN '高中'
  WHEN school_type = '外籍人员子女学校' THEN '小学,初中,高中'
  WHEN school_type = '民办国际高中'
       AND grades IN ('1-12年级', '幼儿园至12年级', '幼儿园托班至12年级',
                      '幼儿园小班至12年级', '托班至12年级')
    THEN '小学,初中,高中'
  WHEN school_type = '民办国际高中' AND grades = '6-12年级' THEN '初中,高中'
  WHEN school_type = '民办国际高中'
       AND grades IN ('9-12年级', '10-12年级', '高中')
    THEN '高中'
  ELSE grades
END;

SELECT ROW_COUNT() AS matched_rows;
COMMIT;

SELECT grades, COUNT(*) AS school_count
FROM school
GROUP BY grades
ORDER BY grades;


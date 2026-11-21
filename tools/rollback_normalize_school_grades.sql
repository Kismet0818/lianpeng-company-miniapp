-- 回滚本次 grades 规范化。要求备份表存在且仍有全部 152 条记录。
START TRANSACTION;

UPDATE school s
JOIN school_grades_backup_20260908 b ON b.id = s.id
SET s.grades = b.grades;

SELECT ROW_COUNT() AS restored_rows;
COMMIT;

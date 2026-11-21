-- 修复 admission_content_article 与后端实体/查询条件不一致的问题。
-- 可重复执行：字段已存在时不会再次添加。
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'admission_content_article'
      AND column_name = 'status'
);

SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE admission_content_article ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT ''启用状态：1启用，0停用'' AFTER sort_order',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

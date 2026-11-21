-- 为全部学校信息表增加官网、作息时间、教学特色、占地面积字段。
-- 可重复执行；已有字段不会被覆盖。占地面积统一以平方米存储。

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'),
            '` ', p_column_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('school', 'official_website', 'VARCHAR(500) NULL COMMENT ''学校官网''');
CALL add_column_if_missing('school', 'schedule', 'TEXT NULL COMMENT ''作息时间''');
CALL add_column_if_missing('school', 'teaching_features', 'TEXT NULL COMMENT ''教学特色''');
CALL add_column_if_missing('school', 'campus_area', 'DECIMAL(12,2) NULL COMMENT ''占地面积（平方米）''');

CALL add_column_if_missing('score_query_tool_school', 'official_website', 'VARCHAR(500) NULL COMMENT ''学校官网''');
CALL add_column_if_missing('score_query_tool_school', 'schedule', 'TEXT NULL COMMENT ''作息时间''');
CALL add_column_if_missing('score_query_tool_school', 'teaching_features', 'TEXT NULL COMMENT ''教学特色''');
CALL add_column_if_missing('score_query_tool_school', 'campus_area', 'DECIMAL(12,2) NULL COMMENT ''占地面积（平方米）''');

CALL add_column_if_missing('junior_high_school', 'official_website', 'VARCHAR(500) NULL COMMENT ''学校官网''');
CALL add_column_if_missing('junior_high_school', 'schedule', 'TEXT NULL COMMENT ''作息时间''');
CALL add_column_if_missing('junior_high_school', 'teaching_features', 'TEXT NULL COMMENT ''教学特色''');
CALL add_column_if_missing('junior_high_school', 'campus_area', 'DECIMAL(12,2) NULL COMMENT ''占地面积（平方米）''');

CALL add_column_if_missing('general_high_school', 'official_website', 'VARCHAR(500) NULL COMMENT ''学校官网''');
CALL add_column_if_missing('general_high_school', 'schedule', 'TEXT NULL COMMENT ''作息时间''');
CALL add_column_if_missing('general_high_school', 'teaching_features', 'TEXT NULL COMMENT ''教学特色''');
CALL add_column_if_missing('general_high_school', 'campus_area', 'DECIMAL(12,2) NULL COMMENT ''占地面积（平方米）''');

DROP PROCEDURE add_column_if_missing;

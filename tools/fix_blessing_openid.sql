-- 修复线上 blessing 表的云数据库兼容字段。
-- 后端现在会同步写入 openid 和 _openid；此处将历史表结构调整为允许旧数据为空。
SET @column_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'blessing' AND COLUMN_NAME = '_openid'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE `blessing` ADD COLUMN `_openid` varchar(256) NULL COMMENT ''云数据库兼容字段'' AFTER `openid`',
    'ALTER TABLE `blessing` MODIFY COLUMN `_openid` varchar(256) NULL COMMENT ''云数据库兼容字段'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

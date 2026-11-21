-- Local compatibility migration for the cloud-only `_openid` column mapped by UserFavorite.
-- Safe to run repeatedly. Production already has this field and does not need this migration.

SET @column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_favorite'
    AND column_name = '_openid'
);

SET @add_column_sql := IF(
  @column_exists = 0,
  'ALTER TABLE `user_favorite` ADD COLUMN `_openid` varchar(256) NULL COMMENT ''云数据库权限兼容字段'' AFTER `school_name`',
  'SELECT 1'
);
PREPARE add_column_stmt FROM @add_column_sql;
EXECUTE add_column_stmt;
DEALLOCATE PREPARE add_column_stmt;

UPDATE `user_favorite` f
LEFT JOIN `user` u ON u.id = f.user_id
SET f.`_openid` = COALESCE(NULLIF(f.`_openid`, ''), u.openid, '')
WHERE f.`_openid` IS NULL OR f.`_openid` = '';

ALTER TABLE `user_favorite`
  MODIFY COLUMN `_openid` varchar(256) NOT NULL DEFAULT '' COMMENT '云数据库权限兼容字段';

ALTER TABLE `user`
    ADD COLUMN `unionid` varchar(100) NULL COMMENT '微信开放平台统一标识' AFTER `openid`,
    ADD COLUMN `last_login_time` datetime NULL COMMENT '最近登录时间' AFTER `avatar`,
    ADD COLUMN `last_active_time` datetime NULL COMMENT '最近活跃时间' AFTER `last_login_time`,
    ADD COLUMN `login_count` int NOT NULL DEFAULT 0 COMMENT '累计登录次数' AFTER `last_active_time`,
    ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '1正常 0停用' AFTER `login_count`,
    ADD COLUMN `remark` varchar(500) NULL COMMENT '管理员备注' AFTER `status`,
    ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `remark`,
    ADD UNIQUE KEY `uk_unionid` (`unionid`),
    ADD KEY `idx_user_last_active` (`last_active_time`),
    ADD KEY `idx_user_status` (`status`);

UPDATE `user`
SET `last_login_time` = COALESCE(`last_login_time`, `create_time`),
    `last_active_time` = COALESCE(`last_active_time`, `create_time`),
    `login_count` = CASE WHEN `login_count` = 0 THEN 1 ELSE `login_count` END
WHERE `last_login_time` IS NULL OR `last_active_time` IS NULL OR `login_count` = 0;

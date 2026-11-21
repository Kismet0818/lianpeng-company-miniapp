CREATE TABLE IF NOT EXISTS `school_reaction` (
  `id` int NOT NULL AUTO_INCREMENT,
  `school_source` varchar(20) NOT NULL DEFAULT 'international' COMMENT 'international/score/junior/general/primary',
  `school_id` int NOT NULL,
  `user_id` int NOT NULL,
  `reaction_type` tinyint NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_reaction_user` (`school_source`,`school_id`,`user_id`),
  KEY `idx_school_reaction_school_type` (`school_source`,`school_id`,`reaction_type`),
  CONSTRAINT `chk_school_reaction_type` CHECK (`reaction_type` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='School intro five-level reactions';

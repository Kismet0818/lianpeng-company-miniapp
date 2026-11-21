CREATE TABLE `user_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `school_source` varchar(20) NOT NULL,
  `school_id` int NOT NULL,
  `school_name` varchar(150) NOT NULL,
  `event_type` varchar(30) NOT NULL DEFAULT 'school_view',
  `duration_seconds` int NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_user_time` (`user_id`,`create_time`),
  KEY `idx_activity_school` (`school_source`,`school_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户行为记录';

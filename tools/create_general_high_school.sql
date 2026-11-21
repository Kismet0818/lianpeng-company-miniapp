CREATE TABLE IF NOT EXISTS `general_high_school` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '学校全名',
  `short_name` varchar(100) DEFAULT NULL COMMENT '学校简称',
  `background_image_url` varchar(500) DEFAULT NULL COMMENT '学校背景图OSS完整URL',
  `district` varchar(50) NOT NULL COMMENT '所属区',
  `tuition_min` decimal(10,2) DEFAULT NULL COMMENT '最低学费（万元）',
  `tuition_max` decimal(10,2) DEFAULT NULL COMMENT '最高学费（万元）',
  `boarding_info` varchar(100) DEFAULT NULL COMMENT '寄宿情况',
  `school_type` varchar(20) NOT NULL COMMENT '学校性质：公办/民办',
  `address` varchar(255) DEFAULT NULL COMMENT '学校地址',
  `description` text COMMENT '学校简介',
  `official_website` varchar(500) DEFAULT NULL COMMENT '学校官网',
  `schedule` text COMMENT '作息时间',
  `teaching_features` text COMMENT '教学特色',
  `campus_area` decimal(12,2) DEFAULT NULL COMMENT '占地面积（平方米）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_district` (`name`, `district`),
  KEY `idx_district` (`district`),
  KEY `idx_school_type` (`school_type`),
  CONSTRAINT `chk_general_high_school_type`
    CHECK (`school_type` IN ('公办', '民办')),
  CONSTRAINT `chk_general_high_school_tuition`
    CHECK (`tuition_min` IS NULL OR `tuition_max` IS NULL OR `tuition_min` <= `tuition_max`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='普通高中学校信息表（单表含背景图）';

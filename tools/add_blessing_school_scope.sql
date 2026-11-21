ALTER TABLE `blessing`
  ADD COLUMN `school_source` varchar(20) DEFAULT NULL COMMENT '学校来源：international/score/junior/general/primary' AFTER `avatar`,
  ADD COLUMN `school_id` int DEFAULT NULL COMMENT '对应来源表的学校ID' AFTER `school_source`,
  ADD COLUMN `school_name` varchar(150) DEFAULT NULL COMMENT '发布时学校名称快照' AFTER `school_id`,
  ADD KEY `idx_blessing_school_time` (`school_source`,`school_id`,`status`,`create_time`);

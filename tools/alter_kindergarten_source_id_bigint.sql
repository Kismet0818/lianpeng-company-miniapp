ALTER TABLE `kindergarten`
  MODIFY COLUMN `source_school_id` BIGINT NOT NULL COMMENT '来源平台幼儿园ID，用于幂等更新和去重';

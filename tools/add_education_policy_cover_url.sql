ALTER TABLE `education_policy`
  ADD COLUMN `cover_image_url` varchar(1000) DEFAULT NULL COMMENT '政策封面图片OSS地址' AFTER `content`;

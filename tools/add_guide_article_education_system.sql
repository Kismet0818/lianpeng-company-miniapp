ALTER TABLE `guide_article`
  ADD COLUMN `education_system` varchar(20) NULL COMMENT '升学体系：external体制外，internal体制内' AFTER `category`;

UPDATE `guide_article`
SET `education_system` = CASE
  WHEN `id` BETWEEN 1 AND 69 THEN 'external'
  ELSE 'internal'
END;

ALTER TABLE `guide_article`
  MODIFY COLUMN `education_system` varchar(20) NOT NULL COMMENT '升学体系：external体制外，internal体制内',
  ADD KEY `idx_guide_system_category_path` (`education_system`, `category`, `path_name`);

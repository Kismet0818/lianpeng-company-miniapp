-- 首页四类资料启用状态：1 启用，0 停用；历史数据默认启用。
ALTER TABLE `duikou_material`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1启用，0停用';

ALTER TABLE `admission_content_article`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1启用，0停用';

ALTER TABLE `parent_qa`
    MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1启用，0停用';

ALTER TABLE `lottery_guide`
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1启用，0停用';

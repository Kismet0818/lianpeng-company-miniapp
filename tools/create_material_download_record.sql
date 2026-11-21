CREATE TABLE IF NOT EXISTS material_download_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL COMMENT '下载用户ID',
    _openid VARCHAR(256) NOT NULL COMMENT '下载用户openid',
    resource_id BIGINT NOT NULL COMMENT '资料ID',
    source_type VARCHAR(32) NOT NULL DEFAULT 'SCHOOL_MATERIAL' COMMENT '来源类型',
    source_attachment_id BIGINT DEFAULT NULL COMMENT '升学路径附件ID',
    resource_code VARCHAR(64) NOT NULL COMMENT '资料业务编号快照',
    resource_title VARCHAR(255) NOT NULL COMMENT '资料标题快照',
    file_name VARCHAR(255) NOT NULL COMMENT '下载文件名快照',
    file_url VARCHAR(1000) DEFAULT NULL COMMENT '文件地址快照',
    download_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_material_download_time (download_time),
    KEY idx_material_download_user_time (user_id, download_time),
    KEY idx_material_download_resource_time (resource_id, download_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户资料下载记录';

ALTER TABLE material_download_record
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32) NOT NULL DEFAULT 'SCHOOL_MATERIAL' AFTER resource_id,
    ADD COLUMN IF NOT EXISTS source_attachment_id BIGINT NULL AFTER source_type,
    ADD COLUMN IF NOT EXISTS file_url VARCHAR(1000) NULL AFTER file_name;

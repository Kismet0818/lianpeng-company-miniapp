ALTER TABLE primary_school
    ADD COLUMN tier varchar(10) DEFAULT NULL COMMENT '学校梯队：一梯队/二梯队；不在名单中为NULL'
    AFTER school_type,
    ADD KEY idx_primary_school_tier (tier);

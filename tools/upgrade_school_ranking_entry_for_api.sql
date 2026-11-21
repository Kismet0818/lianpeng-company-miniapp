ALTER TABLE school_ranking_entry
    ADD COLUMN school_source VARCHAR(30) NOT NULL DEFAULT 'international' AFTER school_id,
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 AFTER source_url,
    ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 AFTER status,
    ADD KEY idx_ranking_public (ranking_year, status, is_deleted, rank_position),
    ADD CONSTRAINT chk_ranking_school_source CHECK (school_source = 'international');

UPDATE school_ranking_entry
SET school_source = 'international', status = 1, is_deleted = 0;

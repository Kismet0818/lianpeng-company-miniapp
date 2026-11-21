-- 收藏支持多类学校。仅用于本地 lpsx 验收；云库迁移必须另行获得用户明确同意。
-- 旧收藏均来自 school 表，因此默认标记为 international。
ALTER TABLE user_favorite
  ADD COLUMN school_source varchar(20) NOT NULL DEFAULT 'international'
    COMMENT '学校来源：international/score/junior/general/primary' AFTER school_id;

ALTER TABLE user_favorite
  DROP INDEX uk_user_school,
  ADD UNIQUE KEY uk_user_source_school (user_id, school_source, school_id),
  ADD KEY idx_favorite_school (school_source, school_id);


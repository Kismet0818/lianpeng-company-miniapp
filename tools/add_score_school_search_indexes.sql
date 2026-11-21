-- 仅在本地 lpsx 验收后执行；不得默认连接云数据库。
-- 普通 BTREE 索引用于全名、简称精确/前缀检索；包含检索在当前小数据量下保留兼容。
ALTER TABLE score_query_tool_school
    ADD INDEX idx_score_school_name (name),
    ADD INDEX idx_score_school_short_name (short_name);

ALTER TABLE junior_high_school
    ADD INDEX idx_junior_school_short_name (short_name);

-- 回滚：
-- ALTER TABLE score_query_tool_school DROP INDEX idx_score_school_name, DROP INDEX idx_score_school_short_name;
-- ALTER TABLE junior_high_school DROP INDEX idx_junior_school_short_name;

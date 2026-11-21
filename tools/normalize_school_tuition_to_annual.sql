-- 学校学费统一为“万元/年”
-- 执行日期：2026-09-08
-- 注意：本脚本依赖执行前创建的 *_before_tuition_annual_20260908 备份表。

-- 1. 一般高中：历史数据混有“万元/学期”和“元/学期”。
--    0/0 代表未知，统一改为 NULL。
UPDATE general_high_school
SET tuition_min = CASE
        WHEN tuition_min IS NULL OR tuition_min = 0 THEN NULL
        WHEN tuition_min >= 100 THEN ROUND(tuition_min * 2 / 10000, 2)
        ELSE ROUND(tuition_min * 2, 2)
    END,
    tuition_max = CASE
        WHEN tuition_max IS NULL OR tuition_max = 0 THEN NULL
        WHEN tuition_max >= 100 THEN ROUND(tuition_max * 2 / 10000, 2)
        ELSE ROUND(tuition_max * 2, 2)
    END;

-- 2. 初中：原值按“万元/学期”录入；0/0 代表未知。
UPDATE junior_high_school
SET tuition_min = CASE
        WHEN tuition_min IS NULL OR tuition_min = 0 THEN NULL
        ELSE ROUND(tuition_min * 2, 2)
    END,
    tuition_max = CASE
        WHEN tuition_max IS NULL OR tuition_max = 0 THEN NULL
        ELSE ROUND(tuition_max * 2, 2)
    END;

-- 3. 分数查询高中：0.09/0.12/0.15/0.20 是未年化的公办每学期学费；
--    0.24/0.30 等已经是年费。两所民办记录 2.05/2.90 仍是每学期值。
UPDATE score_query_tool_school
SET tuition_min = CASE
        WHEN tuition_min IS NULL OR tuition_min = 0 THEN NULL
        WHEN tuition_min IN (0.09, 0.12, 0.15, 0.20, 2.05, 2.90) THEN ROUND(tuition_min * 2, 2)
        ELSE tuition_min
    END,
    tuition_max = CASE
        WHEN tuition_max IS NULL OR tuition_max = 0 THEN NULL
        WHEN tuition_max IN (0.09, 0.12, 0.15, 0.20, 2.05, 2.90) THEN ROUND(tuition_max * 2, 2)
        ELSE tuition_max
    END;

-- 4. 国际学校主表原本已经按“万元/年”存储，仅清理可能存在的 0 值。
UPDATE school
SET tuition_min = NULLIF(tuition_min, 0),
    tuition_max = NULLIF(tuition_max, 0);

-- 5. 把数据库字段定义中的单位也固定下来，防止后续导入再次混用。
ALTER TABLE school
    MODIFY tuition_min DECIMAL(10,2) NULL COMMENT '最低学费（万元/年）',
    MODIFY tuition_max DECIMAL(10,2) NULL COMMENT '最高学费（万元/年）';

ALTER TABLE general_high_school
    MODIFY tuition_min DECIMAL(10,2) NULL COMMENT '最低学费（万元/年）',
    MODIFY tuition_max DECIMAL(10,2) NULL COMMENT '最高学费（万元/年）';

ALTER TABLE score_query_tool_school
    MODIFY tuition_min DECIMAL(10,2) NULL COMMENT '最低学费（万元/年）',
    MODIFY tuition_max DECIMAL(10,2) NULL COMMENT '最高学费（万元/年）';

ALTER TABLE junior_high_school
    MODIFY tuition_min DECIMAL(10,2) NULL COMMENT '最低学费（万元/年）',
    MODIFY tuition_max DECIMAL(10,2) NULL COMMENT '最高学费（万元/年）';

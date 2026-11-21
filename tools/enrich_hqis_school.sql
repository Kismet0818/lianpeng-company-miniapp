-- HQIS 后续补充：2026 学费、寄宿情况及 OSS 校徽。
-- tuition_min / tuition_max 单位遵循 school 表现有约定：万元/年。
START TRANSACTION;

UPDATE school
SET tuition_min = 9.846,
    tuition_max = 26.00,
    boarding_info = '无',
    logo_url = 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/logo/2b98a63ae8ee45b9ae4652e8e43c9346.png'
WHERE id = 208
  AND name = '上海虹桥国际外籍人员子女学校';

SELECT ROW_COUNT() AS updated_rows;
COMMIT;

SELECT id, name, tuition_min, tuition_max, boarding_info, logo_url
FROM school
WHERE id = 208;

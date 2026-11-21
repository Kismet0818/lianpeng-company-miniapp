-- Merge duplicate Xuhui WFLMS records in the local lpsx database.
-- Keep school.id = 102 because its gallery objects already use the /102/ OSS path.

START TRANSACTION;

-- Merge the complementary junior/high-school information into the retained row.
UPDATE school
SET name = '上海徐汇世外学校',
    curriculum = '国家课程、双语课程、国内融合课程、IBDP、A-Level',
    tuition_min = 3.10,
    tuition_max = 9.00,
    feature_tags = '世外系,IB课程,藤校收割机,均衡发展,徐汇',
    student_count = 2211,
    grades = '初中,高中',
    description = CONCAT(
        (SELECT description FROM (SELECT description FROM school WHERE id = 186) AS source_school),
        '\n\n',
        description
    ),
    boarding_info = '无',
    school_type = '民办完全中学',
    address = '本部校区：虹漕南路602号；国际校区：百花街400号'
WHERE id = 102
  AND name = '上海市世外中学'
  AND EXISTS (SELECT 1 FROM (SELECT id FROM school WHERE id = 186 AND name = '世界外国语中学') AS duplicate_school);

-- Remove gallery duplicates, then migrate any non-duplicate images.
DELETE duplicate_image
FROM school_image AS duplicate_image
JOIN school_image AS retained_image
  ON retained_image.school_id = 102
 AND duplicate_image.school_id = 186
 AND retained_image.image_url = duplicate_image.image_url
 AND retained_image.image_type = duplicate_image.image_type;

UPDATE school_image
SET school_id = 102
WHERE school_id = 186;

-- Avoid violating the (user_id, school_id) unique key if both entries were favorited.
DELETE duplicate_favorite
FROM user_favorite AS duplicate_favorite
JOIN user_favorite AS retained_favorite
  ON retained_favorite.user_id = duplicate_favorite.user_id
 AND retained_favorite.school_id = 102
WHERE duplicate_favorite.school_id = 186;

UPDATE user_favorite
SET school_id = 102,
    school_name = '上海徐汇世外学校'
WHERE school_id = 186;

UPDATE user_favorite
SET school_name = '上海徐汇世外学校'
WHERE school_id = 102;

UPDATE admission_info
SET school_id = 102,
    school_name = '上海徐汇世外学校'
WHERE school_id IN (102, 186)
   OR school_name IN ('世界外国语中学', '上海市世外中学');

UPDATE open_day
SET school_id = 102,
    school_name = '上海徐汇世外学校'
WHERE school_id IN (102, 186)
   OR school_name IN ('世界外国语中学', '上海市世外中学');

-- These tables store the school only as text, without a foreign key.
UPDATE customer_lead
SET target_school = '上海徐汇世外学校'
WHERE target_school IN ('世界外国语中学', '上海市世外中学');

UPDATE score_query_tool
SET high_school_name = '上海徐汇世外学校'
WHERE high_school_name IN ('世界外国语中学', '上海市世外中学');

UPDATE score_query_tool
SET target_school_name = '上海徐汇世外学校'
WHERE target_school_name IN ('世界外国语中学', '上海市世外中学');

UPDATE score_volunteer_1_15
SET school_name = '上海徐汇世外学校'
WHERE school_name IN ('世界外国语中学', '上海市世外中学');

DELETE FROM school
WHERE id = 186
  AND name = '世界外国语中学';

COMMIT;

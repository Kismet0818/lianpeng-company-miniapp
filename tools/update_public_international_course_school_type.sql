-- 本地 lpsx：把《上海公办国际课程班信息.docx》中的 10 所学校统一标记为“国际课程班”。
-- 仅在 id、名称和原 school_type 同时符合预期时更新，避免误改其他记录。
START TRANSACTION;

UPDATE school
SET school_type = '国际课程班'
WHERE school_type = '公办国际高中'
  AND (id, name) IN (
    (171, '华东师范大学附属第二中学'),
    (172, '复旦大学附属中学'),
    (173, '上海交通大学附属中学'),
    (174, '格致中学'),
    (175, '大同中学'),
    (176, '卢湾高级中学'),
    (177, '市西中学'),
    (178, '建平中学'),
    (179, '上外附属浦东外国语学校'),
    (180, '曹杨第二中学')
  );

SELECT ROW_COUNT() AS updated_rows;

COMMIT;

SELECT id, name, school_type
FROM school
WHERE id BETWEEN 171 AND 180
ORDER BY id;

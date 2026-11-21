-- 如需撤销本次本地数据修改，运行本文件。
START TRANSACTION;

UPDATE school
SET school_type = '公办国际高中'
WHERE school_type = '国际课程班'
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

SELECT ROW_COUNT() AS rolled_back_rows;

COMMIT;

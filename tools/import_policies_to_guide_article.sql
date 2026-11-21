START TRANSACTION;

CREATE TEMPORARY TABLE `tmp_policy_guide_mapping` (
  `source_id` bigint NOT NULL PRIMARY KEY,
  `path_name` varchar(50) NOT NULL,
  `sort_order` int NOT NULL
);

INSERT INTO `tmp_policy_guide_mapping` (`source_id`,`path_name`,`sort_order`) VALUES
  (15,'升学路径',7),
  (17,'升学路径',8),
  (4, '升学路径',9),
  (3, '升学路径',10),
  (22,'规划须知',5),
  (21,'规划须知',6),
  (19,'规划须知',7),
  (18,'规划须知',8),
  (16,'规划须知',9);

INSERT INTO `guide_article`
  (`category`,`path_name`,`title`,`content`,`summary`,`thumbnail_url`,`sort_order`,`create_time`)
SELECT
  '升学规划',
  m.`path_name`,
  p.`name`,
  CONCAT(
    CASE WHEN p.`summary` IS NULL OR TRIM(p.`summary`)='' THEN ''
         ELSE CONCAT('<p><strong>政策摘要：</strong>',
              REPLACE(REPLACE(REPLACE(p.`summary`,'&','&amp;'),'<','&lt;'),'>','&gt;'),'</p>') END,
    CASE WHEN p.`content` IS NULL OR TRIM(p.`content`)='' OR p.`content`=p.`summary` THEN ''
         ELSE CONCAT('<p>',REPLACE(REPLACE(REPLACE(p.`content`,'&','&amp;'),'<','&lt;'),'>','&gt;'),'</p>') END,
    CASE WHEN p.`public_date` IS NULL THEN ''
         ELSE CONCAT('<p><strong>发布日期：</strong>',DATE_FORMAT(p.`public_date`,'%Y-%m-%d'),'</p>') END,
    COALESCE((
      SELECT CONCAT('<p><strong>政策附件：</strong><a href="',a.`file_url`,'">',
                    REPLACE(REPLACE(REPLACE(a.`file_name`,'&','&amp;'),'<','&lt;'),'>','&gt;'),
                    '</a></p><p>',a.`file_url`,'</p>')
      FROM `education_policy_attachment` a
      WHERE a.`policy_id`=p.`id` AND a.`media_type`='pdf' AND a.`is_deleted`=0
            AND a.`file_url` IS NOT NULL AND TRIM(a.`file_url`)<>' '
      ORDER BY a.`sort_order`,a.`id` LIMIT 1
    ),'')
  ),
  p.`summary`,
  p.`cover_image_url`,
  m.`sort_order`,
  COALESCE(p.`public_date`,p.`create_time`,CURRENT_TIMESTAMP)
FROM `education_policy` p
JOIN `tmp_policy_guide_mapping` m ON m.`source_id`=p.`source_id`
WHERE p.`source_platform`='anroader'
  AND NOT EXISTS (
    SELECT 1 FROM `guide_article` g
    WHERE g.`category`='升学规划' AND g.`title`=p.`name`
  );

UPDATE `guide_article` g
JOIN `education_policy` p ON p.`name`=g.`title` AND p.`source_platform`='anroader'
JOIN `tmp_policy_guide_mapping` m ON m.`source_id`=p.`source_id`
SET
  g.`path_name`=m.`path_name`,
  g.`summary`=p.`summary`,
  g.`thumbnail_url`=p.`cover_image_url`,
  g.`sort_order`=m.`sort_order`,
  g.`create_time`=COALESCE(p.`public_date`,p.`create_time`,g.`create_time`),
  g.`content`=CONCAT(
    CASE WHEN p.`summary` IS NULL OR TRIM(p.`summary`)='' THEN ''
         ELSE CONCAT('<p><strong>政策摘要：</strong>',
              REPLACE(REPLACE(REPLACE(p.`summary`,'&','&amp;'),'<','&lt;'),'>','&gt;'),'</p>') END,
    CASE WHEN p.`content` IS NULL OR TRIM(p.`content`)='' OR p.`content`=p.`summary` THEN ''
         ELSE CONCAT('<p>',REPLACE(REPLACE(REPLACE(p.`content`,'&','&amp;'),'<','&lt;'),'>','&gt;'),'</p>') END,
    CASE WHEN p.`public_date` IS NULL THEN ''
         ELSE CONCAT('<p><strong>发布日期：</strong>',DATE_FORMAT(p.`public_date`,'%Y-%m-%d'),'</p>') END,
    COALESCE((
      SELECT CONCAT('<p><strong>政策附件：</strong><a href="',a.`file_url`,'">',
                    REPLACE(REPLACE(REPLACE(a.`file_name`,'&','&amp;'),'<','&lt;'),'>','&gt;'),
                    '</a></p><p>',a.`file_url`,'</p>')
      FROM `education_policy_attachment` a
      WHERE a.`policy_id`=p.`id` AND a.`media_type`='pdf' AND a.`is_deleted`=0
            AND a.`file_url` IS NOT NULL AND TRIM(a.`file_url`)<>' '
      ORDER BY a.`sort_order`,a.`id` LIMIT 1
    ),'')
  )
WHERE g.`category`='升学规划';

DROP TEMPORARY TABLE `tmp_policy_guide_mapping`;
COMMIT;

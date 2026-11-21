START TRANSACTION;

CREATE TABLE IF NOT EXISTS `school_ranking_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ranking_code` varchar(80) NOT NULL,
  `ranking_name` varchar(255) NOT NULL,
  `ranking_year` smallint NOT NULL,
  `rank_position` int NOT NULL,
  `rank_index` decimal(6,2) DEFAULT NULL,
  `school_id` int NOT NULL,
  `display_name` varchar(255) NOT NULL,
  `city` varchar(50) NOT NULL,
  `source_url` varchar(1000) NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ranking_position` (`ranking_code`,`rank_position`),
  UNIQUE KEY `uk_ranking_school` (`ranking_code`,`school_id`),
  KEY `idx_ranking_school` (`school_id`),
  CONSTRAINT `fk_ranking_school` FOREIGN KEY (`school_id`) REFERENCES `school` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学校排行榜条目';

CREATE TEMPORARY TABLE `tmp_hurun_school` (
  `name` varchar(100) NOT NULL PRIMARY KEY, `city` varchar(50), `district` varchar(50),
  `curriculum` varchar(255), `feature_tags` varchar(255), `grades` varchar(50),
  `description` text, `boarding_info` varchar(100), `teaching_mode` varchar(255),
  `founding_time` varchar(50), `school_type` varchar(50), `address` varchar(255),
  `official_website` varchar(500)
);

INSERT INTO `tmp_hurun_school` VALUES
('深圳国际交流书院','深圳','福田区','IGCSE,A-Level,AP','胡润2025全国第2,全英文教学','高中','2003年创办的国际高中，前两年学习IGCSE课程，高年级可选择A-Level或AP方向。','有','除中文课外以英文授课','2003','民办国际高中','深圳市福田区安托山六路3号','https://www.scie.com.cn/'),
('北京市鼎石学校','北京','顺义区','IB课程,双语课程','胡润2025全国第4,中英双语,寄宿教育','小学,初中,高中','十二年一贯制民办双语学校，以中国文化、中英双语和寄宿教育为办学特色，高中实施IBDP课程。','部分寄宿','沉浸式中英双语','2014','民办十二年一贯制','北京市顺义区后沙峪镇安富街11号','https://www.keystoneacademy.cn/'),
('北京师范大学附属实验中学国际部','北京','西城区','AP课程,校本课程','胡润2025全国第6,公办学校国际部','高中','依托北京师范大学附属实验中学办学，课程包含国家必修、双语校本、AP专修、通识专修与升学指导。',NULL,'双语',NULL,'公办学校国际部','北京市西城区二龙路14号','https://www.sdsz.com.cn/'),
('中国常熟世界联合学院','苏州','常熟市','IBDP,FP','胡润2025全国第8,UWC,全寄宿','高中','2015年创立的中国内地第一所UWC，提供一年制FP预备课程和两年制IBDP课程，学生来自近百个国家和地区。','全寄宿','英文为主并设中文课程','2015','民办国际高中','江苏省常熟市昆承湖西路88号','https://www.uwcchina.org/'),
('北京市十一学校国际部','北京','海淀区','A-Level,AP,IBDP','胡润2025全国第9,公办学校国际部','高中','北京市十一学校国际课程项目，开设A-Level、AP和IB三大课程体系，并提供升学指导及多元选修课程。',NULL,'双语及英文授课',NULL,'公办学校国际部','北京市海淀区玉泉路66号','https://www.bnds.cn/course/international/'),
('广东碧桂园学校','佛山','顺德区','国家课程,PYP,MYP,IBDP,IGCSE,AP','胡润2025全国第10,十五年一贯制,寄宿','幼儿园,小学,初中,高中','1994年创办的民办十五年一贯制学校，提供国家课程及中外融通课程，并开设IB、IGCSE和AP等国际课程。','有','双语及英文授课','1994','民办十五年一贯制','广东省佛山市顺德区北滘镇顺德碧桂园南九路','https://bgy.gd.cn/'),
('中国人民大学附属中学国际课程中心','北京','海淀区','A-Level,AP,IBDP','胡润2025全国第11,公办学校国际部','高中','人大附中中外合作办学项目，2004年引入A-Level课程，后续开设AP与IBDP课程。',NULL,'双语及英文授课','2004','公办学校国际部','北京市海淀区中关村大街37号','https://icc.rdfz.cn/'),
('华南师范大学附属中学国际部','广州','天河区','AP课程,融合课程','胡润2025全国第11,公办学校国际部','高中','华南师范大学附属中学国际部，实施中外融合的高中课程，课程包含AP专修、双语校本课程与升学指导。',NULL,'双语及英文授课','2004','公办学校国际部','广州市天河区中山大道西1号华南师大附中奥翔楼','https://gdhfi.com/'),
('北京市第四中学国际部','北京','西城区','AP课程','胡润2025全国第14,公办学校国际部','高中','北京四中国际课程项目，面向国际升学方向提供高中课程与大学申请支持。',NULL,'双语及英文授课',NULL,'公办学校国际部',NULL,'https://www.bhsf.cn/'),
('深圳中学国际部','深圳','罗湖区','AP,A-Level,IGCSE','胡润2025全国第15,公办学校国际部','高中','深圳中学国际课程项目，开设AP、A-Level及IGCSE等课程，培养具有中华底蕴和国际视野的学生。',NULL,'双语及英文授课',NULL,'公办学校国际部','深圳市罗湖区泥岗西路1068号','https://www.shenzhong.net.cn/');

INSERT INTO `school` (`name`,`city`,`district`,`curriculum`,`feature_tags`,`grades`,`description`,`boarding_info`,`teaching_mode`,`founding_time`,`school_type`,`address`,`official_website`,`is_active`)
SELECT t.`name`,t.`city`,t.`district`,t.`curriculum`,t.`feature_tags`,t.`grades`,t.`description`,t.`boarding_info`,t.`teaching_mode`,t.`founding_time`,t.`school_type`,t.`address`,t.`official_website`,1
FROM `tmp_hurun_school` t
WHERE NOT EXISTS (SELECT 1 FROM `school` s WHERE s.`name`=t.`name`);

UPDATE `school` s JOIN `tmp_hurun_school` t ON t.`name`=s.`name`
SET s.`city`=t.`city`,s.`district`=t.`district`,s.`curriculum`=t.`curriculum`,
    s.`feature_tags`=t.`feature_tags`,s.`grades`=t.`grades`,s.`description`=t.`description`,
    s.`boarding_info`=t.`boarding_info`,s.`teaching_mode`=t.`teaching_mode`,
    s.`founding_time`=t.`founding_time`,s.`school_type`=t.`school_type`,s.`address`=t.`address`,
    s.`official_website`=t.`official_website`,s.`is_active`=1;

CREATE TEMPORARY TABLE `tmp_hurun_rank` (`rank_position` int PRIMARY KEY,`rank_index` decimal(6,2),`school_name` varchar(100),`display_name` varchar(255),`city` varchar(50));
INSERT INTO `tmp_hurun_rank` VALUES
(1,100,'上海民办包玉刚实验学校','上海民办包玉刚实验学校','上海'),
(2,97,'深圳国际交流书院','深圳国际交流书院','深圳'),
(3,95,'上海市民办平和学校','上海市民办平和学校','上海'),
(4,92,'北京市鼎石学校','北京市鼎石国际学校','北京'),
(5,90,'上海市世界外国语中学','上海市世界外国语中学','上海'),
(6,88,'北京师范大学附属实验中学国际部','北京师范大学附属实验中学国际部','北京'),
(7,87,'领科教育上海校区','领科教育上海校区','上海'),
(8,85,'中国常熟世界联合学院','UWC常熟世界联合学院','苏州'),
(9,84,'北京市十一学校国际部','北京市十一学校国际部','北京'),
(10,83,'广东碧桂园学校','广东碧桂园学校','佛山'),
(11,82,'中国人民大学附属中学国际课程中心','中国人民大学附属中学国际课程中心','北京'),
(12,82,'华南师范大学附属中学国际部','华南师范大学附属中学国际部','广州'),
(13,82,'WLSA上海学校','WLSA上海学校','上海'),
(14,79,'北京市第四中学国际部','北京市第四中学国际部','北京'),
(15,78,'深圳中学国际部','深圳中学国际部','深圳');

INSERT INTO `school_ranking_entry` (`ranking_code`,`ranking_name`,`ranking_year`,`rank_position`,`rank_index`,`school_id`,`display_name`,`city`,`source_url`)
SELECT 'hurun-international-school-2025','2025胡润百学·中国国际化学校100强',2025,r.`rank_position`,r.`rank_index`,s.`id`,r.`display_name`,r.`city`,
       'https://www.hurun.net/zh-cn/rank/hsrankdetails?pagetype=cschools'
FROM `tmp_hurun_rank` r JOIN `school` s ON s.`name`=r.`school_name`
ON DUPLICATE KEY UPDATE `rank_index`=VALUES(`rank_index`),`school_id`=VALUES(`school_id`),`display_name`=VALUES(`display_name`),`city`=VALUES(`city`),`source_url`=VALUES(`source_url`);

DROP TEMPORARY TABLE `tmp_hurun_rank`;
DROP TEMPORARY TABLE `tmp_hurun_school`;
COMMIT;

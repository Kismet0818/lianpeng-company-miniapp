package com.example.lpsx.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏列表视图对象 — user_favorite + user + school 三表 JOIN 结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteVO {

    /** user_favorite.id */
    private Integer favoriteId;

    /** user.id */
    private Integer userId;

    /** user.nickname */
    private String userNickname;

    /** user.avatar */
    private String userAvatar;

    /** school.id */
    private Integer schoolId;

    /** 学校来源：international/score/junior/general/primary */
    private String schoolSource;

    /** school.name */
    private String schoolName;

    /** school.logo_url */
    private String schoolLogo;

    /** school.district */
    private String district;

    /** school.curriculum */
    private String curriculum;

    /** school.grades */
    private String grades;

    /** user_favorite.create_time */
    private LocalDateTime createTime;
}

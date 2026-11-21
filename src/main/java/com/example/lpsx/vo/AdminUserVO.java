package com.example.lpsx.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminUserVO {
    private Integer id;
    private String openidMasked;
    private String phoneMasked;
    /** 仅管理员接口返回的完整绑定手机号。 */
    private String phone;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
    private LocalDateTime lastLoginTime;
    private LocalDateTime lastActiveTime;
    private Integer loginCount;
    private Integer status;
    private String remark;
    private Long favoriteCount;
    private Long leadCount;
    private Long reactionCount;
    private Long blessingCount;
}

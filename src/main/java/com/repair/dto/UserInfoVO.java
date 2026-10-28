package com.repair.dto;

import com.repair.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对外用户信息视图：绝不含 password 字段。
 */
@Data
@Builder
public class UserInfoVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String role;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    /** 密码重置流转标记：0 未申请 / 1 已申请待批准 / 2 已批准 */
    private Integer resetApproved;

    /** 信誉分 */
    private Integer creditScore;

    /** 师傅平均评分（仅 REPAIRER 使用） */
    private BigDecimal rating;

    private String avatar;

    private LocalDateTime createdAt;

    public static UserInfoVO from(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .resetApproved(user.getResetApproved())
                .creditScore(user.getCreditScore())
                .rating(user.getRating())
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

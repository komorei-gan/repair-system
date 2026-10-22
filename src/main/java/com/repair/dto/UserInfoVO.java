package com.repair.dto;

import com.repair.entity.User;
import lombok.Builder;
import lombok.Data;

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
                .avatar(user.getAvatar())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

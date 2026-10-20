package com.repair.dto;

import com.repair.entity.User;
import lombok.Builder;
import lombok.Data;

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

    private String avatar;

    public static UserInfoVO from(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }
}

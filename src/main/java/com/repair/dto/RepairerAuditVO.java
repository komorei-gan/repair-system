package com.repair.dto;

import com.repair.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 师傅审核列表视图（不含 password）。
 */
@Data
@Builder
public class RepairerAuditVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    /** 申请理由（师傅注册时填写） */
    private String reason;

    /** 审核状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer auditStatus;

    private LocalDateTime createdAt;

    public static RepairerAuditVO from(User user) {
        return RepairerAuditVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .reason(user.getReason())
                .auditStatus(user.getAuditStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

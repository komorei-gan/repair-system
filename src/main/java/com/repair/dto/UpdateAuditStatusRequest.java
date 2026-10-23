package com.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 师傅审核请求（PUT /api/admin/repairers/audit/{id}）。
 */
@Data
public class UpdateAuditStatusRequest {

    /** 审核结果：1 通过 / 2 拒绝（0 待审核不允许由管理端反向置回） */
    @NotNull(message = "请指定审核结果")
    private Integer auditStatus;
}

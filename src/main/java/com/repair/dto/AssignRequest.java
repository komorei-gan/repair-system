package com.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 派单请求（design.md §3.4 PUT /api/admin/orders/{id}/assign）。
 */
@Data
public class AssignRequest {

    /** 被指派的维修师傅，逻辑关联 user.id */
    @NotNull(message = "请选择维修师傅")
    private Long repairerId;
}

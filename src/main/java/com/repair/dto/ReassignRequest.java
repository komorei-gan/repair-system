package com.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 改派请求（design.md §3.4 PUT /api/admin/orders/{id}/reassign）。
 */
@Data
public class ReassignRequest {

    /** 新维修师傅，逻辑关联 user.id（须与现任师傅不同） */
    @NotNull(message = "请选择新维修师傅")
    private Long repairerId;
}

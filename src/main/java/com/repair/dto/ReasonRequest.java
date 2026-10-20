package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 驳回 / 拒单 / 取消请求（必填原因），对应多个接口：
 * - 管理员驳回 PUT /api/admin/orders/{id}/reject
 * - 师傅拒单 PUT /api/repairer/orders/{id}/reject
 * - 报修人取消 PUT /api/orders/{id}/cancel
 * - 管理员取消 PUT /api/admin/orders/{id}/cancel
 */
@Data
public class ReasonRequest {

    @NotBlank(message = "原因不能为空")
    @Size(max = 200, message = "原因不能超过 200 字")
    private String reason;
}

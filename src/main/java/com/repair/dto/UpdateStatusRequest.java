package com.repair.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用/禁用用户请求（PUT /api/admin/users/{id}/status）。
 */
@Data
public class UpdateStatusRequest {

    /** 1 启用 / 0 禁用 */
    @NotNull(message = "请指定状态")
    private Integer status;
}

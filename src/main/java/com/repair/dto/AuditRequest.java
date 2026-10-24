package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员统一审批请求，对应 PUT /api/admin/orders/{id}/audit。
 * type 决定审批分支：REASSIGN 申请改派 / ARBITRATE 恶意报修仲裁。
 */
@Data
public class AuditRequest {

    /** 审批结论：true 同意 / false 驳回 */
    @NotNull(message = "审批结论不能为空")
    private Boolean approve;

    /** 审批类型：REASSIGN 申请改派 / ARBITRATE 恶意报修仲裁 */
    @NotBlank(message = "审批类型不能为空")
    private String type;

    /** 审批备注（可选，写入操作日志） */
    @Size(max = 200, message = "备注不能超过 200 字")
    private String remark;
}

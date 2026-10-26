package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 师傅申请请求（申请改派 / 申请仲裁），对应接口：
 * - 申请改派 PUT /api/repairer/orders/{id}/reassign-request
 * - 申请仲裁 PUT /api/repairer/orders/{id}/arbitration-request
 *
 * 与 {@link ReasonRequest} 的区别：新增选填的证据图片字段 applyImages，
 * 图片由前端上传后传入 URL 的 JSON 数组字符串，写入 repair_order.apply_images。
 */
@Data
public class ApplyRequest {

    /** 申请理由（必填） */
    @NotBlank(message = "申请理由不能为空")
    @Size(max = 200, message = "申请理由不能超过 200 字")
    private String reason;

    /** 证据图片 URL 的 JSON 数组字符串（选填） */
    @Size(max = 1000, message = "图片信息过长")
    private String applyImages;
}

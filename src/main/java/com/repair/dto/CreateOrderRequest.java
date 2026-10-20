package com.repair.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交报修工单请求（design.md §3.2 POST /api/orders）。
 */
@Data
public class CreateOrderRequest {

    /** 故障分类，逻辑关联 category.id */
    @NotNull(message = "故障分类不能为空")
    private Long categoryId;

    @NotBlank(message = "故障标题不能为空")
    @Size(max = 100, message = "故障标题不能超过 100 字")
    private String title;

    @Size(max = 1000, message = "故障描述不能超过 1000 字")
    private String description;

    /** 图片 URL 的 JSON 数组字符串（前端自行上传后传入，本阶段不处理上传） */
    @Size(max = 1000, message = "图片信息过长")
    private String images;

    @Size(max = 200, message = "报修地址不能超过 200 字")
    private String address;

    /** 联系电话；填写时须为大陆手机号格式 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
}

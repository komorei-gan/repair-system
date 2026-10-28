package com.repair.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交完成请求（design.md §3.3 PUT /api/repairer/orders/{id}/complete）。
 */
@Data
public class CompleteRequest {

    /** 维修结果描述（选填，最多 500 字） */
    @Size(max = 500, message = "维修结果不能超过 500 字")
    private String repairResult;
}

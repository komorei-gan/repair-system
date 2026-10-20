package com.repair.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 确认完成并评价请求（design.md §3.2 POST /api/orders/{id}/confirm）。
 */
@Data
public class ConfirmRequest {

    /** 评分 1~5 */
    @NotNull(message = "请评分")
    @DecimalMin(value = "1.0", message = "评分最低 1 分")
    @DecimalMax(value = "5.0", message = "评分最高 5 分")
    private BigDecimal rating;

    @Size(max = 200, message = "评价内容不能超过 200 字")
    private String ratingComment;
}

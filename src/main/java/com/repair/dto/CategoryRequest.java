package com.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/编辑分类请求（POST /api/admin/categories、PUT /api/admin/categories/{id}）。
 */
@Data
public class CategoryRequest {

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 字")
    private String name;

    /** 排序值（数字，越大越靠后） */
    private Integer sort;
}

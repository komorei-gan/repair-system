package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.entity.Category;
import com.repair.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 故障分类接口（design.md §3.2 GET /api/categories）。
 * 任何已登录用户可访问（提交报修前选择分类）。
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 启用的分类列表 */
    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(categoryService.listEnabled());
    }
}

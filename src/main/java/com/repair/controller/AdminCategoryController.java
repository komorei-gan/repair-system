package com.repair.controller;

import com.repair.common.result.Result;
import com.repair.dto.CategoryRequest;
import com.repair.entity.Category;
import com.repair.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端故障分类接口（design.md §3.4）。
 * - GET    /api/admin/categories        ：全部分类（含停用）
 * - POST   /api/admin/categories        ：新增分类
 * - PUT    /api/admin/categories/{id}   ：编辑分类
 * - DELETE /api/admin/categories/{id}   ：删除分类（存在未完成工单则拒绝）
 */
@Tag(name = "管理端分类模块")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /** 全部分类列表（含停用，供管理端表格展示） */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    /** 新增分类 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody CategoryRequest request) {
        categoryService.create(request.getName(), request.getSort());
        return Result.success();
    }

    /** 编辑分类 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        categoryService.update(id, request.getName(), request.getSort());
        return Result.success();
    }

    /** 删除分类 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}

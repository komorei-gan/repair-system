package com.repair.service;

import com.repair.entity.Category;

import java.util.List;

/**
 * 故障分类服务。
 */
public interface CategoryService {

    /** 启用的分类列表（按 sort 升序），供报修端选择 */
    List<Category> listEnabled();

    /** 全部分类列表（含停用，按 sort 升序），供管理端展示 */
    List<Category> listAll();

    /** 按 id 取启用的分类，不存在或已停用抛 CATEGORY_NOT_FOUND */
    Category getEnabledById(Long id);

    /** 新增分类 */
    void create(String name, Integer sort);

    /** 更新分类（名称 + 排序） */
    void update(Long id, String name, Integer sort);

    /** 删除分类：若存在未完成工单引用则拒绝 */
    void delete(Long id);
}

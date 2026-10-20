package com.repair.service;

import com.repair.entity.Category;

import java.util.List;

/**
 * 故障分类服务。
 */
public interface CategoryService {

    /** 启用的分类列表（按 sort 升序），供报修端选择 */
    List<Category> listEnabled();

    /** 按 id 取启用的分类，不存在或已停用抛 CATEGORY_NOT_FOUND */
    Category getEnabledById(Long id);
}

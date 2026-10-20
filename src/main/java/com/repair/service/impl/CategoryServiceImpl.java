package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.entity.Category;
import com.repair.mapper.CategoryMapper;
import com.repair.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
    }

    @Override
    public Category getEnabledById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }
}

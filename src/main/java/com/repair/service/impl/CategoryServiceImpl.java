package com.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.repair.common.enums.OrderStatus;
import com.repair.common.exception.BusinessException;
import com.repair.common.result.ErrorCode;
import com.repair.entity.Category;
import com.repair.entity.RepairOrder;
import com.repair.mapper.CategoryMapper;
import com.repair.mapper.RepairOrderMapper;
import com.repair.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    private final RepairOrderMapper repairOrderMapper;

    @Override
    public List<Category> listEnabled() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .eq(Category::getStatus, 1)
                .orderByAsc(Category::getSort));
    }

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    @Override
    public Category getEnabledById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(String name, Integer sort) {
        validateName(name);
        Category category = new Category();
        category.setName(name.trim());
        category.setSort(sort == null ? 0 : sort);
        category.setStatus(1);
        categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, String name, Integer sort) {
        validateName(name);
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }
        categoryMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Category>()
                .eq("id", id)
                .set("name", name.trim())
                .set("sort", sort == null ? 0 : sort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND, "分类不存在");
        }
        // 安全校验：该分类下存在未完成工单（非终态）则禁止删除，防止数据脏乱
        long activeCount = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getCategoryId, id)
                .notIn(RepairOrder::getStatus,
                        OrderStatus.COMPLETED, OrderStatus.CONFIRMED,
                        OrderStatus.CANCELLED, OrderStatus.REJECTED));
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该分类下存在待处理工单，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    /** 校验分类名称非空且长度合理 */
    private void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能为空");
        }
        if (name.trim().length() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分类名称不能超过 50 字");
        }
    }
}

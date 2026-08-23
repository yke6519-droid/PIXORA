package com.example.picturebackend.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.CategoryMapper;
import com.example.picturebackend.Service.CategoryService;
import com.example.picturebackend.domain.po.Category;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/** 公共图库主题管理服务实现。 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>
        implements CategoryService {

    @Override
    public List<Category> listAvailableCategories() {
        return this.list(new QueryWrapper<Category>()
                .orderByAsc("sortOrder")
                .orderByAsc("id"));
    }

    @Override
    public Category getRequired(Long categoryId) {
        ThrowExceptionUtils.throwIF(
                categoryId == null,
                ErrorCode.PARAMS_ERROR,
                "主题 id 不能为空");
        Category category = this.getById(categoryId);
        ThrowExceptionUtils.throwIF(
                category == null,
                ErrorCode.NOT_FOUND_ERROR,
                "主题不存在");
        return category;
    }

    @Override
    public Category createSystemCategory(String categoryName) {
        String normalizedName = StrUtil.trim(categoryName);
        ThrowExceptionUtils.throwIF(
                StrUtil.isBlank(normalizedName),
                ErrorCode.PARAMS_ERROR,
                "主题名称不能为空");
        ThrowExceptionUtils.throwIF(
                normalizedName.length() > 64,
                ErrorCode.PARAMS_ERROR,
                "主题名称不能超过64个字符");

        // 主题数量通常很少，直接遍历可以把“去首尾空格后重名”的规则写清楚。
        List<Category> categories = this.list();
        int maxSortOrder = 0;
        for (Category category : categories) {
            ThrowExceptionUtils.throwIF(
                    normalizedName.equals(StrUtil.trim(category.getCategoryName())),
                    ErrorCode.PARAMS_ERROR,
                    "主题名称已存在");
            if (category.getSortOrder() != null && category.getSortOrder() > maxSortOrder) {
                maxSortOrder = category.getSortOrder();
            }
        }

        Category category = new Category();
        category.setCategoryName(normalizedName);
        category.setSortOrder(maxSortOrder + 1);
        category.setIsSystem(1);
        ThrowExceptionUtils.throwIF(!this.save(category), ErrorCode.OPERATION_ERROR, "主题创建失败");
        return category;
    }
}

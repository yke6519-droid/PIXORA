package com.example.picturebackend.Service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import com.example.picturebackend.Mapper.CategoryMapper;
import com.example.picturebackend.Service.CategoryService;
import com.example.picturebackend.domain.po.Category;
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
}

package com.example.picturebackend.Service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.picturebackend.domain.po.Category;

import java.util.List;

/** 公共图库主题管理服务。 */
public interface CategoryService extends IService<Category> {

    /** 返回公共图库可选择的主题，按后台配置的顺序排列。 */
    List<Category> listAvailableCategories();

    /** 获取主题，不存在时抛出业务异常。 */
    Category getRequired(Long categoryId);
}

package com.example.picturebackend.Controller;

import com.example.picturebackend.Service.CategoryService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.domain.po.Category;
import com.example.picturebackend.domain.request.BaseResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公共图库主题接口。 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /** 查询公共图库可用主题。 */
    @GetMapping("/list")
    public BaseResponse<List<Category>> list() {
        return ResponseUtils.success(categoryService.listAvailableCategories());
    }
}

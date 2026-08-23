package com.example.picturebackend.Controller;

import com.example.picturebackend.Service.CategoryService;
import com.example.picturebackend.Utils.ResponseUtils;
import com.example.picturebackend.annotation.AuthCheck;
import com.example.picturebackend.constant.UserConstant;
import com.example.picturebackend.domain.po.Category;
import com.example.picturebackend.domain.request.BaseResponse;
import com.example.picturebackend.domain.request.category.CategoryCreateRequest;
import com.example.picturebackend.Exception.ErrorCode;
import com.example.picturebackend.Exception.ThrowExceptionUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /** 只有管理员可以新增公共图库主题。 */
    @PostMapping("/create")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Category> create(@RequestBody CategoryCreateRequest request) {
        ThrowExceptionUtils.throwIF(request == null, ErrorCode.PARAMS_ERROR, "主题请求不能为空");
        return ResponseUtils.success(
            categoryService.createSystemCategory(request.getCategoryName())
        );
    }
}

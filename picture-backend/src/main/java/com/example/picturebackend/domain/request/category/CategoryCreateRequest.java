package com.example.picturebackend.domain.request.category;

import lombok.Data;

/** 管理员创建公共图库主题的请求参数。 */
@Data
public class CategoryCreateRequest {

    /** 主题展示名称。 */
    private String categoryName;
}
